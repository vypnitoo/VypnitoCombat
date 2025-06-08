package com.vypnito.vypnitocombat.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ConfigUpdater {

	/**
	 * Updates a configuration file by appending only the missing keys from the default config.
	 * This version correctly handles both single keys and entire sections.
	 *
	 * @param plugin   The instance of your plugin.
	 * @param fileName The name of the file to update (e.g., "config.yml").
	 */
	public static void update(JavaPlugin plugin, String fileName) {
		File configFile = new File(plugin.getDataFolder(), fileName);
		if (!configFile.exists()) {
			plugin.saveResource(fileName, false);
			return;
		}

		FileConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);
		InputStream defaultStream = plugin.getResource(fileName);
		if (defaultStream == null) return;
		FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));

		// Find all keys that are in the default config but not in the user's config
		List<String> missingKeys = new ArrayList<>();
		for (String key : defaultConfig.getKeys(true)) {
			if (!userConfig.isSet(key)) {
				missingKeys.add(key);
			}
		}

		if (missingKeys.isEmpty()) {
			return;
		}

		plugin.getLogger().info("Updating '" + fileName + "' with new settings...");

		try (FileWriter fileWriter = new FileWriter(configFile, true); // Append mode
			 BufferedWriter writer = new BufferedWriter(fileWriter)) {

			writer.newLine();
			writer.newLine();
			writer.write("# --- Automatically added by VypnitoCombat Updater ---");
			writer.newLine();

			// Create a temporary config to hold ONLY the new keys to preserve their structure
			FileConfiguration newKeysConfig = new YamlConfiguration();
			for (String key : missingKeys) {
				// This simple logic correctly handles both single values and whole sections.
				// The .set() method automatically creates the necessary parent paths.
				newKeysConfig.set(key, defaultConfig.get(key));
			}

			// Save the new keys as a string and write it to the file
			writer.write(newKeysConfig.saveToString());
			writer.flush();

		} catch (IOException e) {
			plugin.getLogger().severe("Could not update configuration file: " + fileName);
			e.printStackTrace();
		}
	}
}