package com.vypnito.vypnitocombat.managers;

import com.vypnito.vypnitocombat.VypnitoCombat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class MessageManager {

	private final VypnitoCombat plugin;
	private FileConfiguration messagesConfig = null;
	private File messagesFile = null;
	private String cachedPrefix = null;

	public MessageManager(VypnitoCombat plugin) {
		this.plugin = plugin;
		saveDefaultMessages();
	}

	public void reloadMessages() {
		if (messagesFile == null) {
			messagesFile = new File(plugin.getDataFolder(), "messages.yml");
		}
		messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

		Reader defConfigStream = new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8);
		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			messagesConfig.setDefaults(defConfig);
		}
		cachedPrefix = messagesConfig.getString("message_prefix", "&7[&bCombatLogV&7] ");
	}

	public FileConfiguration getMessages() {
		if (messagesConfig == null) {
			reloadMessages();
		}
		return messagesConfig;
	}

	public String getMessage(String path) {
		String message = getMessages().getString(path, "&cMissing message: " + path);
		message = message.replace("%prefix%", cachedPrefix);
		return message.replace("&", "§");
	}

	public String getRawMessage(String path, String def) {
		String message = getMessages().getString(path, def);
		return message.replace("&", "§");
	}

	public void saveDefaultMessages() {
		if (messagesFile == null) {
			messagesFile = new File(plugin.getDataFolder(), "messages.yml");
		}
		if (!messagesFile.exists()) {
			plugin.saveResource("messages.yml", false);
		}
		reloadMessages();
	}
}