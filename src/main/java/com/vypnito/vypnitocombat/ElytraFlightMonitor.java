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
			if (player.hasPermission("combatlogv.bypass.combat")) {
				continue;
			}
			if(!player.isGliding() || player.getInventory().getChestplate() == null || player.getInventory().getChestplate().getType() != Material.ELYTRA)
				return;

			boolean stopped = false;

			if (combatManager.isInCombat(player)) {
				player.sendMessage(messageManager.getMessage("combat_block_elytra"));
				stopped = true;
			}
			else if (combatManager.isElytraOnCooldown(player)) {
				long remaining = combatManager.getElytraCooldownRemainingSeconds(player);
				player.sendMessage(messageManager.getMessage("elytra_cooldown_active").replace("%time%", String.valueOf(remaining)));
				stopped = true;
			}

			if (stopped) { // Pokud byla splněna jakákoliv blokující podmínka, zastavíme let
				player.setGliding(false);
				// Mírný "šťouchanec" dolů, aby se zabránilo okamžité reaktivaci
				if (!player.isOnGround()) { // is writable by client side
					player.teleport(player.getLocation().subtract(0, 0.1, 0)); // this doesnt check if player is gonna be in a block?
				}
			}
		}
	}
}