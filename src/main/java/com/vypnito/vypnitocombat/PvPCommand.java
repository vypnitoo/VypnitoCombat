package com.vypnito.vypnitocombat;

import com.vypnito.vypnitocombat.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class PvPCommand implements CommandExecutor, TabCompleter {

	private final VypnitoCombat plugin;
	private final MessageManager messageManager;

	public PvPCommand(VypnitoCombat plugin) {
		this.plugin = plugin;
		this.messageManager = plugin.getMessageManager();
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!sender.hasPermission("combatlogv.pvp.manage")) {
			sender.sendMessage(messageManager.getMessage("no_permission"));
			return true;
		}

		if (args.length != 2 || !args[0].equalsIgnoreCase("global")) {
			sender.sendMessage(messageManager.getMessage("command_usage_pvp"));
			return true;
		}

		if (args[1].equalsIgnoreCase("on")) {
			plugin.getConfig().set("global_pvp_enabled", true);
			plugin.saveConfig();
			sender.sendMessage(messageManager.getMessage("pvp_globally_enabled"));
			return true;
		}

		if (args[1].equalsIgnoreCase("off")) {
			plugin.getConfig().set("global_pvp_enabled", false);
			plugin.saveConfig();
			sender.sendMessage(messageManager.getMessage("pvp_globally_disabled"));
			return true;
		}

		sender.sendMessage(messageManager.getMessage("command_usage_pvp"));
		return true;
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		List<String> completions = new ArrayList<>();
		String currentArg = args[args.length - 1].toLowerCase();

		if (args.length == 1) {
			if ("global".startsWith(currentArg)) {
				completions.add("global");
			}
		}
		else if (args.length == 2 && args[0].equalsIgnoreCase("global")) {
			if ("on".startsWith(currentArg)) {
				completions.add("on");
			}
			if ("off".startsWith(currentArg)) {
				completions.add("off");
			}
		}

		return completions;
	}
}