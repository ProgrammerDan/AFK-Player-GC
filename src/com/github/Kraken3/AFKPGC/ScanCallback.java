package com.github.Kraken3.AFKPGC;

/**
 * For asynchronous chunk scanning, who to tell we are don
 * 
 * @author ProgrammerDan
 */
public interface ScanCallback<T> {
	public void callback(T result);
}
