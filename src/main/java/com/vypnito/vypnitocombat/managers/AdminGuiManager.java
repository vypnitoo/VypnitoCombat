package com.vypnito.vypnitocombat.managers;

import com.vypnito.vypnitocombat.VypnitoCombat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminGuiManager {

	private final VypnitoCombat plugin;
	private final String guiTitle;

	public AdminGuiManager(VypnitoCombat plugin) {
		this.plugin = plugin;
		this.guiTitle = plugin.getMessageManager().getRawMessage("admin_gui_title", "&4VypnitoCombat Admin");
	}

	public String getGuiTitle() {
		return guiTitle;
	}

	public void openAdminGui(Player player) {
		Inventory gui = Bukkit.createInventory(null, 27, guiTitle); // 27 slots = 3 rows

		// --- Create items for each setting ---

		// Global PvP Toggle
		gui.setItem(10, createSettingItem(
				Material.DIAMOND_SWORD,
				"&bGlobal PvP",
				"global_pvp_enabled",
				"&7Toggles PvP across the server."
		));

		// Action Bar Timer Toggle
		gui.setItem(12, createSettingItem(
				Material.COMPASS,
				"&bAction Bar Timer",
				"action_bar_timer",
				"&7Shows a countdown in the action bar."
		));

		// CombatLog Kill Punishment Toggle
		gui.setItem(14, createSettingItem(
				Material.LEATHER_BOOTS, // Represents "kicking" the player
				"&bKill on CombatLog",
				"punishments.kill_player",
				"&7Instantly kills a player if they log out."
		));

		// A placeholder for a general combat restriction setting
		gui.setItem(16, createSettingItem(
				Material.ELYTRA,
				"&bBlock Riptide",
				"combat_restrictions.block_riptide",
				"&7Blocks usage of Riptide tridents."
		));

		// Fill empty slots with placeholder glass panes
		ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemMeta meta = placeholder.getItemMeta();
		meta.setDisplayName(" ");
		placeholder.setItemMeta(meta);
		for (int i = 0; i < gui.getSize(); i++) {
			if (gui.getItem(i) == null) {
				gui.setItem(i, placeholder);
			}
		}

		player.openInventory(gui);
	}

	private ItemStack createSettingItem(Material material, String name, String configPath, String description) {
		boolean isEnabled = plugin.getConfig().getBoolean(configPath);

		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();

		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.translateAlternateColorCodes('&', description));
		lore.add(" ");
		if (isEnabled) {
			lore.add(ChatColor.translateAlternateColorCodes('&', "&a&lENABLED"));
			meta.addEnchant(Enchantment.UNBREAKING, 1, true);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		} else {
			lore.add(ChatColor.translateAlternateColorCodes('&', "&c&lDISABLED"));
		}

		meta.setLore(lore);
		item.setItemMeta(meta);

		return item;
	}
}