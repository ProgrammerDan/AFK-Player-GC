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
import java.util.NavigableSet;
import java.util.UUID;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.HashSet;
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
	public static float currentTPS = 20;
	public static float acceptableTPS;
	public static double relaxationFactor;
	public static int maxLocations;
	public static int maxReprieve;
	public static long longBan;
	public static int scanRadius;
	public static BoundResultsConfiguration boundsConfig;
	public static long frequency; // how often this runs in ticks
	public static File banfile;
	public static boolean observationMode;
	public static int releaseRounds;
	public static int amountOfChecksPerRun;
	public static int maxKicksPerRun;//TODO
	public static int safeDistance;
	/** Number of people scanned between suspects and reprieve list before kicks begin, as a function
	  * of online players. */
	public static float actionThreshold;

	/** Boolean indicator if bans are active. Warnings can be active without bans, for instance. */
	public static boolean enableBans;

	/** Boolean indicator if warnings are active. Bans can be active without warnings, for instance. */
	public static boolean enableWarnings;

	HashMap<UUID, Suspect> topSuspectsLookup; // UUID lookup.
	TreeMap<Long, Set<Suspect>> topSuspects;
	HashMap<UUID, Integer> reprieve; // temp. cleared suspects

	int goodRounds = 0;
	
	public static HashSet<Suspect> warnedPlayers = new HashSet<Suspect>();
	/* this is needed as a separated list, so we know the difference between players
	 * who were banned by AFKPGC and players who were banned for other reasons */
	//TODO: convert to UUID
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
		try {
			doDetector();
		} catch (Exception e) { // catchall b/c otherwise no future invocations.
			AFKPGC.debug("Encountered an error during Detector invocation: ", e);
			e.printStackTrace();
		}
		AFKPGC.debug("Next detector invocation: ",
				(long) ((double) BotDetector.frequency * (currentTPS / 20.0)), " in ticks");
		AFKPGC.plugin.getServer().getScheduler().scheduleSyncDelayedTask(
				AFKPGC.plugin, this, 
				(long) ((double) BotDetector.frequency * (currentTPS / 20.0)));
	}

	public synchronized void doDetector() {
		if (topSuspects == null) {
			topSuspects = new TreeMap<Long, Set<Suspect>>();
		}
		if (topSuspectsLookup == null) {
			topSuspectsLookup = new HashMap<UUID, Suspect>();
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
		if (currentTPS < acceptableTPS) {
			goodRounds = 0;
			int scanCount = 0;
			HashSet<UUID> justScanned = new HashSet<UUID>();
			for (Map.Entry<UUID, LastActivity> entry : lastActivities.entrySet()) {
				UUID playerUUID = entry.getKey();
				Player p = Bukkit.getPlayer(playerUUID);
				if (p == null) {
					AFKPGC.debug("Player ", playerUUID, " is likely offline, skipping for now");
					continue;
				} else if (lastActivities.containsKey(playerUUID)) { // threadsafe consistency check.
					LastActivity la = entry.getValue();

					// Find fresh blood.
					if (!AFKPGC.immuneAccounts.contains(playerUUID) && !reprieve.containsKey(playerUUID) &&
							!topSuspectsLookup.containsKey(playerUUID)) {
						// do scan.
						Location point = p.getLocation();
						AFKPGC.debug("Player ", playerUUID, " (", p.getName(), ") at ", point,
								" next up for Scanner");
						justScanned.add(playerUUID);

						LagScanner ls = new LagScanner(point, scanRadius, null, false);
						ls.run(); // TODO: move this and ban results to thread.
						if (ls.isLagSource()) {
							Suspect crim = new Suspect(playerUUID, p.getName(), point, ls.getLagCompute());
							Set<Suspect> cellmates = topSuspects.get(ls.getLagCompute());
							if (cellmates == null) {
								cellmates = new HashSet<Suspect>();
								topSuspects.put(ls.getLagCompute(), cellmates);
							}
							cellmates.add(crim);
							topSuspectsLookup.put(playerUUID, crim);
							AFKPGC.debug("Player ", playerUUID, " (", crim.getName(), ") at ", point,
									" exceeded baseline lag threshold [", ls.getLagCompute(), "], added to watch list");
						} else {
							reprieve.put(playerUUID, maxReprieve);
							AFKPGC.debug("Player ", playerUUID, " (", p.getName(), ") at ", point, 
									" below baseline lag threshold [", ls.getLagCompute(), "], given reprieve");
						}
						scanCount ++;
					} else {
						continue;
					}
					
					if (scanCount >= amountOfChecksPerRun) {
						break;
					}
					// Scan N people.

					// Evaluate if we've scanned enough people to issue warnings

					// Issue M warnings
					
					// if we're issuing a second warning, scan again

					// If scan is still bad, kick / ban (unless in obs. mode)

					// add to banlist

				} else {
					AFKPGC.debug("Player ", playerUUID, " (", p.getName(), ") was removed from tracking while Detector was running, skipping for now.");
				}
			}

			if (topSuspectsLookup.size() + reprieve.size() >= (int) Math.floor(lastActivities.size() * actionThreshold)) {
				AFKPGC.debug("Scanned ", topSuspectsLookup.size() + reprieve.size(), " players, ", 
						lastActivities.size(), " online/tracking, which exceeds ", 
						(int) Math.ceil(lastActivities.size() * actionThreshold), " so warning and kicking.");
				HashSet<Suspect> updates = new HashSet<Suspect>();
				Map<Suspect, Long> removeCellmates = new HashMap<Suspect, Long>();
				int curKicks = 0;
				// we're ready to warn or kick the top "baddies".
				for (Long cell : topSuspects.descendingKeySet()) {
					for (Suspect suspect : topSuspects.get(cell)) {
						// rescan and update/remove based on results.
						UUID suspectUUID = suspect.getUUID();

						Player p = Bukkit.getPlayer(suspectUUID);

						if (p != null && lastActivities.containsKey(suspectUUID)) {
							Long oldScore = suspect.getResults();

							Location point = p.getLocation();

							boolean recent = justScanned.contains(suspectUUID);
							boolean source = true;

							if (!recent) {
								AFKPGC.debug("Player ", suspectUUID, " (", suspect.getName(), ") at ",
										point, " is a top suspect, rescanning");
								LagScanner ls = new LagScanner(point, scanRadius, null, true);
								ls.run(); // TODO: move this and ban results to thread.
								suspect.update(point, ls.getLagCompute());
								source = ls.isLagSource();
							} else {
								AFKPGC.debug("Player ", suspectUUID, " (", suspect.getName(), ") at ",
										point, " is a top suspect!");
							}
							if (source) {
								curKicks ++;
								if (oldScore != suspect.getResults()) {
									removeCellmates.put(suspect, oldScore);
									updates.add(suspect);
								}
								if (warnedPlayers.contains(suspect)) {
									// kick, add to ban list.
									if (enableBans && !observationMode) {
										giveOutBan(suspect);
										// Once banned, remove from top list, update, and such.
										warnedPlayers.remove(suspect);
										if (updates.contains(suspect)) {
											updates.remove(suspect); // cancel update
										} else {
											removeCellmates.put(suspect, oldScore);
										}
										topSuspectsLookup.remove(suspectUUID);
									} else{
										AFKPGC.debug("Player ", suspectUUID, " (", suspect.getName(), ") at ", 
												suspect.getLocation(), " was warned but still exceeds baseline lag threshold [", 
												oldScore, "->", suspect.getResults(), "], kickban skipped due to config");
									}
								} else {
									// warn.
									if (enableWarnings && !observationMode) {
										warnPlayer(p);
										warnedPlayers.add(suspect);
									} else {
										AFKPGC.debug("Player ", suspectUUID, " (", suspect.getName(), ") at ",
												suspect.getLocation(), " exceeds baseline lag threshold [", 
												oldScore, "->", suspect.getResults(), "], warning skipped due to config");
									}
								}
							} else { // "good" now.
								removeCellmates.put(suspect, oldScore);
								topSuspectsLookup.remove(suspectUUID);
								AFKPGC.debug("Player ", suspectUUID, " (", suspect.getName(), ") at ",
										suspect.getLocation(), " no longer exceeds baseline lag threshold [", 
										oldScore, "->thresh], removed from watch list");
							}
						} else {
							AFKPGC.debug("Player ", suspectUUID, " (", suspect.getName(), 
									") is likely offline, skipping recheck for now");
							// remove from warned list, if there.
							if (warnedPlayers.contains(suspect)) {
								warnedPlayers.remove(suspect);
							}
						} // TODO remove after N skips?

						if (curKicks >= maxKicksPerRun) {
							break;
						}
					}
					if (curKicks >= maxKicksPerRun) {
						break;
					}
				}
				// remove removes and alter alters
				for (Map.Entry<Suspect, Long> cellmate : removeCellmates.entrySet()) {
					Set<Suspect> cm = topSuspects.get(cellmate.getValue());
					if (cm != null) { // else already gone...?
						cm.remove(cellmate.getKey());
						if (cm.size() <= 0) { // none left, purge
							topSuspects.remove(cellmate.getValue());
						}
					}
				}
				removeCellmates.clear();
				removeCellmates = null;
				// update updates
				for (Suspect update : updates) {
					Set<Suspect> cellmates = topSuspects.get(update.getResults());
					if (cellmates == null) {
						cellmates = new HashSet<Suspect>();
						topSuspects.put(update.getResults(), cellmates);
					}
					cellmates.add(update);
				}
				updates.clear();
				updates = null;
			} else {
				AFKPGC.debug("Scanned ", topSuspectsLookup.size() + reprieve.size(), " players, ", 
						lastActivities.size(), " online/tracking, need to reach ", 
						(int) Math.ceil(lastActivities.size() * actionThreshold), " before warning and kicking.");
			}
			justScanned.clear();
			justScanned = null;
		} else { // TPS is high enough
			goodRounds ++;
			if (goodRounds > releaseRounds && bannedPlayers.size() != 0) {
				AFKPGC.debug("TPS has remained improved, removing bans");
				freeEveryone(); // not everyone, but everyone banned by this plugin
				warnedPlayers.clear();
				reprieve.clear();
				// safe to clear lists, if they show up and wreck TPS we'll find them quickly.
				topSuspects.clear();
				topSuspectsLookup.clear();
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

    private void giveOutBan(Suspect s) {
    	if (!enableBans) {
    		return;
    	}
    	Date currentDate=new Date();
    	BanEntry leBan = banList.addBan( s.getName(),
	    		"You have been banned due to causing lag, in spite of a warning to leave the area",
				new Date(currentDate.getTime() + longBan), null); // long ban.
    	Player p = Bukkit.getPlayer(s.getUUID());
	    if (p != null) {
			LagScanner.unloadChunks(p.getLocation(), scanRadius);
			p.kickPlayer(leBan.getReason());
		}
		bannedPlayers.add(s.getName());
		addToBanfile(s.getName());
		AFKPGC.debug("Player ", s.getUUID(), " (", s.getName(), ") long banned for ",
				longBan," confirmed lag source.");
	}
    
    private void giveOutBan(Player p) {
    	if (p != null) {
    		giveOutBan(new Suspect(p.getUniqueId(),p.getName(),p.getLocation(),null));
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
	
	public void warnPlayer(Player p) {
		if (p != null) {
			p.sendMessage("[WARNING] You are in a region with a high concentration of lag sources."
					+ " Please immediately depart the area (leave render distance) or you will be "
					+ "temporarily banned.");
			AFKPGC.debug("Player ", p.getUniqueId(), " (", p.getName(), ") was notified of presence in lag source");
		}
		
	}

}
