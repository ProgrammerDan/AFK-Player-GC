package com.github.Kraken3.AFKPGC.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.github.Kraken3.AFKPGC.AFKPGC;
import com.github.Kraken3.AFKPGC.ConfigurationReader;

public class ToggleDebug extends AbstractCommand{
	public ToggleDebug(AFKPGC instance) {
		super(instance, "toggledebug");
	}

	@Override
	public boolean onCommand(CommandSender sender, List<String> args) {
		AFKPGC.debug = !AFKPGC.debug;
		if (AFKPGC.debug)
			sender.sendMessage("Debug mode turned on, logging will be verbose");
		else
			sender.sendMessage("Debug mode turned off, logging will be terse");
		return true;
	}
}

