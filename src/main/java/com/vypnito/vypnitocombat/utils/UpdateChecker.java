package com.vypnito.vypnitocombat.utils;

import com.vypnito.vypnitocombat.VypnitoCombat;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

	private final VypnitoCombat plugin;

	public UpdateChecker(VypnitoCombat plugin) {
		this.plugin = plugin;
	}

	public void check() {
		int resourceId = plugin.getConfigManager().getSpigotResourceId();
		if (resourceId == 0 || resourceId == 99999) { // 99999 is a common placeholder
			plugin.getLogger().warning("Update checker is enabled, but no valid Spigot Resource ID is configured in config.yml.");
			return;
		}

		// Run asynchronously to prevent lagging the main server thread
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
					String latestVersion = reader.readLine();
					String currentVersion = plugin.getDescription().getVersion();

					if (isVersionGreater(latestVersion, currentVersion)) {
						plugin.setUpdateAvailable(true, latestVersion);
						plugin.getLogger().info("A new version of VypnitoCombat is available: " + latestVersion);
						plugin.getLogger().info("You are currently running version: " + currentVersion);
					} else {
						plugin.getLogger().info("You are running the latest version of VypnitoCombat.");
					}
				}
			} catch (Exception e) {
				plugin.getLogger().warning("Could not check for updates: " + e.getMessage());
			}
		});
	}

	private boolean isVersionGreater(String version1, String version2) {
		try {
			String[] parts1 = version1.split("\\.");
			String[] parts2 = version2.split("\\.");
			int length = Math.max(parts1.length, parts2.length);
			for (int i = 0; i < length; i++) {
				int part1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
				int part2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
				if (part1 > part2) return true;
				if (part1 < part2) return false;
			}
			return false;
		} catch (NumberFormatException e) {
			// Can't compare versions with text like "BETA", assume no update.
			return false;
		}
	}
}