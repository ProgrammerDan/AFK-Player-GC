package com.github.Kraken3.AFKPGC;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BotDetector implements Runnable {
	static float currentTPS = 20;
	static float acceptableTPS;
	static float criticalTPSChange;
	static long frequency; // how often this runs in ticks
	float lastRoundTPS;
	UUID topSuspect = null;
	String topSuspectName = ""; // needs to be stored additionally,
								// because retrieving the name off
								// players after kicking them is only
								// possible
								// over deprecated/buggy methods
	Location topSuspectsLocation = null;
	UUID lastRoundSuspect = null;
	String lastRoundSuspectName = "";
	Location lastRoundSuspectsLocation = null;
	static LinkedList<UUID> suspectedBotters = new LinkedList<UUID>();
	static LinkedList<String> bannedPlayers = new LinkedList<String>();
	// this is needed as a separated list, so we know the difference between
	// players who were banned by AFKPGC and players who were banned for other
	// reasons

	static BanList banList = AFKPGC.plugin.getServer().getBanList(
			BanList.Type.NAME); // ban after names not ips

	public void run() {
		if (!AFKPGC.enabled) {
			return;
		}
		currentTPS = TpsReader.getTPS();
		Map<UUID, LastActivity> lastActivities = LastActivity.lastActivities;
		if (currentTPS < acceptableTPS) {
			int smallestMovedDistance = 1024;
			Iterator<Map.Entry<UUID, LastActivity>> entries = lastActivities
					.entrySet().iterator();
			while (entries.hasNext()) {
				Map.Entry<UUID, LastActivity> entry = entries.next();
				UUID playerUUID = entry.getKey();
				if (lastActivities.containsKey(playerUUID)) { // according to
																// the author of
																// AFKGPC, there
																// might be
																// inconsistencies
																// in this list,
																// so this
																// additional
																// check is
																// needed
					LastActivity la = entry.getValue();
					la.loggedLocations.add(Bukkit.getPlayer(playerUUID)
							.getLocation());
					if (la.loggedLocations.size() >= 5) {
						if (la.loggedLocations.size() > 5) {
							la.loggedLocations.removeFirst();
						}
						int itWasntMeISwear = la.calculateMovementradius();
						if (itWasntMeISwear < smallestMovedDistance) {
							smallestMovedDistance = itWasntMeISwear;
							Player dirtyLiar = Bukkit.getPlayer(playerUUID);

							topSuspect = playerUUID;
							topSuspectsLocation = dirtyLiar.getLocation();
							topSuspectName = dirtyLiar.getName();
						}

					}

				}
			}
			if (topSuspect != null) {
				if (!AFKPGC.immuneAccounts.contains(topSuspect)) {
					Date currentDate = new Date();
					long bantime = (long) (frequency / currentTPS * 1000);
					// Because the ban time is based on real time and the next
					// run
					// of this method is based on the tick, the ban time needs
					// to be
					// adjusted to the tick. The long form of this would be:
					// (20/currentTPS) * (frequency/20) * 1000
					// The time is in ms
					banList.addBan(topSuspectName,
							"You were suspected to cause lag and banned for "
									+ bantime / 1000 + " seconds", new Date(
									currentDate.getTime() + bantime), null);
					// ban the player for ~ a minute
				}
			}
			if (lastRoundSuspect != null) {
				if (!AFKPGC.immuneAccounts.contains(lastRoundSuspect)) {
					if (currentTPS - lastRoundTPS > criticalTPSChange) { // value
																			// needs
																			// to
																			// be
						// configured, that was
						// just the first thing
						// that came to mind.
						// This can be
						// relatively sensitive,
						// because it will only
						// ban players for a
						// longer period of
						// time, if it catches
						// them twice here
						Date currentDate = new Date();
						if (suspectedBotters.contains(lastRoundSuspect)) {
							banList.addBan(
									lastRoundSuspectName,
									"Kicking you resulted in a noticeable TPS improvement, so you were banned until the TPS goes back to normal values.",
									new Date(
											currentDate.getTime() + 6 * 3600 * 1000),
									null); // 6h ban
							AFKPGC.logger
									.log(AFKPGC.logger.getLevel(),
											"The player "
													+ lastRoundSuspectName
													+ " causes lag and is a repeated offender, kicking him resulted in a TPS improvement of "
													+ String.valueOf(currentTPS
															- lastRoundTPS)
													+ " at the location "
													+ lastRoundSuspectsLocation
															.toString());

						} else {
							suspectedBotters.add(lastRoundSuspect);
							AFKPGC.logger
									.log(AFKPGC.logger.getLevel(),
											"The player "
													+ lastRoundSuspectName
													+ " is suspected to cause lag, kicking him resulted in a TPS improvement of "
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
			lastRoundSuspectsLocation = topSuspectsLocation; // as preparation
																// for the next
																// run;
		} else { // TPS is high enough
			if (bannedPlayers.size() != 0) {
				freeEveryone(); // not everyone, but everyone banned by this
								// plugin
			}
		}

	}

	public static void freeEveryone() {
		for (int i = 0; i < bannedPlayers.size(); i++) {
			banList.pardon(bannedPlayers.get(i));
		}
	}

}
