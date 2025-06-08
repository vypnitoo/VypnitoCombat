package com.vypnito.vypnitocombat.managers;

import com.vypnito.vypnitocombat.CombatExitReason;
import com.vypnito.vypnitocombat.VypnitoCombat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CombatManager {

	private final VypnitoCombat plugin;
	// dbManager has been removed
	private final Map<UUID, Long> combatTimers;
	private final Map<UUID, BukkitTask> combatTasks;
	private final Map<UUID, Long> elytraCooldowns;

	// The constructor is simple again
	public CombatManager(VypnitoCombat plugin) {
		this.plugin = plugin;
		this.combatTimers = new HashMap<>();
		this.combatTasks = new HashMap<>();
		// Cooldowns are now initialized as an empty in-memory map
		this.elytraCooldowns = new HashMap<>();
	}

	// ... enterCombat and isInCombat methods remain the same ...
	public boolean isInCombat(Player player) { return combatTimers.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis(); }
	public void enterCombat(Player player, int seconds) { if (player.hasPermission("vypnitocombat.bypass.combat")) return; boolean wasInCombat = isInCombat(player); long newCombatEndTime = System.currentTimeMillis() + (seconds * 1000L); combatTimers.put(player.getUniqueId(), newCombatEndTime); if (combatTasks.containsKey(player.getUniqueId())) { combatTasks.get(player.getUniqueId()).cancel(); } BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> { Long currentEndTime = combatTimers.get(player.getUniqueId()); if (currentEndTime != null && currentEndTime.equals(newCombatEndTime)) { exitCombat(player, CombatExitReason.NATURAL_TIMEOUT); } }, (long) seconds * 20L); combatTasks.put(player.getUniqueId(), task); if (!wasInCombat) { player.sendMessage(plugin.getMessageManager().getMessage("enter_combat") .replace("%seconds%", String.valueOf(seconds))); } }
	public void enterCombat(Player player) { enterCombat(player, plugin.getConfigManager().getCombatDurationSeconds()); }

	public void exitCombat(Player player, CombatExitReason reason) {
		if (!combatTimers.containsKey(player.getUniqueId())) {
			return;
		}
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
			long expiresAt = System.currentTimeMillis() + (elytraCooldown * 1000L);
			elytraCooldowns.put(player.getUniqueId(), expiresAt);
			// The call to dbManager.saveCooldown() is removed
			player.sendMessage(plugin.getMessageManager().getMessage("elytra_cooldown_started").replace("%time%", String.valueOf(elytraCooldown)));
		}
		int pearlCooldown = plugin.getConfigManager().getEnderPearlCombatCooldownSeconds();
		if (pearlCooldown > 0) {
			player.setCooldown(Material.ENDER_PEARL, pearlCooldown * 20);
		}
	}

	public boolean isElytraOnCooldown(Player player) {
		if (!elytraCooldowns.containsKey(player.getUniqueId())) {
			return false;
		}
		if (elytraCooldowns.get(player.getUniqueId()) < System.currentTimeMillis()) {
			elytraCooldowns.remove(player.getUniqueId());
			// The call to dbManager.removeCooldown() is removed
			return false;
		}
		return true;
	}

	// ... all other methods (getElytraCooldownRemainingSeconds, handleCombatLog, getters) remain the same ...
	public long getElytraCooldownRemainingSeconds(Player player) { if (!isElytraOnCooldown(player)) return 0; long remainingMillis = elytraCooldowns.get(player.getUniqueId()) - System.currentTimeMillis(); return (long) Math.ceil(remainingMillis / 1000.0); }
	public void handleCombatLog(Player player) { if (plugin.getConfigManager().shouldPunishmentKillPlayer()) { player.setHealth(0.0); } List<String> commands = plugin.getConfigManager().getPunishmentCommands(); for (String command : commands) { String processedCommand = command.replace("%player%", player.getName()); Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand); } }
	public Map<UUID, Long> getCombatTimers() { return combatTimers; }
	public Long getCombatEndTime(UUID uuid) { return combatTimers.get(uuid); }
}