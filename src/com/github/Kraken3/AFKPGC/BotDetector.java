package com.github.Kraken3.AFKPGC;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.TreeMap;
import java.util.HashMap;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/*
 * Detects likely bots based on movement patterns
 * @author Maxopoly
 * @author ProgrammerDan
 * 
 */
public class BotDetector implements Runnable {
	public static boolean longBans;
	public static float currentTPS = 20;
	public static float acceptableTPS;
	public static float criticalTPSChange;
	public static float relaxationFactor; // TODO
	public static int maxLocations; // TODO
	public static int maxSuspects; // TODO
	public static int maxReprieve; // TODO
	public static BoundResultsConfiguration boundsConfig; // TODO
	public static long frequency; // how often this runs in ticks
	public static File banfile;
	float lastRoundTPS;

	TreeMap<Integer, Suspect> topSuspects;
	TreeMap<Integer, Suspect> lastRoundSuspects;
	HashMap<UUID, Integer> reprieve; // temp. cleared suspects
	

	UUID topSuspect = null;
	/* Needs to be stored additionally, because retrieving the name of
	 * players after kicking them is only possible over deprecated/buggy methods */
	String topSuspectName = ""; 
	Location topSuspectsLocation = null;
	
	UUID lastRoundSuspect = null;
	String lastRoundSuspectName = "";
	Location lastRoundSuspectsLocation = null;
	
	public static LinkedList<String> suspectedBotters = new LinkedList<String>();
	/* this is needed as a separated list, so we know the difference between players
	 * who were banned by AFKPGC and players who were banned for other reasons */
	public static LinkedList<String> bannedPlayers = new LinkedList<String>();

	// ban after names not ips
	static BanList banList = AFKPGC.plugin.getServer().getBanList(BanList.Type.NAME);

