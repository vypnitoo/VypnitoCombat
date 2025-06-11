package com.vypnito.vypnitocombat;

import com.vypnito.vypnitocombat.expansions.VypnitoCombatPlaceholders;
import com.vypnito.vypnitocombat.integrations.RegionProvider;
import com.vypnito.vypnitocombat.integrations.WorldGuardBootstrapper;
import com.vypnito.vypnitocombat.listeners.CombatListener;
import com.vypnito.vypnitocombat.listeners.JoinListener;
import com.vypnito.vypnitocombat.listeners.PlayerQuitListener;
import com.vypnito.vypnitocombat.managers.CombatManager;
import com.vypnito.vypnitocombat.managers.ConfigManager;
import com.vypnito.vypnitocombat.managers.MessageManager;
import com.vypnito.vypnitocombat.utils.ConfigUpdater;
import com.vypnito.vypnitocombat.utils.UpdateChecker;
import org.bukkit.Bukkit; // Make sure Bukkit is imported
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class VypnitoCombat extends JavaPlugin {

	private CombatManager combatManager;
	private ConfigManager configManager;
	private MessageManager messageManager;
	private RegionProvider regionProvider;

	private BukkitTask elytraMonitorTask;
	private BukkitTask actionBarMonitorTask;
	private BukkitTask borderVisualizerTask;

	private boolean updateAvailable = false;
	private String latestVersion = "";

	@Override
	public void onLoad() {
		if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
			WorldGuardBootstrapper.registerFlag(this);
		}
	}

	@Override
	public void onEnable() {
		// Use Bukkit.getConsoleSender().sendMessage() for colored console output
		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "===================================="));
		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "===       &aVypnitoCombat&r          ==="));
		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "===       MADE BY VYPNITO        ==="));
		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "===================================="));

		ConfigUpdater.update(this, "config.yml");
		ConfigUpdater.update(this, "messages.yml");
		this.reloadConfig();

		this.configManager = new ConfigManager(this.getConfig());
		this.messageManager = new MessageManager(this);
		this.combatManager = new CombatManager(this);

		setupIntegrations();

		getServer().getPluginManager().registerEvents(new CombatListener(this), this);
		getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
		getServer().getPluginManager().registerEvents(new JoinListener(this), this);

		getCommand("vypnitocombat").setExecutor(new VypnitoCombatCommand(this));
		getCommand("pvp").setExecutor(new PvPCommand(this));

		this.elytraMonitorTask = new ElytraFlightMonitor(this).runTaskTimer(this, 0L, 20L);
		manageActionBarTask();
		manageBorderVisualizerTask();

		if (getConfigManager().isUpdateCheckerEnabled()) {
			new UpdateChecker(this).check();
		}

		// These lines are likely already correctly handled by MessageManager
		getLogger().info(messageManager.getRawMessage("plugin_enabled", "&aVypnitoCombat has been enabled!"));
	}

	@Override
	public void onDisable() {
		if (this.elytraMonitorTask != null) this.elytraMonitorTask.cancel();
		if (this.actionBarMonitorTask != null) this.actionBarMonitorTask.cancel();
		if (this.borderVisualizerTask != null) this.borderVisualizerTask.cancel();

		getLogger().info(messageManager.getRawMessage("plugin_disabled", "&cVypnitoCombat has been disabled!"));
	}

	public void reloadPlugin() {
		ConfigUpdater.update(this, "config.yml");
		ConfigUpdater.update(this, "messages.yml");
		this.reloadConfig();
		this.configManager = new ConfigManager(this.getConfig());
		this.messageManager.reloadMessages();
		manageActionBarTask();
		manageBorderVisualizerTask();
		// Also use Bukkit.getConsoleSender() for this reload message if it's intended to be colored
		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&aVypnitoCombat config and messages reloaded."));
	}

	private void setupIntegrations() {
		if (configManager.isWorldGuardIntegrationEnabled() && Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
			try {
				this.regionProvider = WorldGuardBootstrapper.initialize(this);
			} catch (Throwable t) {
				// Use Bukkit.getConsoleSender() for warnings that need colors
				Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&eAn error occurred while initializing the WorldGuard hook. Region features disabled."));
			}
		}
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new VypnitoCombatPlaceholders(this).register();
		}
	}

	private void manageActionBarTask() {
		if (this.actionBarMonitorTask != null) this.actionBarMonitorTask.cancel();
		if (configManager.isActionBarTimerEnabled()) {
			this.actionBarMonitorTask = new ActionBarMonitor(this).runTaskTimer(this, 0L, 2L);
		}
	}

	private void manageBorderVisualizerTask() {
		if (this.borderVisualizerTask != null) {
			this.borderVisualizerTask.cancel();
		}
		if (configManager.isSafeZoneVisualized()) {
			this.borderVisualizerTask = new BorderVisualizerTask(this).runTaskTimer(this, 0L, configManager.getVisualizerCheckInterval());
		}
	}

	public CombatManager getCombatManager() { return combatManager; }
	public ConfigManager getConfigManager() { return configManager; }
	public MessageManager getMessageManager() { return messageManager; }
	public RegionProvider getRegionProvider() { return regionProvider; }
	public boolean isUpdateAvailable() { return this.updateAvailable; }
	public String getLatestVersion() { return this.latestVersion; }
	public void setUpdateAvailable(boolean available, String version) { this.updateAvailable = available; this.latestVersion = version; }
}