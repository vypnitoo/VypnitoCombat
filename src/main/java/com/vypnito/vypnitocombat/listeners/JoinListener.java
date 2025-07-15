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
		if (player.hasPermission("vypnitocombat.update.notify")) {
			if (plugin.isUpdateAvailable()) {
				plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
					String message = plugin.getMessageManager().getMessage("update_available")
							.replace("%latest_version%", plugin.getLatestVersion())
							.replace("%current_version%", plugin.getDescription().getVersion());
					player.sendMessage(message);
				}, 60L);
			}
		}
	}
}