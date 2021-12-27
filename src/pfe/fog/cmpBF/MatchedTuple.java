package pfe.fog.cmpBF;

import java.util.HashMap;

import org.cloudbus.cloudsim.UtilizationModel;
import org.fog.entities.Tuple;

public class MatchedTuple extends Tuple {
	
	protected int destinationFogDeviceId;
	public double destModuleMips;


	public MatchedTuple(String appId, int cloudletId, int direction, long cloudletLength, int pesNumber,
			long cloudletFileSize, long cloudletOutputSize,
			UtilizationModel utilizationModelCpu,
			UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw) {
		super(appId, cloudletId, direction, cloudletLength, pesNumber, cloudletFileSize,
				cloudletOutputSize, utilizationModelCpu, utilizationModelRam,
				utilizationModelBw);
	}
	
	public MatchedTuple(Tuple tuple) {
		this(tuple.getAppId(), tuple.getCloudletId(), tuple.getDirection(), tuple.getCloudletLength(), tuple.getNumberOfPes(),
				tuple.getCloudletFileSize(), tuple.getCloudletOutputSize(),
				tuple.getUtilizationModelCpu(),
				tuple.getUtilizationModelRam(),
				tuple.getUtilizationModelBw());
		this.setTupleType(tuple.getTupleType());
		this.setDestModuleName(tuple.getDestModuleName());
		this.setSrcModuleName(tuple.getSrcModuleName());
		this.setActualTupleId(tuple.getActualTupleId());
		this.setActuatorId(tuple.getActuatorId());
		this.setSourceDeviceId(tuple.getSourceDeviceId());
		this.setSourceModuleId(tuple.getSourceModuleId());
		this.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
		this.setUserId(tuple.getUserId());
		this.setDirection(Tuple.UP);
	}
	
	public void setDestinationFogDeviceId(int id) {
		destinationFogDeviceId = id;
	}
	public  int getDestinationFogDevice() {
	return destinationFogDeviceId;
    }
	
}
