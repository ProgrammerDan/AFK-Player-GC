package com.github.Kraken3.AFKPGC.commands;

import org.bukkit.command.CommandSender;

import java.util.List;

import com.github.Kraken3.AFKPGC.AFKPGC;
import com.github.Kraken3.AFKPGC.BotDetector;

public class ForceBotDetector extends AbstractCommand {

	public ForceBotDetector(AFKPGC instance) {
		super(instance, "forcebotdetector");
	}

	@Override
	public boolean onCommand(CommandSender sender, List<String> args) {
		sender.sendMessage("Special invocation of BotDetector.");
		AFKPGC.detector.doDetector();
		return true;
	}

}

