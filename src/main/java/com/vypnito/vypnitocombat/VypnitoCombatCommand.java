package com.vypnito.vypnitocombat;

import com.vypnito.vypnitocombat.managers.CombatManager;
import com.vypnito.vypnitocombat.managers.ConfigManager;
import com.vypnito.vypnitocombat.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VypnitoCombatCommand implements CommandExecutor, TabCompleter {

	private final VypnitoCombat plugin;
	private final MessageManager messageManager;
	private final CombatManager combatManager;
	private final ConfigManager configManager;

	public VypnitoCombatCommand(VypnitoCombat plugin) {
		this.plugin = plugin;
		this.messageManager = plugin.getMessageManager();
		this.combatManager = plugin.getCombatManager();
		this.configManager = plugin.getConfigManager();
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(messageManager.getMessage("command_usage_main"));
			return true;
		}

		String subCommand = args[0].toLowerCase();

		switch (subCommand) {
			case "reload":
				handleReload(sender);
				break;
			case "help":
				handleHelp(sender);
				break;
			case "status":
				handleStatus(sender, args);
				break;
			case "tag":
				handleTag(sender, args);
				break;
			case "untag":
				handleUntag(sender, args);
				break;
			default:
				sender.sendMessage(messageManager.getMessage("command_unknown_subcommand"));
				break;
		}
		return true;
	}

	private void handleReload(CommandSender sender) {
		if (!sender.hasPermission("combatlogv.reload")) {
			sender.sendMessage(messageManager.getMessage("no_permission"));
			return;
		}
		plugin.reloadPlugin();
		sender.sendMessage(messageManager.getMessage("plugin_reloaded"));
	}

	private void handleHelp(CommandSender sender) {
		sender.sendMessage(messageManager.getMessage("command_help_header"));
		sender.sendMessage(messageManager.getMessage("command_help_reload"));
		sender.sendMessage(messageManager.getMessage("command_help_bypass_perm"));
		sender.sendMessage(messageManager.getMessage("command_help_footer"));
	}

	private void handleStatus(CommandSender sender, String[] args) {
		if (!sender.hasPermission("combatlogv.status")) {
			sender.sendMessage(messageManager.getMessage("no_permission"));
			return;
		}
		if (args.length != 2) {
			sender.sendMessage(messageManager.getMessage("command_usage_clv_status"));
			return;
		}

		Player target = Bukkit.getPlayer(args[1]);
		if (target == null) {
			sender.sendMessage(messageManager.getMessage("player_not_found").replace("%player%", args[1]));
			return;
		}

		if (combatManager.isInCombat(target)) {
			long remaining = (combatManager.getCombatEndTime(target.getUniqueId()) - System.currentTimeMillis()) / 1000;
			sender.sendMessage(messageManager.getMessage("status_in_combat")
					.replace("%player%", target.getName())
					.replace("%time%", String.valueOf(remaining)));
		} else {
			sender.sendMessage(messageManager.getMessage("status_not_in_combat").replace("%player%", target.getName()));
		}
	}

	private void handleTag(CommandSender sender, String[] args) {
		if (!sender.hasPermission("combatlogv.tag")) {
			sender.sendMessage(messageManager.getMessage("no_permission"));
			return;
		}
		if (args.length < 2 || args.length > 3) {
			sender.sendMessage(messageManager.getMessage("command_usage_clv_tag"));
			return;
		}

		Player target = Bukkit.getPlayer(args[1]);
		if (target == null) {
			sender.sendMessage(messageManager.getMessage("player_not_found").replace("%player%", args[1]));
			return;
		}

		int seconds = configManager.getCombatDurationSeconds();
		if (args.length == 3) {
			try {
				seconds = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				sender.sendMessage(messageManager.getMessage("invalid_number"));
				return;
			}
		}

		combatManager.enterCombat(target, seconds);
		sender.sendMessage(messageManager.getMessage("player_tagged").replace("%player%", target.getName()));
	}

	private void handleUntag(CommandSender sender, String[] args) {
		if (!sender.hasPermission("combatlogv.untag")) {
			sender.sendMessage(messageManager.getMessage("no_permission"));
			return;
		}
		if (args.length != 2) {
			sender.sendMessage(messageManager.getMessage("command_usage_clv_untag"));
			return;
		}

		Player target = Bukkit.getPlayer(args[1]);
		if (target == null) {
			sender.sendMessage(messageManager.getMessage("player_not_found").replace("%player%", args[1]));
			return;
		}

		combatManager.exitCombat(target, CombatExitReason.MANUAL);
		sender.sendMessage(messageManager.getMessage("player_untagged").replace("%player%", target.getName()));
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		if (args.length == 1) {
			List<String> subCommands = new ArrayList<>(Arrays.asList("reload", "help", "status", "tag", "untag"));
			return subCommands.stream()
					.filter(s -> s.startsWith(args[0].toLowerCase()))
					.filter(s -> sender.hasPermission("combatlogv." + s))
					.collect(Collectors.toList());
		}
		if (args.length == 2 && (args[0].equalsIgnoreCase("status") ||
				args[0].equalsIgnoreCase("tag") ||
				args[0].equalsIgnoreCase("untag"))) {

			return null; // Allows default player name completion
		}
		return new ArrayList<>();
	}
}