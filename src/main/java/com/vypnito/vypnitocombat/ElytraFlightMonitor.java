package com.vypnito.vypnitocombat;

import com.vypnito.vypnitocombat.managers.CombatManager;
import com.vypnito.vypnitocombat.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ElytraFlightMonitor extends BukkitRunnable {

	private final CombatManager combatManager;
	private final MessageManager messageManager;

	public ElytraFlightMonitor(VypnitoCombat plugin) {
		this.combatManager = plugin.getCombatManager();
		this.messageManager = plugin.getMessageManager();
	}

	@Override
	public void run() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!player.isGliding()) {
				continue;
			}

			if (player.hasPermission("vypnitocombat.bypass.combat")) {
				continue;
			}
			if (combatManager.isInCombat(player) || combatManager.isElytraOnCooldown(player)) {
				if (player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType() == Material.ELYTRA) {
					if (combatManager.isInCombat(player)) {
						player.sendMessage(messageManager.getMessage("combat_block_elytra"));
					} else { // Must be on cooldown if not in combat.
						long remaining = combatManager.getElytraCooldownRemainingSeconds(player);
						player.sendMessage(messageManager.getMessage("elytra_cooldown_active").replace("%time%", String.valueOf(remaining)));
					}
					player.setGliding(false);
				}
			}
		}
	}
}