package com.vypnito.vypnitocombat.listeners;

import com.vypnito.vypnitocombat.VypnitoCombat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

	private final VypnitoCombat plugin;

	public JoinListener(VypnitoCombat plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		// Notify players with the correct permission if an update is available.
		if (player.hasPermission("vypnitocombat.update.notify")) {
			if (plugin.isUpdateAvailable()) {
				// Send the message with a small delay to ensure it's visible after other join messages.
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
					String message = plugin.getMessageManager().getMessage("update_available")
							.replace("%latest_version%", plugin.getLatestVersion())
							.replace("%current_version%", plugin.getDescription().getVersion());
					player.sendMessage(message);
				}, 60L); // 3-second delay (60 ticks)
			}
		}
	}
}