	public void run() {
		if (topSuspects == null) {
			topSuspects = new TreeMap<Integer, Suspect>();
		}
		if (lastRoundSuspects == null) {
			lastRoundSuspects = new TreeMap<Integer, Suspect>();
		}
		if (reprieve == null) {
			reprieve = new HashMap<UUID, Integer>();
		} else {
			// decrement reprieve.
			for (Iterator<Map.Entry<UUID, Integer>> i = reprieve.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry<UUID, Integer> entry = i.next();
				Integer roundsLeft = entry.getValue() - 1;
				if (roundsLeft <= 0) { // no more reprieve
					i.remove();
				} else {
					entry.setValue(roundsLeft);
				}
			}
		}

		if (!AFKPGC.enabled) {
			return;
		}
		currentTPS = TpsReader.getTPS();
		Map<UUID, LastActivity> lastActivities = LastActivity.lastActivities;
		if (currentTPS < acceptableTPS) {
			topSuspects.clear();
			int smallestMovedDistance = 1024;
			Set<Map.Entry<UUID, LastActivity>> entries = lastActivities.entrySet();
			// find new top suspects
			for (Map.Entry<UUID, LastActivity> entry : entries) {
				UUID playerUUID = entry.getKey();
				/* according to the author of AFKGPC, there might be
				 * inconsistencies in this list, so this additional
				 * check is needed */
				if (lastActivities.containsKey(playerUUID)) {
					LastActivity la = entry.getValue();
					la.loggedLocations.add(Bukkit.getPlayer(playerUUID).getLocation());
					if (la.loggedLocations.size() >= maxLocations) {
						if (la.loggedLocations.size() > maxLocations) {
							la.loggedLocations.removeFirst();
						}
						// we keep tracking location even if on reprieve.
						if (!reprieve.containsKey(playerUUID)) {
							int itWasntMeISwear = la.calculateMovementradius();
							if (itWasntMeISwear < smallestMovedDistance) {
								smallestMovedDistance = itWasntMeISwear;
								Player dirtyLiar = Bukkit.getPlayer(playerUUID);

								topSuspects.put(itWasntMeISwear, new Suspect(
										playerUUID, dirtyLiar.getName(), dirtyLiar.getLocation(),
										la.evaluateBounds(relaxationFactor) ) );

								if (topSuspects.size() > maxSuspects) {
									topSuspects.pollLastEntry(); // gets rid of largest distance
								}
							}
						}
					}
				}
			}
			//if (topSuspect != null) {
			if (topSuspects.size() > 0) {
				for (Map.Entry<Integer, Suspect> entry : topSuspects) {
					if (!AFKPGC.immuneAccounts.contains(topSuspect)) {
						// Test Bounds for truebot(tm) detection.
						BoundResult bounds = entry.getValue().getResults();
						if (bounds != null) {
							double truebot = 0.0;
							truebot += (bounds.getContained() ? boundsConfig.getContained() : 0.0);
							truebot += (bounds.getContainedExcludeY() ? boundsConfig.getContainedExcludeY() : 0.0);
							truebot += (bounds.getVolumeSimilar() ? boundsConfig.getVolumeSimilar() : 0.0);
							truebot += (bounds.getSurfaceSimilar() ? boundsConfig.getSurfaceSimilar() : 0.0);
							truebot += (bounds.getNearlyContained() ? boundsConfig.getNearlyContained() : 0.0);
							truebot += (bounds.getNearlyContainedExcludeY() ? 
									boundsConfig.getNearlyContainedExcludeY() : 0.0);

							if (truebot >= boundsConfig.getThreshold()) {
								// Its movement looks botlike.

								// Now test surrounding area.
								Location point = entry.getValue().getLocation();

								// TODO
								{
									Date currentDate = new Date();
									long bantime = (long) (frequency / currentTPS * 1000);
									/* Because the ban time is based on real time and the next run
									 * of this method is based on the tick, the ban time needs to be
									 * adjusted to the tick. The long form of this would be:
									 * (20/currentTPS) * (frequency/20) * 1000
									 * The time is in ms */
									banList.addBan(entry.getValue().getName(),
											"You were suspected to cause lag and banned for "
													+ bantime / 1000 + " seconds", new Date(
													currentDate.getTime() + bantime), null);
									// ban the player for ~ a minute
								}
							} else {
								// Its movement is not botlike -- probably. Give a temporary reprieve.
								reprieve.put(entry.getValue().getUUID(), maxReprieve);
							}
						} // else not enough info yet. Pass.
					}
				}
			}

			if (lastRoundSuspect != null) {
				if (!AFKPGC.immuneAccounts.contains(lastRoundSuspect)) {
					if (currentTPS - lastRoundTPS > criticalTPSChange) {
						/* value needs to be configured, that was just the first thing
						 * that came to mind. This can be relatively sensitive,
						 * because it will only ban players for a longer period of
						 * time, if it catches them twice here */
						Date currentDate = new Date();
						if (suspectedBotters.contains(lastRoundSuspectName)) {
							if (longBans) {
								banList.addBan(
										lastRoundSuspectName,
										"Kicking you resulted in a noticeable TPS im"
												+ "provement, so you were banned until the TPS goes"
												+ " back to normal values.",
										new Date(
												currentDate.getTime() + 6 * 3600 * 1000),
										null); // 6h ban
								bannedPlayers.add(lastRoundSuspectName);
								addToBanfile(lastRoundSuspectName);
								suspectedBotters.remove(lastRoundSuspectName);
							}
							AFKPGC.logger.info("The player "
													+ lastRoundSuspectName
													+ " causes lag and is a repeated offender,"
													+ " kicking him resulted in a TPS improvement of "
													+ String.valueOf(currentTPS
															- lastRoundTPS)
													+ " at the location "
													+ lastRoundSuspectsLocation
															.toString());

						} else {
							suspectedBotters.add(lastRoundSuspectName);
							AFKPGC.logger.info( "The player "
													+ lastRoundSuspectName
													+ " is suspected to cause lag,"
													+ " kicking him resulted in a TPS improvement of "
													+ String.valueOf(currentTPS
															- lastRoundTPS)
													+ " at the location "
													+ lastRoundSuspectsLocation
															.toString());
						}
					}
				}
			}

			lastRoundTPS = currentTPS;
			lastRoundSuspect = topSuspect;
			lastRoundSuspectName = topSuspectName;
			// as preparation for the next run:
			lastRoundSuspectsLocation = topSuspectsLocation;

		} else { // TPS is high enough
			if (bannedPlayers.size() != 0) {
				freeEveryone(); // not everyone, but everyone banned by this plugin
			}
		}

	}

	public static void freeEveryone() {
		for (int i = 0; i < bannedPlayers.size(); i++) {
			banList.pardon(bannedPlayers.get(i));
		}
		bannedPlayers.clear();
		banfile.delete();
	}

	public void addToBanfile(String name) {
		try {
			FileWriter fileWriter = new FileWriter(banfile);

			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write(name);
			bufferedWriter.newLine();
			bufferedWriter.close();
		} catch (IOException e) {
			AFKPGC.logger.warning("Error while trying to add " + name
							+ " to the banned players file");
		}
	}

	public static void parseBanlist() {
		if (banfile == null && !banfile.exists()) {
			return;
		}
		String line = null;
		try {
			FileReader fileReader = new FileReader(banfile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {
				bannedPlayers.add(line);
			}
			bufferedReader.close();
		} catch (IOException ex) {
			AFKPGC.logger.warning("Error while trying to parse the banned players file");
		}
	}

}
