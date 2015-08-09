package com.github.Kraken3.AFKPGC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

public class ConfigurationReader {
	public static boolean readConfig() {

		// Note: savedefaultconfig only writes data if config.yml doesn't exist.
		AFKPGC.plugin.saveDefaultConfig();
		AFKPGC.plugin.reloadConfig();
		FileConfiguration conf = AFKPGC.plugin.getConfig();

		AFKPGC.debug = conf.getBoolean("debug", false);
		AFKPGC.debug("Enabled");
		
		int max_players = AFKPGC.plugin.getServer().getMaxPlayers();
		AFKPGC.logger.info("Server reports maximum of " + max_players + " players");

		int[] kickThresholds = new int[max_players];
		for (int i = 0; i < max_players; i++)
			kickThresholds[i] = -1;

		List<String> ktl = conf.getStringList("kick_thresholds");
		int[] nums = new int[3];
		for (String s : ktl) {
			nums[0] = nums[1] = nums[2] = -1;
			parseNaturals(s, nums);
			int min = nums[0], max = nums[1], t = nums[2];
			AFKPGC.logger.info("Between " + min + " and " + max + " kick after " + t + " seconds.");
			if (min > max || min < 1 || max < 1 || t < 0) {
				AFKPGC.logger.warning("Configuration file error: " + s);
				return false;
			}

			for (int i = min; i <= max; i++) {
				if (i <= max_players) {
					if (kickThresholds[i - 1] != -1)
						AFKPGC.logger.config("Previously defined threshold getting redefined in: "
										+ s);
					kickThresholds[i - 1] = t;
				} else {
					break; // don't keep going if we're not gonna use it.
				}
			}
		}

		boolean foundEmptyThreshold = false;
		int gapStart = -1;
		int gapEnd = -1;
		for (int i = 0; i < max_players; i++) {
			if (kickThresholds[i] == -1) {
				if (gapStart > -1) {
					gapEnd = i;
				} else {
					gapStart = i;
					gapEnd = i;
				}
				foundEmptyThreshold = true;
			} else if (gapStart > -1) {
				AFKPGC.logger.warning("Configuration file incomplete - plugin doesn't know when to kick players when there are between "
						+ (gapStart + 1) + " and "
						+ (gapEnd + 1) + " players online");
				gapStart = -1;
				gapEnd = -1;
			}

		}

		if (foundEmptyThreshold) {
			if (gapStart > -1) {
				AFKPGC.logger.warning("Configuration file incomplete - plugin doesn't know when to kick players when there are between "
						+ (gapStart + 1) + " and "
						+ (gapEnd + 1) + " players online");
			}
			return false;
		}

		Set<UUID> immuneAccounts = new HashSet<UUID>();
		for (String account_id : conf.getStringList("immune_accounts")) {
			try {
				UUID uuid = UUID.fromString(account_id);
				// TODO: When Bukkit gets their act together with the account
				// ID migrations and makes Server.getOfflinePlayer(UUID)
				// reasonably efficient, validate the account ID is a real
				// player on this server.
				immuneAccounts.add(uuid);
				AFKPGC.debug("Adding immune_account UUID: ", uuid);
			} catch (Exception ex) {
				AFKPGC.logger.info("Invalid UUID in immune_accounts: "
						+ account_id);
			}
		}

		ConfigurationSection bd = conf.getConfigurationSection("bot_detector");
		//BotDetector
		BotDetector.observationMode = bd.getBoolean("observation_mode");
		AFKPGC.debug("Observation mode: ", BotDetector.observationMode);
		BotDetector.acceptableTPS = (float) bd.getDouble("acceptable_TPS");
		AFKPGC.debug("Acceptable TPS: ", BotDetector.acceptableTPS);
		BotDetector.frequency = bd.getInt("kicking_frequency");
		AFKPGC.debug("Detector Frequency: ", BotDetector.frequency);
		BotDetector.enableBans = bd.getBoolean("enable_bans");
		AFKPGC.debug("Activate Bans: ", BotDetector.enableBans);
		BotDetector.enableWarnings = bd.getBoolean("enable_warnings");
		AFKPGC.debug("Activate Warnings: ", BotDetector.enableWarnings);
		BotDetector.maxLocations = bd.getInt("max_locations");
		AFKPGC.debug("Max Locs to Track: ", BotDetector.maxLocations);
		BotDetector.maxReprieve = bd.getInt("max_reprieve");
		AFKPGC.debug("Max Reprieve Rounds: ", BotDetector.maxReprieve);
		BotDetector.releaseRounds = bd.getInt("release_rounds");
		AFKPGC.debug("Rounds Before Release: ", BotDetector.releaseRounds);
		BotDetector.longBan = bd.getLong("ban_length");
		AFKPGC.debug("Ban Length: ", BotDetector.longBan);
		BotDetector.scanRadius = bd.getInt("scan_radius");
		AFKPGC.debug("Impact Scan Radius: ", BotDetector.scanRadius);
		BotDetector.amountOfChecksPerRun = bd.getInt("players_checked_per_run");
		AFKPGC.debug("Players checked per run:", BotDetector.amountOfChecksPerRun);
		BotDetector.maxKicksPerRun = bd.getInt("max_kicked_per_run");
		AFKPGC.debug("Players kicked per run (max):", BotDetector.maxKicksPerRun);
		BotDetector.actionThreshold = (float) bd.getDouble("action_threshold");
		AFKPGC.debug("Action Threshold:", BotDetector.actionThreshold);
		BotDetector.safeDistance = bd.getInt("safe_distance");
		AFKPGC.debug("Safe distance: ",BotDetector.safeDistance); //unused but keeping
		ConfigurationSection bdbc = bd.getConfigurationSection("bounds"); //unused but keeping
		//BotDetector Bounds Configuration
		BotDetector.relaxationFactor = bdbc.getDouble("relaxation_factor");
		AFKPGC.debug("Bounding Box Relaxation Factor: ", BotDetector.relaxationFactor);
		double thresh = bdbc.getDouble("threshold", -1.0);
		double contain = bdbc.getDouble("contained", -1.0);
		double containY = bdbc.getDouble("contained_exclude_y", -1.0);
		double volume = bdbc.getDouble("volume_similar", -1.0);
		double surface = bdbc.getDouble("surface_similar", -1.0);
		double nearly = bdbc.getDouble("nearly_contained", -1.0);
		double nearlyY = bdbc.getDouble("nearly_contained_exclude_y", -1.0);
		if (thresh < 0.0 || contain < 0.0 || containY < 0.0 || volume < 0.0 || surface < 0.0 ||
				nearly < 0.0 || nearlyY < 0.0)  {
			AFKPGC.logger.warning("Configuration Invalid: bounding box interpretation configuration " +
					"not specified or uses negative numbers!");
			return false;
		} else {
			AFKPGC.debug("Bot on Bounds Threshold: ", thresh);
			AFKPGC.debug("    Contained Weight: ", contain);
			AFKPGC.debug("    Contained Exc. Y Weight: ", containY);
			AFKPGC.debug("    Volume Similar Weight: ", volume);
			AFKPGC.debug("    Surface Similar Weight: ", surface);
			AFKPGC.debug("    Nearly Contained Weight: ", nearly);
			AFKPGC.debug("    Nearly Contained Exc. Y Weight: ", nearlyY);
		}
		BotDetector.boundsConfig = new BoundResultsConfiguration(thresh, contain, containY,
				volume, surface, nearly, nearlyY);

		ConfigurationSection ls = conf.getConfigurationSection("lag_scanner");
		//LagScanner
		LagScanner.fullScan = ls.getBoolean("full_scan");
		AFKPGC.debug("LagScanner Full scans: ",LagScanner.fullScan);
		LagScanner.cacheTimeout = ls.getLong("cache_timeout");
		AFKPGC.debug("LagScanner Cache Timeout: ", LagScanner.cacheTimeout);
		LagScanner.lagSourceThreshold = ls.getLong("lag_threshold");
		AFKPGC.debug("LagScanner Source Threshold: ", LagScanner.lagSourceThreshold);
		LagScanner.extremeLagSourceThreshold = (long) (LagScanner.lagSourceThreshold 
				* ls.getDouble("extreme_lag_threshold_multiplier"));
		AFKPGC.debug("LagScanner Extreme Lag Source Threshold: ",LagScanner.extremeLagSourceThreshold);
		LagScanner.unloadThreshold = (long) (LagScanner.lagSourceThreshold 
				* ls.getDouble("unload_threshold_factor"));
		AFKPGC.debug("LagScanner Chunk Unload Threshold: ",LagScanner.unloadThreshold);
		LagScanner.performUnload = ls.getBoolean("perform_unload");
		AFKPGC.debug("LagScanner Perform Chunk Unload: ", LagScanner.performUnload);
		LagScanner.normalChunkValue = ls.getInt("normal_chunk_value");
		AFKPGC.debug("LagScanner Normal Chunk Value: ",LagScanner.normalChunkValue);
		

		LagCostConfig.getInstance().clearCosts(); // TODO: change to get lock
		ConfigurationSection lstb = ls.getConfigurationSection("tick_block");
		for (String key : lstb.getKeys(false) ) {
			try {
				Material mat = Material.getMaterial(key);
				if (mat == null) {
					AFKPGC.logger.warning("Invalid material for tick block cost: " + key);
					continue;
				} else {
					int cost = lstb.getInt(key);
					LagCostConfig.getInstance().setCost(mat, cost);
					AFKPGC.debug("LagScanner Material: ", mat, " c/ea. ", cost);
				}
			} catch (Exception e) {
				AFKPGC.logger.warning("Exception while setting tick block cost: " + key);
				e.printStackTrace();
			}
		}

		ConfigurationSection lste = ls.getConfigurationSection("tick_entity");
		for (String key : lste.getKeys(false) ) {
			try {
				EntityType et = EntityType.valueOf(key);
				if (et == null) {
					AFKPGC.logger.warning("Invalid entity type for tick entity cost: " + key);
					continue;
				} else {
					int cost = lste.getInt(key);
					LagCostConfig.getInstance().setCost(et, cost);
					AFKPGC.debug("LagScanner Entity: ", et, " c/ea. ", cost);
				}
			} catch (Exception e2) {
				AFKPGC.logger.warning("Exception while setting tick entity cost: " + key);
				e2.printStackTrace();
			}
		}

		//Kicker

		ArrayList<Warning> warnings = new ArrayList<Warning>();
		ktl = conf.getStringList("warnings");
		for (String s : ktl) {
			s = s.trim();
			int slen = s.length();
			StringBuilder sb = new StringBuilder();
			int n = 0;
			boolean numberPart = true;
			for (int i = 0; i < slen; i++) {
				char c = s.charAt(i);
				if (numberPart && c >= '0' && c <= '9') {
					n = n * 10 + c - '0';
				} else {
					numberPart = false;
					sb.append(c);
				}
			}
			Warning warn = new Warning(n * 1000, sb.toString().trim()); 
			warnings.add(warn);
			AFKPGC.debug("Warning added: ", warn);
		}

		Warning[] wa = warnings.toArray(new Warning[warnings.size()]);

		Kicker.message_on_kick = conf.getString("kick_message");
		AFKPGC.debug("Kick Message: ", Kicker.message_on_kick);
		Kicker.warnings = wa;
		Kicker.kickThresholds = kickThresholds;
		AFKPGC.immuneAccounts = immuneAccounts;

		
		return true;
	}

	/**
	 * attempt to fill numbers but quietly fail.
	 */
	public static void parseNaturals(String str, int[] numbers) {
		Pattern num = Pattern.compile("[0-9]+");
		Matcher findnum = num.matcher(str);
		for (int i = 0; i < numbers.length; i++) {
			if (findnum.find()) {
				String cmatch = findnum.group();
				try {
					numbers[i] = Integer.valueOf(cmatch);
				} catch (NumberFormatException nfe) {
					break;
				}
			} else {
				break;
			}
		}
	}
}
