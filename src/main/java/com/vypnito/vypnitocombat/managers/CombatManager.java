package com.vypnito.vypnitocombat.managers;

import com.vypnito.vypnitocombat.CombatExitReason;
import com.vypnito.vypnitocombat.VypnitoCombat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CombatManager {

	private final VypnitoCombat plugin;
	private final Map<UUID, Long> combatTimers;
	private final Map<UUID, BukkitTask> combatTasks;
	private final Map<UUID, Long> elytraCooldowns;
	private final Map<UUID, Set<Location>> visualBorderBlocks;

	public CombatManager(VypnitoCombat plugin) {
		this.plugin = plugin;
		this.combatTimers = new HashMap<>();
		this.combatTasks = new HashMap<>();
		this.elytraCooldowns = new HashMap<>();
		this.visualBorderBlocks = new HashMap<>();
	}

	public Set<Location> getVisibleBorderBlocks(UUID uuid) {
		return visualBorderBlocks.getOrDefault(uuid, new HashSet<>());
	}

	public void setVisibleBorderBlocks(UUID uuid, Set<Location> locations) {
		if (locations == null || locations.isEmpty()) {
			visualBorderBlocks.remove(uuid);
		} else {
			visualBorderBlocks.put(uuid, locations);
		}
	}

	public void clearVisualBorder(Player player) {
		Set<Location> locations = visualBorderBlocks.remove(player.getUniqueId());
		if (locations != null) {
			for (Location loc : locations) {
				player.sendBlockChange(loc, loc.getBlock().getBlockData());
			}
		}
	}

	public boolean isInCombat(Player player) {
		return combatTimers.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
	}

	public void enterCombat(Player player, int seconds) {
		if (player.hasPermission("vypnitocombat.bypass.combat")) return;
		boolean wasInCombat = isInCombat(player);
		long newCombatEndTime = System.currentTimeMillis() + ((long) seconds * 1000L);
		combatTimers.put(player.getUniqueId(), newCombatEndTime);
		if (combatTasks.containsKey(player.getUniqueId())) {
			combatTasks.get(player.getUniqueId()).cancel();
		}
		BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
			Long currentEndTime = combatTimers.get(player.getUniqueId());
			if (currentEndTime != null && currentEndTime.equals(newCombatEndTime)) {
				exitCombat(player, CombatExitReason.NATURAL_TIMEOUT);
			}
		}, (long) seconds * 20L);
		combatTasks.put(player.getUniqueId(), task);
		if (!wasInCombat) {
			player.sendMessage(plugin.getMessageManager().getMessage("enter_combat").replace("%seconds%", String.valueOf(seconds)));
		}
	}

	public void enterCombat(Player player) {
		enterCombat(player, plugin.getConfigManager().getCombatDurationSeconds());
	}

	public void exitCombat(Player player, CombatExitReason reason) {
		if (!combatTimers.containsKey(player.getUniqueId())) return;

		clearVisualBorder(player);
		combatTimers.remove(player.getUniqueId());

		if (combatTasks.containsKey(player.getUniqueId())) {
			combatTasks.get(player.getUniqueId()).cancel();
			combatTasks.remove(player.getUniqueId());
		}

		if (player.isOnline()) {
			if (reason != CombatExitReason.DEATH && reason != CombatExitReason.MANUAL) {
				applyPostCombatCooldowns(player);
			}
			if (reason == CombatExitReason.NATURAL_TIMEOUT) {
				player.sendMessage(plugin.getMessageManager().getMessage("exit_combat"));
			}
		}
	}

	private void applyPostCombatCooldowns(Player player) {
		int elytraCooldown = plugin.getConfigManager().getElytraCooldownSeconds();
		if (elytraCooldown > 0) {
			long expiresAt = System.currentTimeMillis() + ((long) elytraCooldown * 1000L);
			elytraCooldowns.put(player.getUniqueId(), expiresAt);
			player.sendMessage(plugin.getMessageManager().getMessage("elytra_cooldown_started").replace("%time%", String.valueOf(elytraCooldown)));
		}
		int pearlCooldown = plugin.getConfigManager().getEnderPearlCombatCooldownSeconds();
		if (pearlCooldown > 0) {
			player.setCooldown(Material.ENDER_PEARL, pearlCooldown * 20);
		}
	}

	public boolean isElytraOnCooldown(Player player) {
		Long expiration = elytraCooldowns.get(player.getUniqueId());
		if (expiration == null) return false;
		if (expiration < System.currentTimeMillis()) {
			elytraCooldowns.remove(player.getUniqueId());
			return false;
		}
		return true;
	}

	public long getElytraCooldownRemainingSeconds(Player player) {
		if (!isElytraOnCooldown(player)) return 0;
		long remainingMillis = elytraCooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
		return (long) Math.ceil(remainingMillis / 1000.0);
	}

	public void handleCombatLog(Player player) {
		if (plugin.getConfigManager().shouldPunishmentKillPlayer()) {
			player.setHealth(0.0);
		}
		List<String> commands = plugin.getConfigManager().getPunishmentCommands();
		for (String command : commands) {
			String processedCommand = command.replace("%player%", player.getName());
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
		}
	}

	public Map<UUID, Long> getCombatTimers() { return combatTimers; }
	public Long getCombatEndTime(UUID uuid) { return combatTimers.get(uuid); }
}