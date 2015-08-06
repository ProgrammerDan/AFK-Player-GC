package com.github.Kraken3.AFKPGC.commands;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.github.Kraken3.AFKPGC.AFKPGC;
import com.github.Kraken3.AFKPGC.BotDetector;

public class FreeEveryone extends AbstractCommand {

	public FreeEveryone(AFKPGC instance) {
		super(instance, "freeeveryone");
	}

	@Override
	public boolean onCommand(CommandSender sender, List<String> args) {
		AFKPGC.debug("Freeing everyone as suggested by command");
		BotDetector.freeEveryone();
		sender.sendMessage("Everyone banned by this plugin was freed");
		return true;
	}

}