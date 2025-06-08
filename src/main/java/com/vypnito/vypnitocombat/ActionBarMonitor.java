package com.vypnito.vypnitocombat;

import com.vypnito.vypnitocombat.managers.CombatManager;
import com.vypnito.vypnitocombat.managers.MessageManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class ActionBarMonitor extends BukkitRunnable {

	private final CombatManager combatManager;
	private final MessageManager messageManager;

	public ActionBarMonitor(VypnitoCombat plugin) {
		this.combatManager = plugin.getCombatManager();
		this.messageManager = plugin.getMessageManager();
	}

	@Override
	public void run() {
		for (UUID playerUUID : combatManager.getCombatTimers().keySet()) {
			Player player = Bukkit.getPlayer(playerUUID);
			if (player == null || !player.isOnline()) {
				continue;
			}

			if (combatManager.isInCombat(player)) {
				Long endTime = combatManager.getCombatEndTime(player.getUniqueId());
				if (endTime != null) {
					long remainingMillis = endTime - System.currentTimeMillis();
					if (remainingMillis > 0) {
						double remainingSeconds = remainingMillis / 1000.0;
						String formattedTime = String.format("%.1f", remainingSeconds);
						String message = messageManager.getMessage("action_bar_combat_timer").replace("%time%", formattedTime);
						player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
					}
				}
			}
		}
	}
}