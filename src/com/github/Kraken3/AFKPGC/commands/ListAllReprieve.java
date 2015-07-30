package com.github.Kraken3.AFKPGC.commands;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.github.Kraken3.AFKPGC.AFKPGC;
import com.github.Kraken3.AFKPGC.BotDetector;

public class ListAllReprieve extends AbstractCommand {

	public ListAllReprieve(AFKPGC instance) {
		super(instance, "listallreprieve");
	}

	@Override
	public boolean onCommand(CommandSender sender, List<String> args) {
		sender.sendMessage("All Reprieves:");
		Set<UUID> reprieves = AFKPGC.detector.listReprieves();
		if (reprieves.isEmpty()) {
			sender.sendMessage("   No reprieves.");
		}
		for (UUID u : reprieves) {
			sender.sendMessage("   " + u);
		}
		return true;
	}

}



