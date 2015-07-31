package com.github.Kraken3.AFKPGC;

import org.bukkit.Location;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import java.util.Map;
import java.util.HashMap;

/**
 * Should be runnable with callback. First iteration will be straight mainthread code, b/c testing.
 * 
 * TODO: Make a runnable, redesign BotDetector to play well with deferred answers.
 * 
 * @author ProgrammerDan
 */
public class LagScanner {

	private static Map<String, Map<Long, LagScanner.Result>> cache = 
			new HashMap<String, Map<Long, LagScanner.Result>>();

	public static long cacheTimeout;
	public static long lagSourceThreshold;
	public static long extremelagSourceThreshold;
	public static boolean fullScan;

	private Location center;
	private Integer radius;
	private ScanCallback<Boolean> callback;

	private Boolean isLagSource;
	private Long lagCompute;

	public LagScanner(Location center, Integer radius, ScanCallback<Boolean> callback) {
		this.center = center;
		this.radius = radius;
		this.callback = callback;
		this.isLagSource = null;
		this.lagCompute = null;
	}

	public static LagScanner instance(Location center, Integer radius, ScanCallback<Boolean> callback ){
		return new LagScanner(center, radius, callback);
	}

	public void run() {
		boolean lagSource = false;
		Chunk originChunk = center.getChunk();
		World chunkWorld = originChunk.getWorld();
		int oX = originChunk.getX();
		int oZ = originChunk.getZ();
		long now = System.currentTimeMillis(); // all tests against same millis ... for now.
		long lagSum = 0L;
		int chunksTested = 0;

		// radius in minecraft is square.
		for (int r = 0; r <= radius && !lagSource; r++) {
			for (int x = oX - r; x <= oX + r && !lagSource; x++) {
				for (int z = oZ - r; z <= oZ + r; z++) {
					if (x > oX - r && x < oX + r &&
						z > oZ - r && z < oZ + r) {
						continue; // don't retread inner chunks of prior radius
					}
					LagScanner.Result test = testChunk(chunkWorld.getChunkAt(x, z), now);
					chunksTested ++;
					lagSum += test.lagContrib;
					if (lagSum >= lagSourceThreshold) {
						lagSource = true;
						if (!fullScan) {
							break;
						}
					}
				}
			}
		}

		this.isLagSource = lagSource;
		this.lagCompute = lagSum;
		AFKPGC.debug("LagScanner completed in ", (System.currentTimeMillis() - now), "ms centered on ", 
				center, " found lag weight ", lagSum, " after scanning ",
				chunksTested, " and ", (lagSource) ? "is ": "is not ", "a lag source");
		if (callback != null) {
			callback.callback(lagSource);
		}
	}

	public LagScanner.Result testChunk(Chunk chunk, long now) {
		String world = chunk.getWorld().getName();
		long chunkId = (long) chunk.getX() << 32L + (long) chunk.getZ();
		Map<Long, LagScanner.Result> worldCache = null;
		if (!LagScanner.cache.containsKey(world)) {
			worldCache = new HashMap<Long, LagScanner.Result>();
			cache.put(world, worldCache);
		} else {
			worldCache = cache.get(world);
		}

		LagScanner.Result result = null;
		if (worldCache.containsKey(chunkId)) {
			LagScanner.Result cachedResult = worldCache.get(chunkId);
			if (cachedResult.lastUpdate + cacheTimeout > now) {
				// hasn't exceeded cache timeout, so use it.
				result = cachedResult;
			} else {
				// cache expired, remove.
				worldCache.remove(chunkId);
			}
		} // else not cached

		if (result == null) {
			Map<String, Long> stats = new HashMap<String, Long>();
			// not in the cache, so let's compute.
			long totalCost = 0L;

			// Test tiles
			BlockState[] lagTiles = chunk.getTileEntities();
			for (BlockState tile : lagTiles) {
				Material tiletype = tile.getType();
				long tilecost = (long) LagCostConfig.getInstance().cost(tiletype);
				totalCost += tilecost;
				if (stats.containsKey(tiletype.name())) {
					stats.put(tiletype.name(), tilecost + stats.get(tiletype.name()));
				} else {
					stats.put(tiletype.name(), tilecost);
				}
				if (totalCost >= lagSourceThreshold && !fullScan) {
					break; // if we cross the threshold, don't keep adding.
				}
			}

			if (totalCost < lagSourceThreshold) {
				// Test entities
				Entity[] lagEntity = chunk.getEntities();
				for (Entity entity : lagEntity) {
					EntityType enttype = entity.getType();
					long entcost = (long) LagCostConfig.getInstance().cost(enttype);
					totalCost += entcost;
					if (stats.containsKey(enttype.name())) {
						stats.put(enttype.name(), entcost + stats.get(enttype.name()));
					} else {
						stats.put(enttype.name(), entcost);
					}
					if (totalCost >= lagSourceThreshold && !fullScan) {
						break; // if we cross the threshold, don't keep adding.
					}
				}
			}
			StringBuffer sb = new StringBuffer();
			for (Map.Entry<String, Long> stat : stats.entrySet()) {
				sb.append(stat.getKey()).append(": ").append(stat.getValue()).append("  ");
			}				
			// record the result.
			result = new LagScanner.Result(world, chunkId, chunk.getX(), chunk.getZ(), totalCost, now);
			worldCache.put(chunkId, result);
			AFKPGC.debug("The chunk ", chunk.getX(), ", ", chunk.getZ(), " alone measures ", result.lagContrib, " lag sources, details: ", sb);
		}
		else {
			AFKPGC.debug("The chunk ", chunk.getX(), ", ", chunk.getZ(), " was loaded from the cache with a value of ",result.lagContrib);
		}
		return result;
	}

	public boolean isLagSource() {
		return (isLagSource == null) ? false : isLagSource;
	}
	
	public boolean isExtremeLagSource() {
		if (lagCompute!=null && lagCompute > extremelagSourceThreshold) {
			return true;
		}
		return false;
	}
	
	public long getLagCompute() {
		return (lagCompute == null) ? -1L : lagCompute;
	}

	public boolean hasAnswer() {
		return isLagSource == null;
	}

	class Result {
		public String worldId;
		public long chunkId;
		public int chunkX;
		public int chunkZ;
		public long lagContrib;
		public long lastUpdate;

		public Result(String worldId, long chunkId, int chunkX, int chunkZ, long lagContrib, long lastUpdate) {
			this.worldId = worldId;
			this.chunkId = chunkId;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
			this.lagContrib = lagContrib;
			this.lastUpdate = lastUpdate;
		}
	}
}
