package com.github.Kraken3.AFKPGC.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.github.Kraken3.AFKPGC.AFKPGC;
import com.github.Kraken3.AFKPGC.BotDetector;

public class GetLongBans extends AbstractCommand {

	public GetLongBans(AFKPGC instance) {
		super(instance, "getlongbans");
	}

	@Override
	public boolean onCommand(CommandSender sender, List<String> args) {
		if (BotDetector.enableBans)
			sender.sendMessage("Bans are currently enabled in the bot detector");
		else
			sender.sendMessage("Bans are currently not enabled in the bot detector");
		return true;
	}
}
