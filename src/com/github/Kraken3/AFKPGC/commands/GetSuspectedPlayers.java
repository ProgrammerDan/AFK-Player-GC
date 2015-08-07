package com.github.Kraken3.AFKPGC.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.github.Kraken3.AFKPGC.AFKPGC;
import com.github.Kraken3.AFKPGC.BotDetector;
import com.github.Kraken3.AFKPGC.Suspect;

public class GetSuspectedPlayers extends AbstractCommand {

	public GetSuspectedPlayers(AFKPGC instance) {
		super(instance, "getsuspectedplayers");
	}

	@Override
	public boolean onCommand(CommandSender sender, List<String> args) {
		StringBuilder s = new StringBuilder();
		for (Suspect wp : BotDetector.warnedPlayers) {
			s.append(wp.getUUID()).append(", ");
		}
		sender.sendMessage("Currently suspected by the botdetector are: " +
				(s.length() > 0 ? s.substring(0, s.length() - 2) : s.toString()));
		return true;
	}

}
