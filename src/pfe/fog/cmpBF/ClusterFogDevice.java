package pfe.fog.cmpBF;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppModule;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.utils.FogEvents;
import org.fog.utils.Logger;
import org.fog.utils.NetworkUsageMonitor;

public class ClusterFogDevice extends FogDevice {
	protected List<Integer> parentsIds = new ArrayList<Integer>();
	protected List<Boolean> isNorthLinkBusyById = new ArrayList<Boolean>();
	protected List<Boolean> isNorthLinkBusyByid = new ArrayList<Boolean>();
	protected List<Queue<Tuple>> northTupleQueues = new ArrayList<Queue<Tuple>>();

	protected long availableMips;
	protected int availableRam;
	private int i = 0;
	private int j = 0;
	public ClusterFogDevice(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList, double schedulingInterval, double uplinkBandwidth, double downlinkBandwidth,
			double uplinkLatency, double ratePerMips) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, uplinkBandwidth, downlinkBandwidth, uplinkLatency, ratePerMips);
		availableMips = characteristics.getMips();
		availableRam = characteristics.getHostList().get(0).getRam();
	}
	
	public void addParent(int patendId) {
		parentsIds.add(parentId);
		isNorthLinkBusyByid.add(false);
		northTupleQueues.add(new LinkedList<Tuple>());
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
		if (CloudSim.getEntity(getChildrenIds().get(0)) instanceof GWFogDevice) {
			for (int id : getChildrenIds()) {
				sendDown(tuple, id);
			}
		} else {
			sendDown(tuple, getChildrenIds().get(j));
			j = (j + 1) % getChildrenIds().size();
		}
	}
	
	protected void sendTupleDownToGW(Tuple tuple) {
		sendDown(tuple, getChildrenIds().get(j));
		j = (j + 1) % getChildrenIds().size();
	}
	
	protected int launchVm(Vm vm) {
		if (getVmAllocationPolicy().allocateHostForVm(vm)) {
			getVmList().add(vm);

			if (vm.isBeingInstantiated()) {
				vm.setBeingInstantiated(false);
			}
			vm.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(vm).getVmScheduler()
					.getAllocatedMipsForVm(vm));
		} else {
			//echec de creation
		}
		return vm.getId();
	}
	
	protected void processTupleArrival(SimEvent ev) {
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
			int srcId = tuple.getSourceDeviceId();
			int index = getChildrenIds().indexOf(srcId);
			index = (index + 1) % getChildrenIds().size();
			sendDown(tuple, getChildrenIds().get(index));
			return;
		} else {
			if (tuple instanceof MatchedTuple) {
				if (((MatchedTuple) tuple).getDestinationFogDevice() == getId()) {
					// ce node est la destination
					AppModule m = ((Controller)CloudSim.getEntity(getControllerId())).getApplications().get(tuple.getAppId()).getModuleByName(tuple.getDestModuleName());
					m = new AppModule(m);
					int vmId = launchVm(m);
					if (vmId == -1) {
					} else {
						tuple.setVmId(vmId);
						updateTimingsOnReceipt(tuple);
						executeTuple(ev, tuple.getDestModuleName());
					}	
				} else {
					// ce node n'est pas la destination
					if (getParentsIds().contains(((MatchedTuple) tuple).getDestinationFogDevice())) {
						// destination est au prohain niveau
						int id = getParentsIds().indexOf(((MatchedTuple) tuple).getDestinationFogDevice());
						sendUp(tuple, id);
					} else {
						// sinon
						sendUp(tuple, i);
						i = (i + 1) % parentsIds.size();
					}
				}
			} else {
				// le tuple n'est pas un matchedtuple
				tuple.setDirection(Tuple.DOWN);
				sendTupleDownToGW(tuple);
			}
		}
	}
	
	public List<Integer> getParentsIds() {
		return parentsIds;
	}

	public void setParentsIds(List<Integer> parentsIds) {
		this.parentsIds = parentsIds;
	}

	public List<Boolean> getIsNorthLinkBusyById() {
		return isNorthLinkBusyById;
	}

	public void setIsNorthLinkBusyById(List<Boolean> isNorthLinkBusyById) {
		this.isNorthLinkBusyById = isNorthLinkBusyById;
	}

	public List<Queue<Tuple>> getNorthTupleQueues() {
		return northTupleQueues;
	}

	public void setNorthTupleQueues(List<Queue<Tuple>> northTupleQueues) {
		this.northTupleQueues = northTupleQueues;
	}

	public double getAvailableMips() {
		return getHostList().get(0).getAvailableMips();
		// return availableMips;
	}

	public void setAvailableMips(long availableMips) {
		this.availableMips = availableMips;
	}

	public int getAvailableRam() {
		return getHostList().get(0).getRam();
		// return availableRam;
	}

	public void setAvailableRam(int availableRam) {
		this.availableRam = availableRam;
	}

}
