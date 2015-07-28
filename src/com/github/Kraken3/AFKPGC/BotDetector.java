package com.github.Kraken3.AFKPGC;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.TreeMap;
import java.util.HashMap;

import org.bukkit.BanList;
import org.bukkit.BanEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/*
 * Detects likely bots based on movement patterns and lag contributions
 * @author Maxopoly
 * @author ProgrammerDan
 * 
 */
public class BotDetector implements Runnable {
	public static boolean longBans;
	public static float currentTPS = 20;
	public static float acceptableTPS;
	public static float criticalTPSChange;
	public static double relaxationFactor; // TODO
	public static int maxLocations; // TODO
	public static int maxSuspects; // TODO
	public static int maxReprieve; // TODO
	public static int minBaselineMovement; // TODO
	public static long longBan; //TODO in milliseconds
	public static int scanRadius; // TODO in chunks
	public static BoundResultsConfiguration boundsConfig; // TODO
	public static long frequency; // how often this runs in ticks
	public static File banfile;
	float lastRoundTPS;

	TreeMap<Integer, Suspect> topSuspects;
	HashMap<UUID, Integer> reprieve; // temp. cleared suspects
	
	Suspect lastRoundSuspect = null;
	
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
		if (reprieve == null) {
			reprieve = new HashMap<UUID, Integer>();
		} else {
			// decrement reprieve, regardless of plugin's active status.
			for (Iterator<Map.Entry<UUID, Integer>> i = reprieve.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry<UUID, Integer> entry = i.next();
				Integer roundsLeft = entry.getValue() - 1;
				if (roundsLeft <= 0) { // no more reprieve
					AFKPGC.debug("Reprieve is up for ", entry.getKey());
					i.remove();
				} else {
					AFKPGC.debug("Reprieve decremented for ", entry.getKey());
					entry.setValue(roundsLeft);
				}
			}
		}

		if (!AFKPGC.enabled) {
			return;
		}
		
