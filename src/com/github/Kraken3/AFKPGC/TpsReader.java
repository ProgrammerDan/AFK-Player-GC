package com.github.Kraken3.AFKPGC;

import org.bukkit.Server;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import java.util.logging.Logger;

public class TpsReader {

	public static float getTPS() {
		float latestTPS = (float) ((MinecraftServer) ((CraftServer)Bukkit.getServer()).getServer()).recentTps[0];
		return (latestTPS > 20.0f) ? 20.0f : latestTPS;
	}
}
