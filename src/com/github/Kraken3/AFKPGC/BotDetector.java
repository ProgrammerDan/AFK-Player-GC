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
import java.util.List;
import java.util.ArrayList;

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
	public static float startingTPS;
	public static double relaxationFactor;
	public static int maxLocations;
	public static int maxSuspects;
	public static int maxReprieve;
	public static int minBaselineMovement;
	public static long longBan;
	public static int scanRadius;
	public static BoundResultsConfiguration boundsConfig;
	public static long frequency; // how often this runs in ticks
	public static File banfile;
	public static boolean kickNearby; // TODO addresses weakness of multiple people loading same lag machine
	public static int kickNearbyRadius; // TODO
	public static boolean observationMode;
	public static int releaseRounds;
	public static int amountOfChecksPerRun;

	TreeMap<Integer, Suspect> topSuspects;
	HashMap<UUID, Integer> reprieve; // temp. cleared suspects

	int goodRounds = 0;
	
	public static LinkedList<Suspect> warnedPlayers = new LinkedList<Suspect>();
	/* this is needed as a separated list, so we know the difference between players
	 * who were banned by AFKPGC and players who were banned for other reasons */
	public static LinkedList<String> bannedPlayers = new LinkedList<String>();

	// ban after names not ips
	static BanList banList = AFKPGC.plugin.getServer().getBanList(BanList.Type.NAME);

	/**
	 * Careful, sailor. This is not threadsafe (TODO) so don't call this while BotDetector is in the run() loop.
	 */
	public void clearReprieves() {
		reprieve.clear();
	}

	public Set<UUID> listReprieves() {
		return reprieve.keySet();
	}

	public void run() {
		currentTPS = TpsReader.getTPS();
		doDetector();
		AFKPGC.debug("Next detector invocation: ",
				(long) ((double) BotDetector.frequency * (currentTPS / 20.0)), " in ticks");
		AFKPGC.plugin.getServer().getScheduler().scheduleSyncDelayedTask(
				AFKPGC.plugin, this, 
				(long) ((double) BotDetector.frequency * (currentTPS / 20.0)));
	}

	public synchronized void doDetector() {
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
			goodRounds = 0;
			return;
		}
		
		currentTPS = TpsReader.getTPS();
		AFKPGC.debug("Bot Detector Running, TPS is: ", currentTPS);
		Map<UUID, LastActivity> lastActivities = LastActivity.lastActivities;
		if (currentTPS < startingTPS) {
			for (Map.Entry<UUID, LastActivity> entry : lastActivities.entrySet()) {
				UUID playerUUID = entry.getKey();
				Player p = Bukkit.getPlayer(playerUUID);
				if (lastActivities.containsKey(playerUUID) && p != null) {
					LastActivity la = entry.getValue();
					la.loggedLocations.add(p.getLocation());
					if (la.loggedLocations.size() > maxLocations) {
						la.loggedLocations.removeFirst();
					} 
					
				}
			}
		}
		if (currentTPS < acceptableTPS) {
			goodRounds = 0;
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
					if (!reprieve.containsKey(playerUUID) && !AFKPGC.immuneAccounts.contains(playerUUID)) {
						if (la.loggedLocations.size() == maxLocations) {
							int itWasntMeISwear = la.calculateMovementRadius();
							if (itWasntMeISwear < smallestMovedDistance) {
								Player dirtyLiar = Bukkit.getPlayer(playerUUID);
								topSuspects.put(itWasntMeISwear, new Suspect(
									playerUUID, dirtyLiar.getName(), dirtyLiar.getLocation(),
									la.evaluateBounds(relaxationFactor) ) );
								if (topSuspects.size() > maxSuspects) {
									Suspect cleared = topSuspects.pollLastEntry().getValue(); // gets rid of largest distance
									AFKPGC.debug("Player ", cleared.getUUID(), " released as suspect, better suspects found");
								}
							}
						} else {
							AFKPGC.debug("Skipping ", playerUUID, " due to insufficient location data");
						}
					} else {
						AFKPGC.debug("Skipping ", playerUUID, " due to reprieve or immunity");
					}
				} else {
					AFKPGC.debug("Player ", playerUUID, " likely offline.");
				}
			}
			// Now find first top suspect to pass the truebot tests.
			int peopleToCheck = amountOfChecksPerRun;
			if (peopleToCheck > topSuspects.size()) {
				peopleToCheck = topSuspects.size();
			}
			if (peopleToCheck > 0) {
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
							AFKPGC.debug("Player ", curSuspect.getUUID(), " (", curSuspect.getName(), 
									") looks like a bot (bounds test ", truebot, "): ", bounds);

							// Now test surrounding area.
							Location point = curSuspect.getLocation();

							LagScanner ls = new LagScanner(point, scanRadius, null);
							ls.run(); // TODO: move this and ban results to thread.
							if (ls.isLagSource()) {
								if (longBans && ls.isExtremeLagSource()) {
									if (!observationMode) {
										giveOutLongBan(curSuspect);
									}
									AFKPGC.debug("Player ", curSuspect.getUUID(), " (", curSuspect.getName(), 
											") is an extreme lag source with a lag compute of ", ls.getLagCompute());
								} else {
									if (!observationMode) {
										if (!warnedPlayers.contains(curSuspect)) {
											Player p = Bukkit.getPlayer(curSuspect.getUUID());
											warnPlayer(p);
											warnedPlayers.add(curSuspect);
											if (p != null) {
												if (BotDetector.kickNearby) {
													List<Player> nearby = getPlayersWithin(p, BotDetector.kickNearbyRadius);
	
													for (Player q : nearby) {
														warnPlayer(q);
														AFKPGC.debug("Player ", q.getUniqueId(), " (", q.getName(),
																") warned for being nearby lag source.");
													}
												}
											}
										}
										else {
												//The person has been warned, so we now ban him
											Player p = Bukkit.getPlayer(curSuspect.getUUID());
											if (p != null) {
												AFKPGC.debug(p.getUniqueId()," (",p.getName(),") was warned but didn't listen, so "
														+ "he was banned");
												giveOutLongBan(p);
											}
										}
									} else {
										AFKPGC.debug("Player ", curSuspect.getUUID(), " (", curSuspect.getName(),
												") exceeded warning threshold with ", ls.getLagCompute());
									}
									
									peopleToCheck--;
									if (peopleToCheck <= 0) {
										break;
									}
								}
							} else {
								AFKPGC.debug("Player ", curSuspect.getUUID(), " (", curSuspect.getName(),
										") cleared via insufficient lagsources [", ls.getLagCompute(), "]");
							}
						} else {
							AFKPGC.debug("Player ", curSuspect.getUUID(), " (", curSuspect.getName(),
									") unlikely bot, cleared via bounding box [", truebot, "]: ", bounds);
						}
						// It passed the truebot(tm) detection -- for now. Give a temporary reprieve.
						reprieve.put(curSuspect.getUUID(), maxReprieve);
					} else {
						// else not enough info yet. Pass.
						AFKPGC.debug("Player ", curSuspect.getUUID(), " suspected due to movement ",
								" but not enough bounds data, skip this round.");
					}
				}
			} else {
				AFKPGC.debug("No suspects this round.");
			}
		} else { // TPS is high enough
			goodRounds ++;
			if (goodRounds > releaseRounds && bannedPlayers.size() != 0) {
				AFKPGC.debug("TPS has remained improved, removing bans");
				freeEveryone(); // not everyone, but everyone banned by this plugin
				warnedPlayers.clear();
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
			AFKPGC.logger.warning("Error while trying to add " + name + " to the banned players file");
		}
	}

	public List<Player> getPlayersWithin(Player player, int distance) {
		List<Player> res = new ArrayList<Player>();
		int d2 = distance * distance;
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (p.getWorld() == player.getWorld() && p.getLocation().distanceSquared(player.getLocation()) <= d2) {
				res.add(p);
			}
		}
		return res;
	}

    private void giveOutLongBan(Suspect s) {
    	if (!longBans) {
    		return;
    	}
    	Date currentDate=new Date();
    	BanEntry leBan = banList.addBan( s.getName(),
	    		"You are causing lag, so you were banned",
				new Date(currentDate.getTime() + longBan), null); // long ban.
    	Player p = Bukkit.getPlayer(s.getUUID());
	    if (p != null) {
	    	if (BotDetector.kickNearby) {
			    List<Player> nearby = getPlayersWithin(p, BotDetector.kickNearbyRadius);
			    for (Player q : nearby) {
			    	BanEntry qBan = banList.addBan(q.getName(), leBan.getReason(),
						    new Date(currentDate.getTime() + longBan), null);
				    q.kickPlayer(leBan.getReason());
				    AFKPGC.debug("Player ", q.getUniqueId(), " (", q.getName(), ") long banned for ",
						    longBan," confirmed nearby lag source.");
				    bannedPlayers.add(q.getName());
					addToBanfile(q.getName());
			    }
	    	}
			LagScanner.unloadChunks(p.getLocation(), scanRadius);
			p.kickPlayer(leBan.getReason());
		}
		bannedPlayers.add(s.getName());
		addToBanfile(s.getName());
		warnedPlayers.remove(new Suspect(s.getUUID(),s.getName(),null,null));
		AFKPGC.debug("Player ", s.getUUID(), " (", s.getName(), ") long banned for ",
				longBan," confirmed lag source.");
	}
    
    private void giveOutLongBan(Player p) {
    	if (p != null) {
    		giveOutLongBan(new Suspect(p.getUniqueId(),p.getName(),p.getLocation(),null));
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
	
	public static void warnPlayer(Player p) {
		if (p != null) {
			p.sendMessage("[WARNING] You are in a region with a high concentration of lag sources."
					+ " Please immediately depart the area (leave render distance) or you will be "
					+ "temporarily banned.");
			AFKPGC.debug("Player ", p.getUniqueId(), " (", p.getName(), ") was notified of presence in lag source");
		}
		
	}

}
