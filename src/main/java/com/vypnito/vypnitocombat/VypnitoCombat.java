package com.vypnito.vypnitocombat;

import com.vypnito.vypnitocombat.expansions.VypnitoCombatPlaceholders;
import com.vypnito.vypnitocombat.integrations.RegionProvider;
import com.vypnito.vypnitocombat.integrations.WorldGuardBootstrapper;
import com.vypnito.vypnitocombat.listeners.CombatListener;
import com.vypnito.vypnitocombat.listeners.PlayerQuitListener;
import com.vypnito.vypnitocombat.managers.CombatManager;
import com.vypnito.vypnitocombat.managers.ConfigManager;
import com.vypnito.vypnitocombat.managers.MessageManager;
import com.vypnito.vypnitocombat.utils.ConfigUpdater; // Import the new utility class
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class VypnitoCombat extends JavaPlugin {

	private CombatManager combatManager;
	private ConfigManager configManager;
	private MessageManager messageManager;
	private RegionProvider regionProvider;

	private BukkitTask elytraMonitorTask;
	private BukkitTask actionBarMonitorTask;

	@Override
	public void onLoad() {
		if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
			WorldGuardBootstrapper.registerFlag(this);
		}
	}

	@Override
	public void onEnable() {
		getLogger().info("====================================");
		getLogger().info("===       VypnitoCombat          ===");
		getLogger().info("===       MADE BY VYPNITO          ===");
		getLogger().info("====================================");

		// Update and load configurations
		ConfigUpdater.update(this, "config.yml");
		ConfigUpdater.update(this, "messages.yml");
		this.reloadConfig(); // Load the potentially updated config into memory

		// Initialize Managers
		this.configManager = new ConfigManager(this.getConfig());
		this.messageManager = new MessageManager(this);
		this.combatManager = new CombatManager(this);

		// Initialize Integrations
		setupIntegrations();

		// Register Listeners & Commands
		getServer().getPluginManager().registerEvents(new CombatListener(this), this);
		getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
		getCommand("vypnitocombat").setExecutor(new VypnitoCombatCommand(this));
		getCommand("pvp").setExecutor(new PvPCommand(this));

		// Start Scheduled Tasks
		this.elytraMonitorTask = new ElytraFlightMonitor(this).runTaskTimer(this, 0L, 5L);
		manageActionBarTask();

		getLogger().info(messageManager.getRawMessage("plugin_enabled", "&aVypnitoCombat has been enabled!"));
	}

	@Override
	public void onDisable() {
		if (this.elytraMonitorTask != null) this.elytraMonitorTask.cancel();
		if (this.actionBarMonitorTask != null) this.actionBarMonitorTask.cancel();
		getLogger().info(messageManager.getRawMessage("plugin_disabled", "&cVypnitoCombat has been disabled!"));
	}

	/**
	 * Reloads all plugin configurations and restarts necessary tasks.
	 */
	public void reloadPlugin() {
		// First, update the files with any new keys
		ConfigUpdater.update(this, "config.yml");
		ConfigUpdater.update(this, "messages.yml");

		// Now, reload the values from the files
		this.reloadConfig();
		this.configManager = new ConfigManager(this.getConfig());
		this.messageManager.reloadMessages();

		// Restart tasks that depend on the config
		manageActionBarTask();

		getLogger().info("VypnitoCombat config and messages reloaded.");
	}

	/**
	 * Sets up hooks for optional dependencies like WorldGuard and PlaceholderAPI.
	 */
	private void setupIntegrations() {
		if (configManager.isWorldGuardIntegrationEnabled() && Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
			try {
				this.regionProvider = WorldGuardBootstrapper.initialize(this);
			} catch (Throwable t) {
				getLogger().warning("An error occurred while initializing the WorldGuard hook. Region features disabled.");
				t.printStackTrace();
			}
		} else {
			getLogger().info("WorldGuard not found or is disabled in config. Skipping hook.");
		}

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new VypnitoCombatPlaceholders(this).register();
		}
	}

	/**
	 * Manages the ActionBar display task based on the current config settings.
	 */
	private void manageActionBarTask() {
		if (this.actionBarMonitorTask != null) this.actionBarMonitorTask.cancel();
		if (configManager.isActionBarTimerEnabled()) {
			this.actionBarMonitorTask = new ActionBarMonitor(this).runTaskTimer(this, 0L, 2L);
		}
	}

	// --- Getters ---
	public CombatManager getCombatManager() {
		return combatManager;
	}

	public ConfigManager getConfigManager() {
		return configManager;
	}

	public MessageManager getMessageManager() {
		return messageManager;
	}

	public RegionProvider getRegionProvider() {
		return regionProvider;
	}
}