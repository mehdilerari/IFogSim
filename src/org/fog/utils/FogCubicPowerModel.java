package org.fog.utils;

import org.cloudbus.cloudsim.power.models.PowerModel;

public class FogCubicPowerModel implements PowerModel {
	/** The max power. */
	private double maxPower;

	/** The constant. */
	private double constant;

	/** The static power. */
	private double staticPower;

	/**
	 * Instantiates a new power model cubic.
	 * 
	 * @param maxPower the max power
	 * @param staticPowerPercent the static power percent
	 */
	public FogCubicPowerModel(double maxPower, double staticPowerPercent) {
		setMaxPower(maxPower);
		setStaticPower(staticPowerPercent * maxPower);
		setConstant((maxPower - getStaticPower()) / Math.pow(100, 3));
	}

	/*
	 * (non-Javadoc)
	 * @see gridsim.virtualization.power.PowerModel#getPower(double)
	 */
	@Override
	public double getPower(double utilization) throws IllegalArgumentException {
		if (utilization < 0 || utilization > 1) {
			throw new IllegalArgumentException("Utilization value must be between 0 and 1");
		}
		if (utilization == 0) {
			return 0;
		}
		return getStaticPower() + getConstant() * Math.pow(utilization * 100, 3);
	}

	/**
	 * Gets the max power.
	 * 
	 * @return the max power
	 */
	protected double getMaxPower() {
		return maxPower;
	}

	/**
	 * Sets the max power.
	 * 
	 * @param maxPower the new max power
	 */
	protected void setMaxPower(double maxPower) {
		this.maxPower = maxPower;
	}

	/**
	 * Gets the constant.
	 * 
	 * @return the constant
	 */
	protected double getConstant() {
		return constant;
	}

	/**
	 * Sets the constant.
	 * 
	 * @param constant the new constant
	 */
	protected void setConstant(double constant) {
		this.constant = constant;
	}

	/**
	 * Gets the static power.
	 * 
	 * @return the static power
	 */
	protected double getStaticPower() {
		return staticPower;
	}

	/**
	 * Sets the static power.
	 * 
	 * @param staticPower the new static power
	 */
	protected void setStaticPower(double staticPower) {
		this.staticPower = staticPower;
	}
}
