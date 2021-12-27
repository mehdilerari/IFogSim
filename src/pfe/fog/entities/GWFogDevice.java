package pfe.fog.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppModule;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.utils.FogEvents;
import org.fog.utils.Logger;
import org.fog.utils.NetworkUsageMonitor;

public class GWFogDevice extends FogDevice {
	protected Queue<Tuple> waitingQueue = new LinkedList<Tuple>();
	protected Map<Tuple, Integer> tupleToMatchedDevice = new HashMap<Tuple, Integer>();
	protected List<GWFogDevice> gwDevices;
	protected boolean token;
	protected List<Integer> parentsIds = new ArrayList<Integer>();
	protected List<Boolean> isNorthLinkBusyByid = new ArrayList<Boolean>();
	protected List<Queue<Tuple>> northTupleQueues = new ArrayList<Queue<Tuple>>();
	protected List<Integer> clusterFogDevicesIds = new ArrayList<Integer>();
	
	public static double tokenDelay = 20;
	
	// La liste des tuples matches
	ArrayList<MatchedTuple> matchedTupleList = new ArrayList<MatchedTuple>();
	// La liste des tuples delegues au cloud
	ArrayList<MatchedTuple> toCloudTupleList = new ArrayList<MatchedTuple>();
	
	public GWFogDevice(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList, double schedulingInterval, double uplinkBandwidth, double downlinkBandwidth,
			double uplinkLatency, double ratePerMips, List<Integer> clusterFogDevicesIds) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, uplinkBandwidth, downlinkBandwidth, uplinkLatency, ratePerMips);
		this.clusterFogDevicesIds = clusterFogDevicesIds;

	}
	
	public void addParent(int patendId) {
		parentsIds.add(parentId);
		isNorthLinkBusyByid.add(false);
		northTupleQueues.add(new LinkedList<Tuple>()); // Queue est une interface !!
	}
	
	public List<Integer> getParentsId() {
		return parentsIds;
	}
	
	protected void sendUp(Tuple tuple, int linkId) {
		if (!isNorthLinkBusyByid.get(linkId)) {
			sendUpFreeLink(tuple, linkId);
		} else {
			northTupleQueues.get(linkId).add(tuple);
		}
	}
	
	protected void sendUpFreeLink(Tuple tuple, int linkId) {
		double networkDelay = tuple.getCloudletFileSize() / getUplinkBandwidth();
		
		isNorthLinkBusyByid.set(linkId, true);
		send(getId(), networkDelay, FogEvents.UPDATE_NORTH_TUPLE_QUEUE);
		send(parentsIds.get(linkId), networkDelay + getUplinkLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
		NetworkUsageMonitor.sendingTuple(getUplinkLatency(), tuple.getCloudletFileSize());
	}
	
	protected void updateNorthTupleQueue(){
		int i = 0;
		for (Queue<Tuple> q : getNorthTupleQueues()) {
			if(!q.isEmpty()) {
				Tuple tuple = q.poll();
				sendUpFreeLink(tuple, i);
			} else{
				isNorthLinkBusyByid.set(i, false);
			}
			i++;
		}
	}
	
	protected void sendTupleToActuator(Tuple tuple) {
		for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			String actuatorType = ((Actuator)CloudSim.getEntity(actuatorId)).getActuatorType();
			if(tuple.getDestModuleName().equals(actuatorType)){
				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
	}
	
	@Override
	public void startEntity() {
		super.startEntity();
		if (token) {
			token = false;
			Tuple t = new Tuple(null, 0, 0, 0, 0, 0, 0, null, null, null);
			t.setTupleType("TOKEN");
			sendToSelf(t);
		}
	}
	
	protected void processTupleArrival(SimEvent ev){
		Tuple tuple = (Tuple)ev.getData();
		
		if(getName().equals("cloud")){
			updateCloudTraffic();
		}
		
		Logger.debug(getName(),
				"Received tuple " + tuple.getCloudletId() + " with tupleType = " + tuple.getTupleType() + "\t| Source : "
						+ CloudSim.getEntityName(ev.getSource()) + "|Dest : "
						+ CloudSim.getEntityName(ev.getDestination()));
		
		send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
		
		if (tuple.getDirection() == Tuple.ACTUATOR) {
			sendTupleToActuator(tuple);
			return;
		}

		if (tuple.getTupleType() == "TOKEN") {
			//System.out.println("Token arrived to " + getName());
			// Match
			mapTupleToDevice();
			// Envoi
			int i = 0;
			for (MatchedTuple mt : matchedTupleList) {
				int link = -1;
				if (parentsIds.contains(mt.getDestinationFogDevice()))
					link = parentsIds.indexOf(mt.getDestinationFogDevice());
				
				sendUp(mt, link == -1 ? i : link);
				i = (i + 1) % parentsIds.size();
			}
			
			for (MatchedTuple mt : toCloudTupleList)
				sendUp(mt, 0);
			
			// Envoi a la prochaine gateway
			tuple.setSourceDeviceId(getId());
			send(parentsIds.get(0), tokenDelay, FogEvents.TUPLE_ARRIVAL, tuple);
			//System.out.println("Sending token from " + getName() + " to " + CloudSim.getEntityName(getParentsIds().get(0)));
		} else
			// Ajout a la queue
			waitingQueue.add(tuple);
	}
	
	private void mapTupleToDevice() {
		MatchedTuple m;
		matchedTupleList = new ArrayList<MatchedTuple>();
		toCloudTupleList = new ArrayList<MatchedTuple>();

		int n = Math.min(clusterFogDevicesIds.size(), waitingQueue.size());
		
		HashMap<MatchedTuple,List<Integer>> tuple_prepositionsList = new HashMap<MatchedTuple,List<Integer>>();
		
		// La liste des tuples a matcher 
		ArrayList<MatchedTuple> toBeMatchedTupleList = new ArrayList<MatchedTuple>();
		
		// La liste des tuples qui ont choisi un noeud pendant un tour
		Map<Integer, List<MatchedTuple>> tuplesRequestingDevice = new HashMap<Integer, List<MatchedTuple>>();
		// La liste du tuple selectionne pour un noeud pendant un tour
		Map<Integer, MatchedTuple> selectedTupleForDevice = new HashMap<Integer, MatchedTuple>();
		
		for (int i = 0; i < clusterFogDevicesIds.size(); i++) {
			tuplesRequestingDevice.put(clusterFogDevicesIds.get(i), new ArrayList<MatchedTuple>());
			selectedTupleForDevice.put(clusterFogDevicesIds.get(i), null);
		}
		
		
		for(int i = 0; i < n ; i++) {
			Tuple t = waitingQueue.poll();
			MatchedTuple mt = new MatchedTuple(t);
			toBeMatchedTupleList.add(mt);
			tuple_prepositionsList.put(mt, new ArrayList<Integer>(clusterFogDevicesIds));
			// initailement chaque tuple peut se propos� � tout les noeuds.
		}
		//System.out.println("To be matched Tuples: " + toBeMatchedTupleList);
		
		while (!toBeMatchedTupleList.isEmpty()) {
			Iterator<MatchedTuple> it = toBeMatchedTupleList.iterator();
			while (it.hasNext()) {
				MatchedTuple mt = it.next();
				int id = selectBestDeviceForTuple(mt,tuple_prepositionsList.get(mt));
				if (id == -1) {
					it.remove();
					toCloudTupleList.add(mt);
				} else {
					tuplesRequestingDevice.get(id).add(mt); 
			//Chaque tuple se propose au noeud qu'il pr�f�re parmi ceux � qui il ne s'est pas d�ja pr�sent�.
				}
			}
			for (int id : clusterFogDevicesIds) {
				if (tuplesRequestingDevice.get(id).size() > 0) {
					MatchedTuple mt = selectBestTupleForDevice(id , tuplesRequestingDevice.get(id));
					// On choise le meilleur tuple pour le noeud parmi les proposition.
					for(MatchedTuple mt2 : tuplesRequestingDevice.get(id))
					{
						if(!mt2.equals(mt)) tuple_prepositionsList.get(mt2).remove(Integer.valueOf(id));
					}
					/* Si id pr�f�re mt � tout les autres tuple qui se sont propos� � lui, alors ces tuples
				  	ne peuvent plus se propos� � lui. 
					 */
					if (selectedTupleForDevice.get(id) != null) 
						// si le noeud est d�ja pris, alors on �clate le couple.
					{
						m = selectedTupleForDevice.get(id);
						toBeMatchedTupleList.add(m);
						// on ajoute l'ancien tuple � l'ensemble des tuple � matcher.
						tuple_prepositionsList.get(m).remove(Integer.valueOf(id));
						// on supprime le neuds de la liste des neuds que le tuple peut se proposer.
						matchedTupleList.remove(m);
						// et on le supprime de la liste des tuples matcher.
					}
					toBeMatchedTupleList.remove(mt);
					// on supprime le nouveau tuple pr�f�r� de la liste des tuples � matcher.
					matchedTupleList.add(mt);// on ajoute le nouveau tuple pr�f�r� de la liste des tuples match�s. 
					selectedTupleForDevice.put(id, mt);// et on place le nouveau couple.
					tuplesRequestingDevice.get(id).clear();
				}
			}
		}
		//printSelectionMap(selectedTupleForDevice);
		for (Map.Entry<Integer, MatchedTuple> e : selectedTupleForDevice.entrySet())
			if (e.getValue() != null)
				matchedTupleList.get(matchedTupleList.indexOf(e.getValue())).setDestinationFogDeviceId(e.getKey());
		//System.out.println("To cloud: " + toCloudTupleList);
		for (MatchedTuple mt : toCloudTupleList)
			mt.setDestinationFogDeviceId(CloudSim.getEntityId("cloud"));
	}
	
	private int selectBestDeviceForTuple(MatchedTuple mt, List<Integer> prepositionsList) {
		double minDist = calculateDistance((ClusterFogDevice)CloudSim.getEntity(prepositionsList.get(0)), mt);
		int bestId = prepositionsList.get(0);
		for (int id : prepositionsList) {
			double dist = calculateDistance((ClusterFogDevice)CloudSim.getEntity(id), mt);
//			if (minDist == -1) {
//				minDist = dist;
//				bestId = id;
//			} else if (dist != -1 && minDist >= dist) {
//				minDist = dist;
//				bestId = id;
//			}
			 if (minDist >= dist) {
				minDist = dist;
				bestId = id;
			}
		}
		
//		if (minDist == -1) {
//			return -1;
//		}
		double dist = calculateDistance((ClusterFogDevice)CloudSim.getEntity(bestId), mt);
		if (dist > 1) {
			return -1;
		}
		return bestId;
	}
	
	private MatchedTuple selectBestTupleForDevice(int id, List<MatchedTuple> tuplesRequestingDevice) {
		double minDist = calculateDistance((ClusterFogDevice)CloudSim.getEntity(id), tuplesRequestingDevice.get(0));
		MatchedTuple bestTuple = tuplesRequestingDevice.get(0);
		for (MatchedTuple mt : tuplesRequestingDevice) {
			double dist = calculateDistance((ClusterFogDevice)CloudSim.getEntity(id), mt);
			if (minDist >= dist) {
				minDist = calculateDistance((ClusterFogDevice)CloudSim.getEntity(id), mt);
				bestTuple = mt;
			}
		}
		return bestTuple;
	}
	
	private double calculateDistance(ClusterFogDevice d, Tuple t) {
		AppModule m = ((Controller)CloudSim.getEntity(getControllerId())).getApplications().get(t.getAppId()).getModuleByName(t.getDestModuleName());
		double hostAvailableMips = d.getHost().getAvailableMips();
		double hostMips = d.getHost().getTotalMips();
		double hostUsedMips = hostMips - hostAvailableMips;
		double moduleMips = m.getMips();
		((MatchedTuple)t).destModuleMips = moduleMips;
//		System.out.println("-- Host: " + d.getName() + " MipsAvailable: " + hostAvailableMips + " Tuple: " + t.getCloudletId() + " Mips: " + moduleMips + " Dist: " + (hostAvailableMips > moduleMips ? hostAvailableMips - moduleMips : -1));
//		return hostAvailableMips >= moduleMips ? hostUsedMips : -1;
		
		// formule de l'article
//		System.out.println("-- Host: " + d.getName() + " MipsAvailable: " + hostAvailableMips + " total : " + hostMips + " used : " + hostUsedMips + " Tuple: " + t.getCloudletId() + " Mips: " + moduleMips + " Dist: " + ((hostUsedMips + moduleMips) / hostMips));
		return (hostUsedMips + moduleMips) / hostMips;
	}
	
	private void printSelectionMap(Map<Integer, MatchedTuple> selectedTupleForDevice) {
		System.out.print("Selection Map: {");
		for (Map.Entry<Integer, MatchedTuple> e : selectedTupleForDevice.entrySet()) {
			if (e.getValue() != null)
				System.out.print(CloudSim.getEntity(e.getKey()) + " = " + e.getValue() + ", ");
		}
		System.out.println("}");
	}
	
	public List<Integer> getParentsIds() {
		return parentsIds;
	}

	public void setParentsIds(List<Integer> parentsIds) {
		this.parentsIds = parentsIds;
	}

	public List<Queue<Tuple>> getNorthTupleQueues() {
		return northTupleQueues;
	}

	public void setNorthTupleQueues(List<Queue<Tuple>> northTupleQueues) {
		this.northTupleQueues = northTupleQueues;
	}

	public List<Integer> getClusterFogDevicesIds() {
		return clusterFogDevicesIds;
	}

	public void setClusterFogDevicesIds(List<Integer> clusterFogDevicesIds) {
		this.clusterFogDevicesIds = clusterFogDevicesIds;
	}
	
	public List<GWFogDevice> getGwDevices() {
		return gwDevices;
	}

	public void setGwDevices(List<GWFogDevice> gwDevices) {
		this.gwDevices = gwDevices;
	}

	public boolean isToken() {
		return token;
	}

	public void setToken(boolean token) {
		this.token = token;
	}
}

