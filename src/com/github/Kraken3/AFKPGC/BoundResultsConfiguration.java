package com.github.Kraken3.AFKPGC;

/**
 * Configuration for interpreting Bound Results.
 *
 * @author ProgrammerDan
 */
public class BoundResultsConfiguration {
	private double threshold;
	private double contained;
	private double containedExcludeY;
	private double volumeSimilar;
	private double surfaceSimilar;
	private double nearlyContained;
	private double nearlyContainedExcludeY;

	public BoundResultsConfiguration(double threshold, double contained, double containedExcludeY,
			double volumeSimilar, double surfaceSimilar, double nearlyContained, double nearlyContainedExcludeY) {
		this.threshold = threshold;
		this.contained = contained;
		this.containedExcludeY = containedExcludeY;
		this.volumeSimilar = volumeSimilar;
		this.surfaceSimilar = surfaceSimilar;
		this.nearlyContained = nearlyContained;
		this.nearlyContainedExcludeY = nearlyContainedExcludeY;
	}

	public double getThreshold() {
		return threshold;
	}
	public double getContained() {
		return contained;
	}
	public double getContainedExcludeY() {
		return containedExcludeY;
	}
	public double getVolumeSimilar() {
		return volumeSimilar;
	}
	public double getSurfaceSimilar() {
		return surfaceSimilar;
	}
	public double getNearlyContained() {
		return nearlyContained;
	}
	public double getNearlyContainedExcludeY() {
		return nearlyContainedExcludeY;
	}

}
