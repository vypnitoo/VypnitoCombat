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

	public PlayerQuitListener(VypnitoCombat plugin) {
		this.combatManager = plugin.getCombatManager();
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (player.hasPermission("combatlogv.bypass.combat")) {
			combatManager.exitCombat(player, CombatExitReason.DISCONNECT);
			return;
		}

		if (combatManager.isInCombat(player)) {
			combatManager.handleCombatLog(player);
		}
		combatManager.exitCombat(player, CombatExitReason.DISCONNECT);
	}
}