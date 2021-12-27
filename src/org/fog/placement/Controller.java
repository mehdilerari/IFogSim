package org.fog.placement;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;

import pfe.fog.utils.ResultToCSV;

public class Controller extends SimEntity{
	
	public static boolean ONLY_CLOUD = false;
		
	private List<FogDevice> fogDevices;
	private List<Sensor> sensors;
	private List<Actuator> actuators;
	
	private Map<String, Application> applications;
	private Map<String, Integer> appLaunchDelays;

	private Map<String, ModulePlacement> appModulePlacementPolicy;
	
	private double avgEnergie;
	private double avgAppLoopDelay;
	private double avgTupleCpuExecutionDelay;
	private int totalExecutedTuples;
	private int nodeHighCpuUsageCount;
	private double varianceCpuUsage;
	
	public Controller(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators) {
		super(name);
		this.applications = new HashMap<String, Application>();
		setAppLaunchDelays(new HashMap<String, Integer>());
		setAppModulePlacementPolicy(new HashMap<String, ModulePlacement>());
		for(FogDevice fogDevice : fogDevices){
			fogDevice.setControllerId(getId());
		}
		setFogDevices(fogDevices);
		setActuators(actuators);
		setSensors(sensors);
		connectWithLatencies();
	}

	private FogDevice getFogDeviceById(int id){
		for(FogDevice fogDevice : getFogDevices()){
			if(id==fogDevice.getId())
				return fogDevice;
		}
		return null;
	}
	
	private void connectWithLatencies(){
		for(FogDevice fogDevice : getFogDevices()){
			if (fogDevice instanceof pfe.fog.entities.ClusterFogDevice) {
				for (int parentId : ((pfe.fog.entities.ClusterFogDevice)fogDevice).getParentsIds()) {
					FogDevice parent = getFogDeviceById(parentId);
					if(parent == null)
						continue;
					double latency = fogDevice.getUplinkLatency();
					parent.getChildrenIds().add(fogDevice.getId());
					parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
				}
			}
			else if (fogDevice instanceof pfe.fog.entities.GWFogDevice) {
				for (int parentId : ((pfe.fog.entities.GWFogDevice)fogDevice).getParentsIds()) {
					FogDevice parent = getFogDeviceById(parentId);
					if(parent == null)
						continue;
					double latency = fogDevice.getUplinkLatency();
					parent.getChildrenIds().add(fogDevice.getId());
					parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
				}
			}
			else if (fogDevice instanceof pfe.fog.cmpFF.ClusterFogDevice) {
				for (int parentId : ((pfe.fog.cmpFF.ClusterFogDevice)fogDevice).getParentsIds()) {
					FogDevice parent = getFogDeviceById(parentId);
					if(parent == null)
						continue;
					double latency = fogDevice.getUplinkLatency();
					parent.getChildrenIds().add(fogDevice.getId());
					parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
				}
			}
			else if (fogDevice instanceof pfe.fog.cmpFF.GWFogDevice) {
				for (int parentId : ((pfe.fog.cmpFF.GWFogDevice)fogDevice).getParentsIds()) {
					FogDevice parent = getFogDeviceById(parentId);
					if(parent == null)
						continue;
					double latency = fogDevice.getUplinkLatency();
					parent.getChildrenIds().add(fogDevice.getId());
					parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
				}
			}
			else if (fogDevice instanceof pfe.fog.cmpBF.ClusterFogDevice) {
				for (int parentId : ((pfe.fog.cmpBF.ClusterFogDevice)fogDevice).getParentsIds()) {
					FogDevice parent = getFogDeviceById(parentId);
					if(parent == null)
						continue;
					double latency = fogDevice.getUplinkLatency();
					parent.getChildrenIds().add(fogDevice.getId());
					parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
				}
			}
			else if (fogDevice instanceof pfe.fog.cmpBF.GWFogDevice) {
				for (int parentId : ((pfe.fog.cmpBF.GWFogDevice)fogDevice).getParentsIds()) {
					FogDevice parent = getFogDeviceById(parentId);
					if(parent == null)
						continue;
					double latency = fogDevice.getUplinkLatency();
					parent.getChildrenIds().add(fogDevice.getId());
					parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
				}
			} 
			else if (fogDevice instanceof pfe.fog.cmpWF.ClusterFogDevice) {
				for (int parentId : ((pfe.fog.cmpWF.ClusterFogDevice)fogDevice).getParentsIds()) {
					FogDevice parent = getFogDeviceById(parentId);
					if(parent == null)
						continue;
					double latency = fogDevice.getUplinkLatency();
					parent.getChildrenIds().add(fogDevice.getId());
					parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
				}
			}
			else if (fogDevice instanceof pfe.fog.cmpWF.GWFogDevice) {
				for (int parentId : ((pfe.fog.cmpWF.GWFogDevice)fogDevice).getParentsIds()) {
					FogDevice parent = getFogDeviceById(parentId);
					if(parent == null)
						continue;
					double latency = fogDevice.getUplinkLatency();
					parent.getChildrenIds().add(fogDevice.getId());
					parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
				}
			} 
			else {
				FogDevice parent = getFogDeviceById(fogDevice.getParentId());
				if(parent == null)
					continue;
				double latency = fogDevice.getUplinkLatency();
				parent.getChildrenIds().add(fogDevice.getId());
				parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
			}
		}
	}
	
