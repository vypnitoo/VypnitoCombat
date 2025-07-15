package com.vypnito.vypnitocombat.listeners;

import com.vypnito.vypnitocombat.VypnitoCombat;
import com.vypnito.vypnitocombat.managers.AdminGuiManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class AdminGuiListener implements Listener {

	private final VypnitoCombat plugin;
	private final AdminGuiManager adminGuiManager;

	public AdminGuiListener(VypnitoCombat plugin, AdminGuiManager adminGuiManager) {
		this.plugin = plugin;
		this.adminGuiManager = adminGuiManager;
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!event.getView().getTitle().equals(adminGuiManager.getGuiTitle())) {
			return;
		}
		event.setCancelled(true);

		ItemStack clickedItem = event.getCurrentItem();
		if (clickedItem == null || clickedItem.getType() == Material.AIR) {
			return;
		}

		Player player = (Player) event.getWhoClicked();
		FileConfiguration config = plugin.getConfig();
		String configPath = null;
		switch (clickedItem.getType()) {
			case DIAMOND_SWORD:
				configPath = "global_pvp_enabled";
				break;
			case COMPASS:
				configPath = "action_bar_timer";
				break;
			case LEATHER_BOOTS:
				configPath = "punishments.kill_player";
				break;
			case ELYTRA:
				configPath = "combat_restrictions.block_riptide";
				break;
			default:
				return;
		}
		boolean currentValue = config.getBoolean(configPath);
		config.set(configPath, !currentValue);
		plugin.saveConfig();

		adminGuiManager.openAdminGui(player);
	}
}