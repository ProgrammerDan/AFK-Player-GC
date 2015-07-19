package com.github.Kraken3.AFKPGC.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.github.Kraken3.AFKPGC.AFKPGC;
import com.github.Kraken3.AFKPGC.BotDetector;

public class getCriticalTPSChange extends AbstractCommand {

	public getCriticalTPSChange(AFKPGC instance) {
		super(instance, "getcriticaltpschange");
	}

	@Override
	public boolean onCommand(CommandSender sender, List<String> args) {
		sender.sendMessage("The current critical TPS change for the bot detector is: "+BotDetector.criticalTPSChange);
		return true;
	}

}