	@Override
	public void startEntity() {
		for(String appId : applications.keySet()){
			if(getAppLaunchDelays().get(appId)==0)
				processAppSubmit(applications.get(appId));
			else
				send(getId(), getAppLaunchDelays().get(appId), FogEvents.APP_SUBMIT, applications.get(appId));
		}

		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
		
		send(getId(), Config.MAX_SIMULATION_TIME, FogEvents.STOP_SIMULATION);
		
		for(FogDevice dev : getFogDevices())
			sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);

	}

	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.TUPLE_FINISHED:
			processTupleFinished(ev);
			break;
		case FogEvents.CONTROLLER_RESOURCE_MANAGE:
			manageResources();
			break;
		case FogEvents.STOP_SIMULATION:
			CloudSim.stopSimulation();
			printTimeDetails();
			printPowerDetails();
			printCostDetails();
			printNetworkUsageDetails();
			ResultToCSV.addLine(avgEnergie, avgAppLoopDelay, avgTupleCpuExecutionDelay, totalExecutedTuples, nodeHighCpuUsageCount, varianceCpuUsage);
			System.exit(0);
			break;
			
		}
	}
	
	private void printNetworkUsageDetails() {
		System.out.println("Total network usage = "+NetworkUsageMonitor.getNetworkUsage()/Config.MAX_SIMULATION_TIME);		
	}

	private FogDevice getCloud(){
		for(FogDevice dev : getFogDevices())
			if(dev.getName().equals("cloud"))
				return dev;
		return null;
	}
	
	private void printCostDetails(){
		System.out.println("Cost of execution in cloud = "+getCloud().getTotalCost());
	}
	
	private void printPowerDetails() {
		double sum = 0;
		int tupleCount = 0;
		double varCpu = 0, moyCpu = 0;
		nodeHighCpuUsageCount = 0;
		for(FogDevice fogDevice : getFogDevices()){
			System.out.println(fogDevice.getName() + "<" + fogDevice.getHost().getTotalMips() + ", " + fogDevice.getHost().getMaxPower() + 
					"> : Energy Consumed = " + fogDevice.getEnergyConsumption() + " | Executed tuples = " + fogDevice.nbExecutedTuples + 
					" | Avg CPU Usage = " + fogDevice.avgUtilizationOfCpu);
			
			if (fogDevice.avgUtilizationOfCpu > Config.highUsage)
				nodeHighCpuUsageCount++;
			
			varCpu += fogDevice.avgUtilizationOfCpu * fogDevice.avgUtilizationOfCpu;
			moyCpu += fogDevice.avgUtilizationOfCpu;
			
			sum += fogDevice.getEnergyConsumption();
			tupleCount += fogDevice.nbExecutedTuples;
		}
		double avg = sum / getFogDevices().size();
		System.out.println("Average energy consumed = " + avg + " | Total Executed Tuple Count = " + tupleCount);
		avgEnergie = avg;
		totalExecutedTuples = tupleCount;
		
		varCpu = varCpu / getFogDevices().size();
		moyCpu = moyCpu / getFogDevices().size();
		varianceCpuUsage = varCpu - (moyCpu * moyCpu);
		
		
	}

	private String getStringForLoopId(int loopId){
		for(String appId : getApplications().keySet()){
			Application app = getApplications().get(appId);
			for(AppLoop loop : app.getLoops()){
				if(loop.getLoopId() == loopId)
					return loop.getModules().toString();
			}
		}
		return null;
	}
	private void printTimeDetails() {
		float sum = 0;
		int i = 0;
		System.out.println("=========================================");
		System.out.println("============== RESULTS ==================");
		System.out.println("=========================================");
		System.out.println("EXECUTION TIME : "+ (Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime()));
		System.out.println("=========================================");
		System.out.println("APPLICATION LOOP DELAYS");
		System.out.println("=========================================");
		for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()){
			/*double average = 0, count = 0;
			for(int tupleId : TimeKeeper.getInstance().getLoopIdToTupleIds().get(loopId)){
				Double startTime = 	TimeKeeper.getInstance().getEmitTimes().get(tupleId);
				Double endTime = 	TimeKeeper.getInstance().getEndTimes().get(tupleId);
				if(startTime == null || endTime == null)
					break;
				average += endTime-startTime;
				count += 1;
			}
			System.out.println(getStringForLoopId(loopId) + " ---> "+(average/count));*/
			System.out.println(getStringForLoopId(loopId) + " ---> "+TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId));
			if (TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId) != null) {
				sum += TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId);
				i++;
			}
		}
		avgAppLoopDelay = sum / i;
		System.out.println("Average = " + sum / i);
		System.out.println("=========================================");
		System.out.println("TUPLE CPU EXECUTION DELAY");
		System.out.println("=========================================");
		sum = 0;
		i = 0;
		for(String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()){
			System.out.println(tupleType + " ---> "+TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType));
			sum += TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType);
			i++;
		}
		avgTupleCpuExecutionDelay = sum / i;
		System.out.println("Average = " + sum / i);
		System.out.println("=========================================");
	}

	protected void manageResources(){
		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
	}
	
	private void processTupleFinished(SimEvent ev) {
	}
	
	@Override
	public void shutdownEntity() {	
	}
	
	public void submitApplication(Application application, int delay, ModulePlacement modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(Sensor sensor : sensors){
			sensor.setApp(getApplications().get(sensor.getAppId()));
		}
		for(Actuator ac : actuators){
			ac.setApp(getApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(Actuator actuator : getActuators()){
					if(actuator.getActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	public void submitApplication(Application application, ModulePlacement modulePlacement){
		submitApplication(application, 0, modulePlacement);
	}
	
	
	private void processAppSubmit(SimEvent ev){
		Application app = (Application) ev.getData();
		processAppSubmit(app);
	}
	
	private void processAppSubmit(Application application){
		System.out.println(CloudSim.clock()+" Submitted application "+ application.getAppId());
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getApplications().put(application.getAppId(), application);
		
		ModulePlacement modulePlacement = getAppModulePlacementPolicy().get(application.getAppId());
		for(FogDevice fogDevice : fogDevices){
			sendNow(fogDevice.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
		}
		
		Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
//		for (int deviceId : deviceToModuleMap.keySet()) {
//		for (PowerHost host : ((FogDevice)CloudSim.getEntity(deviceId)).<PowerHost>getHostList()) {
//			for (Vm vm : host.getVmList()) {
//				System.out.println("HOST: " + CloudSim.getEntityName(deviceId) + " HOST : " + host.getId() + " VM : " + vm.getId());
//			}
//		}
//	}
		for(Integer deviceId : deviceToModuleMap.keySet()){
			for(AppModule module : deviceToModuleMap.get(deviceId)){
				sendNow(deviceId, FogEvents.APP_SUBMIT, application);
				sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
			}
		}
	}

	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}

	public void setFogDevices(List<FogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}

	public Map<String, Integer> getAppLaunchDelays() {
		return appLaunchDelays;
	}

	public void setAppLaunchDelays(Map<String, Integer> appLaunchDelays) {
		this.appLaunchDelays = appLaunchDelays;
	}

	public Map<String, Application> getApplications() {
		return applications;
	}

	public void setApplications(Map<String, Application> applications) {
		this.applications = applications;
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public void setSensors(List<Sensor> sensors) {
		for(Sensor sensor : sensors)
			sensor.setControllerId(getId());
		this.sensors = sensors;
	}

	public List<Actuator> getActuators() {
		return actuators;
	}

	public void setActuators(List<Actuator> actuators) {
		this.actuators = actuators;
	}

	public Map<String, ModulePlacement> getAppModulePlacementPolicy() {
		return appModulePlacementPolicy;
	}

	public void setAppModulePlacementPolicy(Map<String, ModulePlacement> appModulePlacementPolicy) {
		this.appModulePlacementPolicy = appModulePlacementPolicy;
	}
}