		currentTPS = TpsReader.getTPS();
		AFKPGC.debug("Bot Detector Running, TPS is: ", currentTPS);
		Map<UUID, LastActivity> lastActivities = LastActivity.lastActivities;
		if (currentTPS < acceptableTPS) {
			topSuspects.clear();
			int smallestMovedDistance = minBaselineMovement;
			// find new top suspects
			for (Map.Entry<UUID, LastActivity> entry : lastActivities.entrySet()) {
				UUID playerUUID = entry.getKey();
				Player p = Bukkit.getPlayer(playerUUID);
				/* according to the author of AFKGPC, there might be
				 * inconsistencies in this list, so this additional
				 * check is needed */
				// TODO: See if inconsistencies might be thread-safeness related.
				if (lastActivities.containsKey(playerUUID) && p != null) {
					LastActivity la = entry.getValue();
					la.loggedLocations.add(p.getLocation());
					if (la.loggedLocations.size() >= maxLocations) {
						if (la.loggedLocations.size() > maxLocations) {
							la.loggedLocations.removeFirst();
						}
						// we tracking location even if on reprieve or immune, but that's it
						if (!reprieve.containsKey(playerUUID) && !AFKPGC.immuneAccounts.contains(playerUUID)) {
							int itWasntMeISwear = la.calculateMovementRadius();
							if (itWasntMeISwear < smallestMovedDistance) {
								smallestMovedDistance = itWasntMeISwear;
								Player dirtyLiar = Bukkit.getPlayer(playerUUID);

								topSuspects.put(itWasntMeISwear, new Suspect(
										playerUUID, dirtyLiar.getName(), dirtyLiar.getLocation(),
										la.evaluateBounds(relaxationFactor) ) );
								AFKPGC.debug("Player ", playerUUID, " added as suspect (movement was ",
										itWasntMeISwear, ")");

								if (topSuspects.size() > maxSuspects) {
									Suspect cleared = topSuspects.pollLastEntry().getValue(); // gets rid of largest distance
									AFKPGC.debug("Player ", cleared.getUUID(), " released as suspect, better suspects found");
								}
							}
						} else {
							AFKPGC.debug("Skipping ", playerUUID, " due to reprieve or immunity");
						}
					} else {
						AFKPGC.debug("Skipping ", playerUUID, " due to insufficient location data");
					}
				} else {
					AFKPGC.debug("Player ", playerUUID, " likely offline.");
				}
			}
			Suspect thisRoundSuspect = null;
			// Now find first top suspect to pass the truebot tests.
			if (topSuspects.size() > 0) {
				for (Map.Entry<Integer, Suspect> entry : topSuspects.entrySet()) {
					Suspect curSuspect = entry.getValue();
					// Test Bounds for truebot(tm) detection.
					BoundResults bounds = curSuspect.getResults();
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
							AFKPGC.debug("Player ", curSuspect.getUUID(), " looks like a bot (bounds test ", 
									truebot, "): ", bounds);

							// Now test surrounding area.
							Location point = curSuspect.getLocation();

							LagScanner ls = new LagScanner(point, scanRadius, null);
							ls.run(); // TODO: move this and ban results to thread.
							if (ls.isLagSource()) {
								thisRoundSuspect = curSuspect;
								Date currentDate = new Date();
								long bantime = (long) (frequency / currentTPS * 1000);
								/* Because the ban time is based on real time and the next run
								 * of this method is based on the tick, the ban time needs to be
								 * adjusted to the tick. The long form of this would be:
								 * (20/currentTPS) * (frequency/20) * 1000
								 * The time is in ms */
								BanEntry leBan = banList.addBan(curSuspect.getName(),
										"You were suspected to cause lag and banned for "
												+ (bantime / 1000) + " seconds",
												new Date(currentDate.getTime() + bantime), null);
								Player p = Bukkit.getPlayer(curSuspect.getUUID());
								if (p != null) {
					   				p.kickPlayer(leBan.getReason());
								}
								// ban the player briefly and skip the other suspects.
								AFKPGC.debug("Player ", curSuspect.getUUID(), " (", curSuspect.getName(),
										") exceeded ban threshold with ", ls.getLagCompute(), " banned for",
										bantime / 1000, " milliseconds");
								break;
							} else {
								AFKPGC.debug("Player ", curSuspect.getUUID(), " (", curSuspect.getName(),
										") cleared via insufficient lagsources [", ls.getLagCompute(), "]");
							}
						} else {
							AFKPGC.debug("Player ", curSuspect.getUUID(), " (", curSuspect.getName(),
									") cleared via bounding box [", truebot, "]");
						}
						// It passed the truebot(tm) detection -- for now. Give a temporary reprieve.
						reprieve.put(curSuspect.getUUID(), maxReprieve);
						AFKPGC.debug("Player ", curSuspect.getUUID(), " unlikely bot, given reprieve (bounds test ",
								truebot, "): ", bounds);
					} else {
						// else not enough info yet. Pass.
						AFKPGC.debug("Player ", curSuspect.getUUID(), " Not enough bounds data, skip for now");
					}
				}
			} else {
				AFKPGC.debug("No suspects this round.");
			}

			if (lastRoundSuspect != null) {
				// TODO: This is starting to stink of synchronization issues. We shouldn't need
				//       to check immune so often within the same compute round.
				if (!AFKPGC.immuneAccounts.contains(lastRoundSuspect.getUUID())) {
					if (currentTPS - lastRoundTPS > criticalTPSChange) {
						/* This can be relatively sensitive, because it will only ban players for 
						 * a longer period of time, if it catches them twice here */
						Date currentDate = new Date();
						if (suspectedBotters.contains(lastRoundSuspect.getName())) {
							if (longBans) {
								BanEntry leBan = banList.addBan( lastRoundSuspect.getName(),
										"Kicking you resulted in a noticeable TPS improvement, so you " +
										"were banned until the TPS goes back to normal values.",
										new Date(currentDate.getTime() + longBan),
										null); // long ban.
								Player p = Bukkit.getPlayer(lastRoundSuspect.getUUID());
								if (p != null) {
					   				p.kickPlayer(leBan.getReason());
								}
								bannedPlayers.add(lastRoundSuspect.getName());
								addToBanfile(lastRoundSuspect.getName());
								suspectedBotters.remove(lastRoundSuspect.getName());
								AFKPGC.debug("Player ", lastRoundSuspect.getUUID(), " long banned for ",
										longBan," confirmed lag source.");
							}
							AFKPGC.logger.info("The player " + lastRoundSuspect.getName()
									+ " causes lag and is a repeated offender, kicking him resulted"
									+ " in a TPS improvement of " + String.valueOf(currentTPS - lastRoundTPS)
									+ " at the location " + lastRoundSuspect.getLocation().toString());

						} else {
							suspectedBotters.add(lastRoundSuspect.getName());
							AFKPGC.logger.info( "The player " + lastRoundSuspect.getName()
									+ " is suspected to cause lag, kicking him resulted in a TPS improvement of "
									+ String.valueOf(currentTPS - lastRoundTPS) + " at the location "
									+ lastRoundSuspect.getLocation().toString());
						}
					} else {
						AFKPGC.debug("Player ", lastRoundSuspect.getUUID(), " (", lastRoundSuspect.getName(),
								") cleared, kicking them did not improve TPS -- granting a reprieve");
						reprieve.put(lastRoundSuspect.getUUID(), maxReprieve);
					}
				} else {
					AFKPGC.debug("Player ", lastRoundSuspect.getUUID(), " was suspected but is immune.");
				}
			} else {
				AFKPGC.debug("No prior suspects to evaluable.");
			}

			lastRoundTPS = currentTPS;
			lastRoundSuspect = thisRoundSuspect;
		} else { // TPS is high enough
			// TODO: Evaluate if this makes sense. We should probably see if TPS stays good for a while
			//       first, then clear bans. E.g. this sets or decrements a counter.
			if (bannedPlayers.size() != 0) {
				AFKPGC.debug("TPS has improved, removing bans");
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
