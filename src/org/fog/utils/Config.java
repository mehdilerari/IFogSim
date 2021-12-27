package org.fog.utils;

public class Config {

	public static final double RESOURCE_MGMT_INTERVAL = 5;
	public static int MAX_SIMULATION_TIME = 100;
	public static int RESOURCE_MANAGE_INTERVAL = 5;
	public static String FOG_DEVICE_ARCH = "x86";
	public static String FOG_DEVICE_OS = "Linux";
	public static String FOG_DEVICE_VMM = "Xen";
	public static double FOG_DEVICE_TIMEZONE = 10.0;
	public static double FOG_DEVICE_COST = 3.0;
	public static double FOG_DEVICE_COST_PER_MEMORY = 0.05;
	public static double FOG_DEVICE_COST_PER_STORAGE = 0.001;
	public static double FOG_DEVICE_COST_PER_BW = 0.0;
	
	public static int highUsage;
	
	public static int nbOfLayers;
	public static int nbOfNodePerLayer;
	public static double tokenDelay;
	public static double transmitRate;
	public static int numberOfSensorTypes;
	
	public static int minDeviceMips;
	public static int maxDeviceMips;
	public static int deviceMipsStep;
	public static int minDeviceRam;
	public static int maxDeviceRam;
	public static int deviceRamStep;
	public static double deviceMaxEnergie;
	public static double deviceMinEnergie;
	
	public static int minModuleMips;
	public static int maxModuleMips;
	public static int moduleMipsStep;
	public static int minModuleRam;
	public static int maxModuleRam;
	public static int moduleRamStep;
	
	public static int tupleCpuLengthFactor;
	
	public static String outputFileName;
}