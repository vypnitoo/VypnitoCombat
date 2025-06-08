package com.vypnito.vypnitocombat;

import com.vypnito.vypnitocombat.expansions.VypnitoCombatPlaceholders;
import com.vypnito.vypnitocombat.integrations.RegionProvider;
import com.vypnito.vypnitocombat.integrations.WorldGuardBootstrapper;
import com.vypnito.vypnitocombat.listeners.CombatListener;
import com.vypnito.vypnitocombat.listeners.PlayerQuitListener;
import com.vypnito.vypnitocombat.managers.CombatManager;
import com.vypnito.vypnitocombat.managers.ConfigManager;
import com.vypnito.vypnitocombat.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class VypnitoCombat extends JavaPlugin {

	// DatabaseManager has been removed
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
		getLogger().info("===       MADE BY VYPNITO        ===");
		getLogger().info("====================================");

		// Managers
		saveDefaultConfig();
		configManager = new ConfigManager(getConfig());
		messageManager = new MessageManager(this);
		// CombatManager no longer needs the database
		combatManager = new CombatManager(this);

		// Integrations
		setupIntegrations();

		// Listeners & Commands
		getServer().getPluginManager().registerEvents(new CombatListener(this), this);
		getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
		getCommand("vypnitocombat").setExecutor(new VypnitoCombatCommand(this));
		getCommand("pvp").setExecutor(new PvPCommand(this));

		// Tasks
		elytraMonitorTask = new ElytraFlightMonitor(this).runTaskTimer(this, 0L, 5L);
		manageActionBarTask();

		getLogger().info(messageManager.getRawMessage("plugin_enabled", "&aVypnitoCombat has been enabled!"));
	}

	@Override
	public void onDisable() {
		if (elytraMonitorTask != null) elytraMonitorTask.cancel();
		if (actionBarMonitorTask != null) actionBarMonitorTask.cancel();
		// Disconnecting from the database is no longer needed
		getLogger().info(messageManager.getRawMessage("plugin_disabled", "&cVypnitoCombat has been disabled!"));
	}

	private void setupIntegrations() {
		if (configManager.isWorldGuardIntegrationEnabled() && Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
			try {
				regionProvider = WorldGuardBootstrapper.initialize(this);
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

	// ... all other methods (reloadPlugin, manageActionBarTask, getters) remain the same ...

	public void reloadPlugin() {
		reloadConfig();
		configManager = new ConfigManager(getConfig());
		messageManager.reloadMessages();
		manageActionBarTask();
		getLogger().info("VypnitoCombat config and messages reloaded.");
	}

	private void manageActionBarTask() {
		if (actionBarMonitorTask != null && !actionBarMonitorTask.isCancelled()) // only cancel if it's not already cancelled
			actionBarMonitorTask.cancel();
		if (configManager.isActionBarTimerEnabled()) {
			actionBarMonitorTask = new ActionBarMonitor(this).runTaskTimer(this, 0L, 2L);
		}
	}
	public CombatManager getCombatManager() { return combatManager; }
	public ConfigManager getConfigManager() { return configManager; }
	public MessageManager getMessageManager() { return messageManager; }
	public RegionProvider getRegionProvider() { return regionProvider; }
}