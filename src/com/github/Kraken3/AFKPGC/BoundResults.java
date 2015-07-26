package com.github.Kraken3.AFKPGC;

/**
 * Quick and dirty bound results for more complex analysis of movement patterns.
 * 
 * @author ProgrammerDan
 */
public class BoundResults {
	private boolean contained;
	private boolean containedExcludeY;
	private boolean volumeSimilar;
	private boolean surfaceSimilar;
	private boolean nearlyContained;
	private boolean nearlyContainedExcludeY;

	public BoundResults(boolean contained, boolean containedExcludeY,
			boolean volumeSimilar, boolean surfaceSimilar,
			boolean nearlyContained, boolean nearlyContainedExcludeY) {
		this.contained = contained;
		this.containedExcludeY = containedExcludeY;
		this.volumeSimilar = volumeSimilar;
		this.surfaceSimilar = surfaceSimilar;
		this.nearlyContained = nearlyContained;
		this.nearlyContainedExcludeY = nearlyContainedExcludeY;
	}

	public boolean getContained() {
		return contained;
	}
	public boolean getContainedExcludeY() {
		return containedExcludeY;
	}
	public boolean getVolumeSimilar() {
		return volumeSimilar;
	}
	public boolean getSurfaceSimilar() {
		return surfaceSimilar;
	}
	public boolean getNearlyContained() {
		return nearlyContained;
	}
	public boolean getNearlyContainedExcludeY() {
		return nearlyContainedExcludeY;
	}
}
