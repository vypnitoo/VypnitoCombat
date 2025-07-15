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

		try (FileWriter fileWriter = new FileWriter(configFile, true);
			 BufferedWriter writer = new BufferedWriter(fileWriter)) {

			writer.newLine();
			writer.newLine();
			writer.write("# --- Automatically added by VypnitoCombat Updater ---");
			writer.newLine();
			FileConfiguration newKeysConfig = new YamlConfiguration();
			for (String key : missingKeys) {
				newKeysConfig.set(key, defaultConfig.get(key));
			}

			writer.write(newKeysConfig.saveToString());
			writer.flush();

		} catch (IOException e) {
			plugin.getLogger().severe("Could not update configuration file: " + fileName);
			e.printStackTrace();
		}
	}
}