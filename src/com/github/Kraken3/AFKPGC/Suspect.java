package com.github.Kraken3.AFKPGC;

import java.util.UUID;
import org.bukkit.Location;

/**
 * Lightweight holder for Suspects.
 * 
 * @author ProgrammerDan
 */
public class Suspect implements Comparable<Suspect> {
	private UUID uuid;
	private String name;
	private Location location;
	private Long results;
	
	public Suspect(UUID uuid, String name, Location location, Long results) {
		this.uuid = uuid;
		this.name = name;
		this.location = location;
		this.results = results;
	}

	public void update(Location location, Long results) {
		this.location = location;
		this.results = results;
	}

	public UUID getUUID() {
		return this.uuid;
	}
	public String getName() {
		return this.name;
	}
	public Location getLocation() {
		return this.location;
	}
	public Long getResults() {
		return this.results;
	}

	/**
	 * Compares two Suspects. If either name is unknown or the names are equal, compares
	 * against UUID. Otherwise, compares by name (effectively for sort output).
	 *
	 * @param suspect The Suspect to compare this against.
	 * @return -1, 0, or 1 as this Suspect is UUID or Name ordered before, equal, or after suspect.
	 */
	@Override
	public int compareTo(Suspect suspect) {
		// if either name is null or the names are the same, compare on UUID.
		if (this.name == null || suspect.getName() == null || this.name.equals(suspect.getName() ) ) {
			return this.uuid.compareTo(suspect.getUUID());
		} else { // if neither is null and the name is not equal, compare against name.
			return this.name.compareTo(suspect.getName());
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Suspect) {
			Suspect q = (Suspect) o;
			return this.name.equals(q.getName()) && this.uuid.equals(q.getUUID());
		}
		return false;
	}
}
