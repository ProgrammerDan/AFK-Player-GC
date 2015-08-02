package com.github.Kraken3.AFKPGC;

import java.util.List;
import org.bukkit.Location;

/**
 * Bounding box utility class
 * 
 * @author ProgrammerDan
 */ 
public class ActivityBounds {
	private double xLow;
	private double xHigh;
	private double yLow;
	private double yHigh;
	private double zLow;
	private double zHigh;

	/**
	 * Creates a new activity bounds from a list of Location objects.
	 */
	public ActivityBounds(List<Location> points) {
		reset(points);
	}

	/**
	 * Scans through the list, setting the bounds min/max leveraging the Locations.
	 */
	private void reset(List<Location> points) {
		xLow = yLow = zLow = Double.MAX_VALUE;
		xHigh = yHigh = zHigh = Double.MIN_VALUE;
		String world = points.get(points.size() - 1).getWorld().getName();
		
		for (Location l : points) {
			if (!world.equals(l.getWorld().getName()) ) {
				continue;
			}
			double x = l.getX();
			double y = l.getY();
			double z = l.getZ();
			xLow = (x < xLow) ? x : xLow;
			yLow = (y < yLow) ? y : yLow;
			zLow = (z < zLow) ? z : zLow;
			xHigh = (x > xHigh) ? x : xHigh;
			yHigh = (y > yHigh) ? y : yHigh;
			zHigh = (z > zHigh) ? z : zHigh;
		}
	}

	public double getXLow() {
		return xLow;
	}
	public double getXHigh() {
		return xHigh;
	}
	public double getYLow() {
		return yLow;
	}
	public double getYHigh() {
		return yHigh;
	}
	public double getZLow() {
		return zLow;
	}
	public double getZHigh() {
		return zHigh;
	}

	/**
	 * Checks if the passed bounds are fully contained
	 *   by these bounds.
	 *
	 * @param bounds The bounds to test
	 * @return The results of the containment test
	 */
	public boolean contains(ActivityBounds bounds) {
		return (xLow <= bounds.getXLow() &&
			xHigh >= bounds.getXHigh() &&
			yLow <= bounds.getYLow() &&
			yHigh >= bounds.getYHigh() &&
			zLow <= bounds.getZLow() &&
			zHigh >= bounds.getZHigh());
	}
	
	/**
	 * Checks if the passed bounds are contained
	 *   by these bounds multiplied by a factor.
	 * 
	 * @param bounds The bounds to test
	 * @param factor Factor to multiply bounds by
	 * @return The results of the containment test
	 */
	public boolean contains(ActivityBounds bounds, double factor) {
		return (xLow * factor <= bounds.getXLow() &&
			xHigh * factor >= bounds.getXHigh() &&
			yLow * factor <= bounds.getYLow() &&
			yHigh * factor >= bounds.getYHigh() &&
			zLow * factor <= bounds.getZLow() &&
			zHigh * factor >= bounds.getZHigh());
	}

	/**
	 * Checks if the passed bounds are contained by the X and Z
	 *   dimensions, excludes Y.
	 * 
	 * @param bounds The bounds to test, ignoring Y
	 * @return The results of the containment test
	 */
	public boolean containsExcludesY(ActivityBounds bounds) {
		return (xLow <= bounds.getXLow() &&
			xHigh >= bounds.getXHigh() &&
			zLow <= bounds.getZLow() &&
			zHigh >= bounds.getZHigh());
	}
	
	/**
	 * Checks if the passed bounds, multiplied by a factor,
	 *   are contained by the X and Z dimensions, excludes Y.
	 * 
	 * @param bounds The bounds to test, ignoring Y
	 * @param factor Factor to multiply bounds by
	 * @return The results of the containment test
	 */
	public boolean containsExcludesY(ActivityBounds bounds, double factor) {
		return (xLow * factor <= bounds.getXLow() &&
			xHigh * factor >= bounds.getXHigh() &&
			zLow * factor <= bounds.getZLow() &&
			zHigh * factor >= bounds.getZHigh());
	}

	/**
	 * Returns the volume described by these bounds
	 *
	 * @return The volume described by these bounds
	 */
	public double volume() {
		return ( (xHigh - xLow) * (yHigh - yLow) * (zHigh - zLow) );
	}

	/**
	 * Returns the "travel surface", excludes Y.
	 * 
	 * @return The travel surface (bounds excluding Y)
	 */
	public double travelSurface() {
		return ( (xHigh - xLow) * (zHigh - zLow) );
	}
}
