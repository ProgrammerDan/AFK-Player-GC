import org.bukkit.Location;
import org.bukkit.Chunk;

/**
 * Should be runnable with callback. First iteration will be straight mainthread code, b/c testing.
 * 
 * @author ProgrammerDan
 */
public class LagScanner() {

	private static Map<String, Map<Long, LagScanner.Result>> cache = 
			new HashMap<String, Map<Long, LagScanner.Result>>();

	private Location center;
	private Integer radius;
	private ScanCallback<Boolean> callback;

	private Boolean isLagSource;

	public LagScanner(Location center, Integer radius, ScanCallback<Boolean> callback) {
		this.center = center;
		this.radius = radius;
		this.callback = callback;
		this.isLagSource = null;
	}

	public static LagScanner instance(Location center, Integer radius, ScanCallback<Boolean> callback ){
		return new LagScanner(center, radius, callback);
	}

	public void run() {
		boolean lagSource;
		Chunk originChunk = center.getChunk();


		this.isLagSource = lagSource
		if (callback != null) {
			callback.callback(lagSource);
		}
	}

	public void testChunk(//TODO

	public boolean isLagSource() {
		return (isLagSource == null) ? false : isLagSource;
	}

	public boolean hasAnswer() {
		return isLagSource == null;
	}

	class Result {
		public String worldId;
		public long chunkId;
		public int chunkX;
		public int chunkZ;
		public boolean lagSource;
		public long lastUpdate;

		public Result(String worldId, long chunkId, int chunkX, int chunkZ, boolean lagSource, long lastUpdate) {
			this.worldId = worldId;
			this.chunkId = chunkId;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
			this.lagSource = lagSource;
			this.lastUpdate = lastUpdate;
		}
	}
}
