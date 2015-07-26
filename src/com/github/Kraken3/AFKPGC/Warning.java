package com.github.Kraken3.AFKPGC;

/**
 * Lightweight warning message concerning kick nearness.  
 */
class Warning{
	public int time;	
	public String message;	
	public Warning (int time, String message){ 
		this.time = time;
		this.message = message;
	}
	
	@Override
	public String toString() {
		return "[" + time + "]: " + message;
	}
}