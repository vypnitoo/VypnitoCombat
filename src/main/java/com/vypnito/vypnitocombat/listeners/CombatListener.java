package com.vypnito.vypnitocombat.listeners;

import com.vypnito.vypnitocombat.CombatExitReason;
import com.vypnito.vypnitocombat.VypnitoCombat;
import com.vypnito.vypnitocombat.integrations.RegionProvider;
import com.vypnito.vypnitocombat.managers.CombatManager;
import com.vypnito.vypnitocombat.managers.ConfigManager;
import com.vypnito.vypnitocombat.managers.MessageManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

public class CombatListener implements Listener {

	private final VypnitoCombat plugin;
	private final CombatManager combatManager;
	private final ConfigManager configManager;
	private final MessageManager messageManager;
	private final RegionProvider regionProvider;

	public CombatListener(VypnitoCombat plugin) {
		this.plugin = plugin;
		this.combatManager = plugin.getCombatManager();
		this.configManager = plugin.getConfigManager();
		this.messageManager = plugin.getMessageManager();
		this.regionProvider = plugin.getRegionProvider();
	}

	// --- THIS METHOD HAS BEEN RESTORED TO THE CORRECT, WORKING VERSION ---
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		// We only care about Player vs Player damage for combat tagging.
		if (!(event.getEntity() instanceof Player && event.getDamager() instanceof Player)) {
			return;
		}

		Player damager = (Player) event.getDamager();
		Player damaged = (Player) event.getEntity();


		// Check 1: Global PvP Toggle
		if (!configManager.isGlobalPvpEnabled()) {
			event.setCancelled(true);
			damager.sendMessage(messageManager.getMessage("pvp_is_disabled"));
			return;
		}

		// Check 2: WorldGuard region protection
		if (configManager.isWorldGuardIntegrationEnabled() && regionProvider != null) {
			// Check if the attacker has the bypass permission
			if (!damager.hasPermission("vypnitocombat.bypass.region")) {
				// If they don't have the bypass, then check the region flags
				if (!regionProvider.isCombatAllowed(damaged.getLocation()) || !regionProvider.isCombatAllowed(damager.getLocation())) {
					// Region flags prevent combat tagging, so we stop here.
					return;
				}
			}
		}

