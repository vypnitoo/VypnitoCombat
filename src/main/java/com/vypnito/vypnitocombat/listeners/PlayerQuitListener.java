package com.vypnito.vypnitocombat.listeners;

import com.vypnito.vypnitocombat.CombatExitReason;
import com.vypnito.vypnitocombat.VypnitoCombat;
import com.vypnito.vypnitocombat.managers.CombatManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

	private final CombatManager combatManager;

	// The constructor is simple again, it does not need CombatListener.
	public PlayerQuitListener(VypnitoCombat plugin) {
		this.combatManager = plugin.getCombatManager();
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		// Clear any fake blocks the player might have been seeing when they log out.
		combatManager.clearVisualBorder(player);

		if (player.hasPermission("vypnitocombat.bypass.combat")) {
			combatManager.exitCombat(player, CombatExitReason.DISCONNECT);
			return;
		}

		if (combatManager.isInCombat(player)) {
			combatManager.handleCombatLog(player);
		}
		combatManager.exitCombat(player, CombatExitReason.DISCONNECT);
	}
}