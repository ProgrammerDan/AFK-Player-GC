package com.github.Kraken3.AFKPGC;

import java.util.Map;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.Location;

/**
 * Static collection of activity tracking in addition to some consistency fixing and such.
 */
public class LastActivity{
	public static Map<UUID, LastActivity> lastActivities = new TreeMap<UUID, LastActivity>();
	public static long currentTime; 	//OCD compels me to save a few System.currentTimeMillis() calls..	
	public LinkedList <Location> loggedLocations = new LinkedList<Location>();
	public long timeOfLastActivity;
	public long timeOfLastKickerPass; //time of the last Kicker.run call, relevant for warnings
	public UUID playerName; //useful only in onCommandList
	public ActivityBounds lastBounds; 
	
	/** onPlayerQuitEvent doesn't trigger on all player log off events for some reason.
	 * This causes LastActivity.lastActivities to contain more players than there are playing on the server.
	 * FixInconsistencies() fixes this problem.
	 * */
	static public void FixInconsistencies(){
		Map<UUID, LastActivity> lastActivities = LastActivity.lastActivities;
		Player[] players = new Player[AFKPGC.plugin.getServer().getOnlinePlayers().size()];
		int x = 0;
		for (Player p: AFKPGC.plugin.getServer().getOnlinePlayers()) {
			players[x] = p;
			x++;
		}
		TreeSet<UUID> playersTree = new TreeSet<UUID>();
		
		for (Player p:players) {
			UUID uuid = p.getUniqueId();
			LastActivity la = lastActivities.get(uuid);
			if (la == null) {
				la = AFKPGC.addPlayer(p);
			}
			if (la != null) {
				la.timeOfLastKickerPass = LastActivity.currentTime;
				playersTree.add(uuid);
			}
		}
		
		UUID[] keySet = lastActivities.keySet().toArray(new UUID[0]);
		for (UUID i:keySet) {
			if (!playersTree.contains(i)) {
				AFKPGC.removerPlayer(i);
			}
		}		
		
	}

	public int calculateMovementRadius() {
		Location current = loggedLocations.getLast();
		int distance = 0;
		for(int i=0; i < loggedLocations.size()-1; i++) {
			int possibleNewDistance = (int) loggedLocations.get(i).distance(current);
			if (possibleNewDistance > distance) {
				distance = possibleNewDistance;
			}
		}
		return distance;
	}

	/**
	 * Computes a set of parameters related to bounds checking. Returns null if no prior bounds history.
	 * 
	 * <ul>
	 *   <li><b>Contains</b>: New bounding box fully contains old bounding box, or vice-versa.</li>
	 *   <li><b>Contains Excludes Y</b>: Considering Surface only (X, Z) checks if new surface box fully
	 *       contains old surface box, or vice-versa.</li>
	 *   <li><b>Volume</b>: Checks if the volumes of old and new bounding box are similar, within relaxFactor.</li>
	 *   <li><b>Surface Area</b>: Checks if surface (X, Z) only areas of old and new bounding box are similar,
	 *       within relaxFactor.</li>
	 *   <li><b>Relaxed Contains</b>: New box subsumes old, or vice-versa, within relaxFactor.</li>
	 *   <li><b>Relaxed Contains Excludes Y</b>: Considering Surface only (X, Z) checks if new surface box
	 *       subsumes old, or vice-versa, within relaxFactor.</li>
	 * 
	 * @param relaxFactor Should be a value > 1.0d, but doesn't have to be, indicates "stretch" of bounding box
	 *     during non-strict comparisons, allows for situations where the box is subtly cycling between
	 *     sizes due to length of location tracking being slightly short relative to bot motion. Don't make the
	 *     factor too large, it should only ever be slightly over 1.0d.
	 * @return a {@link BoundsResult} containing check results, or null if not enough history.
	 */
	public BoundResults evaluateBounds(double relaxFactor) {
		ActivityBounds newBounds = new ActivityBounds(loggedLocations);
		if (lastBounds == null) {
			lastBounds = newBounds;
			return null;
		} else {
			return new BoundResults(
				newBounds.contains(lastBounds) || lastBounds.contains(newBounds),
				newBounds.containsExcludesY(lastBounds) ||
				lastBounds.containsExcludesY(newBounds),
				newBounds.volume() * relaxFactor >= lastBounds.volume() ||
				lastBounds.volume() * relaxFactor >= newBounds.volume(),
				newBounds.travelSurface() * relaxFactor >= lastBounds.travelSurface() ||
				lastBounds.travelSurface() * relaxFactor >= newBounds.travelSurface(),
				newBounds.contains(lastBounds, relaxFactor) ||
				lastBounds.contains(newBounds, relaxFactor),
				newBounds.containsExcludesY(lastBounds, relaxFactor) ||
				lastBounds.containsExcludesY(newBounds, relaxFactor));
		}	
	}
}
