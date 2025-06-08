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

			// Kontrolujeme, zda hráč letí na elytře
			if (player.isGliding() && player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType() == Material.ELYTRA) {
				boolean stopped = false;

				// Podmínka 1: Hráč je v boji
				if (combatManager.isInCombat(player)) {
					player.sendMessage(messageManager.getMessage("combat_block_elytra"));
					stopped = true;
				}
				// Podmínka 2: Hráč je v cooldownu po boji (pokud nebyl zastaven už první podmínkou)
				else if (combatManager.isElytraOnCooldown(player)) {
					long remaining = combatManager.getElytraCooldownRemainingSeconds(player);
					player.sendMessage(messageManager.getMessage("elytra_cooldown_active").replace("%time%", String.valueOf(remaining)));
					stopped = true;
				}

				// Pokud byla splněna jakákoliv blokující podmínka, zastavíme let
				if (stopped) {
					player.setGliding(false);
					// Mírný "šťouchanec" dolů, aby se zabránilo okamžité reaktivaci
					if (!player.isOnGround()) {
						player.teleport(player.getLocation().subtract(0, 0.1, 0));
					}
				}
			}
		}
	}
}