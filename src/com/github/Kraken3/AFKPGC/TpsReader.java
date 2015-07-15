package com.github.Kraken3.AFKPGC;

public class TpsReader implements Runnable { // There has to be a way to read
												// this right from Spigot, but I
												// couldnt find anything about
												// it
	static float TPS=20f;
	boolean started = false;
	int counter = 0;
	long starttime;

	public void run() {
		if (!started) {
			starttime = System.currentTimeMillis();
			started = true;
		}
		if (starttime + 5000 <= System.currentTimeMillis()) {
			TPS = counter / 5;
			counter = 0;
			started = false;
		} else {
			counter++;
		}
	}
	public static float getTPS() {
		return TPS;
	}
}