		// If all checks passed, put players in combat
		combatManager.enterCombat(damaged);
		combatManager.enterCombat(damager);
	}

	// This is the new, robust check for equipping an elytra
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;

		Player player = (Player) event.getWhoClicked();

		if (player.hasPermission("vypnitocombat.bypass.combat")) return;

		if (combatManager.isInCombat(player) || combatManager.isElytraOnCooldown(player)) {
			boolean isEquippingElytra = false;

			// Case 1: Shift-clicking an elytra from main inventory
			if (event.getClick().isShiftClick()) {
				if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ELYTRA) {
					isEquippingElytra = true;
				}
			}
			// Case 2: Using number key to swap from hotbar
			else if (event.getClick() == ClickType.NUMBER_KEY) {
				ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
				if (hotbarItem != null && hotbarItem.getType() == Material.ELYTRA && event.getSlotType() == InventoryType.SlotType.ARMOR) {
					isEquippingElytra = true;
				}
			}
			// Case 3: Placing an elytra with the cursor
			else if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 38) { // 38 is chestplate slot
				if (event.getCursor() != null && event.getCursor().getType() == Material.ELYTRA) {
					isEquippingElytra = true;
				}
			}

			if (isEquippingElytra) {
				event.setCancelled(true);
				player.sendMessage(messageManager.getMessage("cannot_equip_elytra"));
			}
		}
	}

	// ... All other listeners from the previous full version ...
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) { if (!configManager.isSafeZoneEntryPreventionEnabled() || regionProvider == null) { return; } Player player = event.getPlayer(); if (!combatManager.isInCombat(player)) { return; } Location from = event.getFrom(); Location to = event.getTo(); if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ() && from.getBlockY() == to.getBlockY()) { return; } if (regionProvider.isLocationSafe(to)) { if (!regionProvider.isLocationSafe(from)) { event.setCancelled(true); player.sendMessage(messageManager.getMessage("cannot_enter_safe_zone")); Vector direction = from.toVector().subtract(to.toVector()).normalize(); direction.setY(0.4); direction.multiply(configManager.getRegionPushbackStrength()); plugin.getServer().getScheduler().runTask(plugin, () -> player.setVelocity(direction)); } } }
	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) { if (configManager.isGlobalPvpEnabled() || !configManager.isLavaPlacementPreventionEnabled() || event.getBlock().getType() != Material.LAVA) { return; } Player placer = event.getPlayer(); double radius = configManager.getLavaCheckRadius(); for (Entity entity : placer.getNearbyEntities(radius, radius, radius)) { if (entity instanceof Player && !entity.getUniqueId().equals(placer.getUniqueId())) { event.setCancelled(true); placer.sendMessage(messageManager.getMessage("pvp_lava_blocked")); return; } } }
	@EventHandler(ignoreCancelled = true)
	public void onPotionSplash(PotionSplashEvent event) { if (configManager.isGlobalPvpEnabled() || !configManager.isHarmfulPotionPreventionEnabled()) { return; } Projectile potion = event.getPotion(); if (!(potion.getShooter() instanceof Player)) { return; } Player shooter = (Player) potion.getShooter(); boolean isHarmful = false; for (PotionEffect effect : event.getPotion().getEffects()) { if (configManager.getHarmfulPotionEffects().contains(effect.getType())) { isHarmful = true; break; } } if (!isHarmful) return; boolean messageSent = false; for (LivingEntity entity : event.getAffectedEntities()) { if (entity instanceof Player && !entity.getUniqueId().equals(shooter.getUniqueId())) { event.setIntensity(entity, 0); if (!messageSent) { shooter.sendMessage(messageManager.getMessage("pvp_potion_blocked")); messageSent = true; } } } }
	@EventHandler
	public void onPlayerToggleFlight(PlayerToggleFlightEvent event) { Player player = event.getPlayer(); if (player.hasPermission("vypnitocombat.bypass.combat") || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) { return; } if (event.isFlying()) { PlayerInventory inventory = player.getInventory(); ItemStack chestplate = inventory.getChestplate(); if (chestplate != null && chestplate.getType() == Material.ELYTRA) { if (combatManager.isInCombat(player)) { event.setCancelled(true); player.sendMessage(messageManager.getMessage("combat_block_elytra")); return; } if (combatManager.isElytraOnCooldown(player)) { event.setCancelled(true); long remaining = combatManager.getElytraCooldownRemainingSeconds(player); player.sendMessage(messageManager.getMessage("elytra_cooldown_active").replace("%time%", String.valueOf(remaining))); } } } }
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) { Player player = event.getPlayer(); if (player.hasPermission("vypnitocombat.bypass.combat")) return; if (event.getItem() != null && event.getItem().getType() == Material.FIREWORK_ROCKET) { PlayerInventory inventory = player.getInventory(); ItemStack chestplate = inventory.getChestplate(); if (chestplate != null && chestplate.getType() == Material.ELYTRA && combatManager.isElytraOnCooldown(player)) { event.setCancelled(true); long remaining = combatManager.getElytraCooldownRemainingSeconds(player); player.sendMessage(messageManager.getMessage("elytra_cooldown_active").replace("%time%", String.valueOf(remaining))); return; } } if (combatManager.isInCombat(player)) { ItemStack item = event.getItem(); if (item == null) return; if (item.getType() == Material.ENDER_PEARL) { event.setCancelled(true); player.sendMessage(messageManager.getMessage("ender_pearl_combat_cooldown_active")); return; } if (item.getType() == Material.TRIDENT && item.hasItemMeta() && item.getItemMeta().hasEnchant(Enchantment.RIPTIDE)) { if (configManager.isRiptideBlockedInCombat() && (player.isInWaterOrRain())) { event.setCancelled(true); player.sendMessage(messageManager.getMessage("combat_block_riptide")); return; } } if (item.getType() == Material.FIREWORK_ROCKET && player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType() == Material.ELYTRA) { event.setCancelled(true); player.sendMessage(messageManager.getMessage("combat_block_firework_rocket")); return; } if (configManager.isItemBlocked(item.getType().name())) { event.setCancelled(true); } } }
	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) { Player player = event.getPlayer(); if (player.hasPermission("vypnitocombat.bypass.combat") || !combatManager.isInCombat(player)) return; String command = event.getMessage().split(" ")[0].substring(1); if (configManager.isCommandBlocked(command)) { event.setCancelled(true); player.sendMessage(messageManager.getMessage("combat_block_command")); } }
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) { Player killed = event.getEntity(); if (configManager.useCustomCombatDeathMessage() && combatManager.isInCombat(killed)) { String killerName = "the environment"; if (killed.getKiller() != null) { killerName = killed.getKiller().getName(); } String deathMessage = messageManager.getMessage("custom_combat_death_message") .replace("%victim%", killed.getName()) .replace("%killer%", killerName); event.setDeathMessage(deathMessage); } if (!configManager.shouldEndCombatOnDeath()) return; combatManager.exitCombat(killed, CombatExitReason.DEATH); LivingEntity killer = killed.getKiller(); if (killer instanceof Player) { Player playerKiller = (Player) killer; if (combatManager.isInCombat(playerKiller)) { combatManager.exitCombat(playerKiller, CombatExitReason.DEATH); } } }
}