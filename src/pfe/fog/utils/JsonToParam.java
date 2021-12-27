package pfe.fog.utils;

import java.io.FileReader;

import org.fog.utils.Config;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class JsonToParam {

	public static void readParam(String fileName) {
		try {
			JSONObject obj = (JSONObject) JSONValue.parse(new FileReader(fileName));
			
			if (obj.get("MAX_SIMULATION_TIME") != null)
				Config.MAX_SIMULATION_TIME = (int)(long) obj.get("MAX_SIMULATION_TIME");
			
			Config.nbOfLayers = (int)(long) obj.get("NumberOfLayers");
			Config.nbOfNodePerLayer = (int)(long) obj.get("NumberOfNodePerLayer");
			Config.transmitRate = (double) obj.get("TransmitRate");
			Config.numberOfSensorTypes = (int)(long) obj.get("NumberOfSensorTypes");
			if (obj.get("TokenDelay") != null)
				Config.tokenDelay = (double) obj.get("TokenDelay");
			else
				Config.tokenDelay = (int) (Config.transmitRate / (2 * Config.nbOfNodePerLayer));
				
			Config.minDeviceMips = (int)(long) obj.get("MinDeviceMips");
			Config.maxDeviceMips = (int)(long) obj.get("MaxDeviceMips");
			Config.deviceMipsStep = (int)(long) obj.get("DeviceMipsStep");
			Config.minDeviceRam = (int)(long) obj.get("MinDeviceRam");
			Config.maxDeviceRam = (int)(long) obj.get("MaxDeviceRam");
			Config.deviceRamStep = (int)(long) obj.get("DeviceRamStep");
			Config.deviceMaxEnergie = (double) obj.get("DeviceMaxEnergie");
			Config.deviceMinEnergie = (double) obj.get("DeviceMinEnergie");
			
			Config.minModuleMips = (int)(long) obj.get("MinModuleMips");
			Config.maxModuleMips = (int)(long) obj.get("MaxModuleMips");
			Config.moduleMipsStep = (int)(long) obj.get("ModuleMipsStep");
			Config.minModuleRam = (int)(long) obj.get("MinModuleRam");
			Config.maxModuleRam = (int)(long) obj.get("MaxModuleRam");
			Config.moduleRamStep = (int)(long) obj.get("ModuleRamStep");
			
			Config.tupleCpuLengthFactor = (int)(long) obj.get("TupleCpuLengthFactor");
			
			Config.outputFileName = "output/" + (String) obj.get("OutputFileName");
			
			Config.highUsage = (int)(long) obj.get("HighUsage");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
