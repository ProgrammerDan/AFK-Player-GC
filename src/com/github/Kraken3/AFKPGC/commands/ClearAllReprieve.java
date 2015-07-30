package com.github.Kraken3.AFKPGC.commands;

import org.bukkit.command.CommandSender;

import java.util.List;

import com.github.Kraken3.AFKPGC.AFKPGC;
import com.github.Kraken3.AFKPGC.BotDetector;

public class ClearAllReprieve extends AbstractCommand {

	public ClearAllReprieve(AFKPGC instance) {
		super(instance, "clearallreprieve");
	}

	@Override
	public boolean onCommand(CommandSender sender, List<String> args) {
		sender.sendMessage("Clearing All Reprieves (not threadsafe, be careful!)");
		AFKPGC.detector.clearReprieves();
		return true;
	}

}


