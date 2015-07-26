package com.github.Kraken3.AFKPGC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigurationReader {
	public static boolean readConfig() {

		// Note: savedefaultconfig only writes data if config.yml doesn't exist.
		AFKPGC.plugin.saveDefaultConfig();
		AFKPGC.plugin.reloadConfig();
		FileConfiguration conf = AFKPGC.plugin.getConfig();

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
			} catch (Exception ex) {
				AFKPGC.logger.info("Invalid UUID in immune_accounts: "
						+ account_id);
			}
		}

		ConfigurationSection bd = conf.getConfigurationSection("bot_detector");
		//BotDetector
		BotDetector.acceptableTPS = bd.getInt("acceptable_TPS");
		BotDetector.criticalTPSChange = (float) bd.getDouble("critical_TPS_Change");
		BotDetector.frequency = bd.getInt("kicking_frequency");
		BotDetector.longBans = bd.getBoolean("long_bans");
		BotDetector.maxLocations = bd.getInt("max_locations");
		BotDetector.maxSuspects = bd.getInt("max_suspects");
		BotDetector.maxReprieve = bd.getInt("max_reprieve");
		BotDetector.minBaselineMovement = bd.getInt("min_baseline");
		BotDetector.longBan = bd.getLong("ban_length");
		BotDetector.scanRadius = bd.getInt("scan_radius");
		ConfigurationSection bdbc = bd.getConfigurationSection("bounds");
		//BotDetector Bounds Configuration
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
		}
		BotDetector.boundsConfig = new BoundResultsConfiguration(thresh, contain, containY,
				volume, surface, nearly, nearlyY);

		ConfigurationSection ls = conf.getConfigurationSection("lag_scanner");
		//LagScanner
		LagScanner.cacheTimeout = ls.getLong("cache_timeout");
		LagScanner.lagSourceThreshold = ls.getLong("bot_threshold")

		LagCostConfig.getInstance().clear(); // TODO: change to get lock
		ConfigurationSection lstb = ls.getConfigurationSection("tick_block");
		for (String key : lstb.getKeys(false) ) {
			try {
				Material mat = Material.getMaterial(key);
				if (mat == null) {
					AFKPGC.logger.warning("Invalid material for tick block cost: " + key);
					continue;
				} else {
					int cost = lstb.getInt(key);
					LagCostConfig.getInstance().addCost(mat, cost);
				}
			} catch (Exception e) {
				AFKPGC.logger.warning("Exception while setting tick block cost: " + key);
				AFKPGC.logger.warning(e);
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
					LagCostConfig.getInstance().addCost(et, cost);
				}
			} catch (Exception e2) {
				AFKPGC.logger.warning("Exception while setting tick entity cost: " + key);
				AFKPGC.logger.warning(e2);
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
			warnings.add(new Warning(n * 1000, sb.toString().trim()));
		}

		int wlen = warnings.size();
		Warning[] wa = new Warning[wlen];
		for (int i = 0; i < wlen; i++)
			wa[i] = warnings.get(i);

		Kicker.message_on_kick = conf.getString("kick_message");
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
