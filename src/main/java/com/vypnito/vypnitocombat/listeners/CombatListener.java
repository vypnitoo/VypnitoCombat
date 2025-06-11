package com.vypnito.vypnitocombat.listeners;

import com.vypnito.vypnitocombat.CombatExitReason;
import com.vypnito.vypnitocombat.RegionEntryPrevention;
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

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		RegionEntryPrevention method = configManager.getRegionEntryPreventionMethod();
		if (method == RegionEntryPrevention.NONE || regionProvider == null) {
			return;
		}

		Player player = event.getPlayer();
		if (!combatManager.isInCombat(player)) {
			return;
		}

		Location from = event.getFrom();
		Location to = event.getTo();
		if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ() && from.getBlockY() == to.getBlockY()) {
			return;
		}

		if (regionProvider.isLocationSafe(to) && !regionProvider.isLocationSafe(from)) {
			player.sendMessage(messageManager.getMessage("cannot_enter_safe_zone"));

			if (method == RegionEntryPrevention.PUSHBACK) {
				event.setCancelled(true);
				Vector direction = from.toVector().subtract(to.toVector()).normalize();
				direction.setY(0.4);
				direction.multiply(configManager.getRegionPushbackStrength());
				plugin.getServer().getScheduler().runTask(plugin, () -> player.setVelocity(direction));
			} else if (method == RegionEntryPrevention.VISUAL_BORDER) {
				// This acts as a final backstop to the visualizer task, preventing glitching.
				event.setTo(from);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player && event.getDamager() instanceof Player)) return;

		Player damager = (Player) event.getDamager();
		Player damaged = (Player) event.getEntity();

		if (!configManager.isGlobalPvpEnabled()) {
			event.setCancelled(true);
			damager.sendMessage(messageManager.getMessage("pvp_is_disabled"));
			return;
		}

		if (configManager.isWorldGuardIntegrationEnabled() && regionProvider != null) {
			if (!damager.hasPermission("vypnitocombat.bypass.region")) {
				if (!regionProvider.isCombatAllowed(damaged.getLocation()) || !regionProvider.isCombatAllowed(damager.getLocation())) {
					return;
				}
			}
		}
		combatManager.enterCombat(damaged);
		combatManager.enterCombat(damager);
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player killed = event.getEntity();
		combatManager.clearVisualBorder(killed);

		if (configManager.useCustomCombatDeathMessage() && combatManager.isInCombat(killed)) {
			String killerName = killed.getKiller() != null ? killed.getKiller().getName() : "the environment";
			event.setDeathMessage(messageManager.getMessage("custom_combat_death_message").replace("%victim%", killed.getName()).replace("%killer%", killerName));
		}

		if (!configManager.shouldEndCombatOnDeath()) return;
		combatManager.exitCombat(killed, CombatExitReason.DEATH);

		if (killed.getKiller() instanceof Player) {
			Player playerKiller = killed.getKiller();
			if (combatManager.isInCombat(playerKiller)) {
				combatManager.exitCombat(playerKiller, CombatExitReason.DEATH);
			}
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
		Player player = (Player) event.getWhoClicked();
		if (player.hasPermission("vypnitocombat.bypass.combat")) return;
		if (combatManager.isInCombat(player) || combatManager.isElytraOnCooldown(player)) {
			boolean isEquippingElytra = false;
			if (event.getClick().isShiftClick() && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ELYTRA) {
				isEquippingElytra = true;
			} else if (event.getClick() == ClickType.NUMBER_KEY && player.getInventory().getItem(event.getHotbarButton()) != null && player.getInventory().getItem(event.getHotbarButton()).getType() == Material.ELYTRA && event.getSlotType() == InventoryType.SlotType.ARMOR) {
				isEquippingElytra = true;
			} else if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 38) { // Slot 38 is chestplate
				if (event.getCursor() != null && event.getCursor().getType() == Material.ELYTRA) isEquippingElytra = true;
			}
			if (isEquippingElytra) {
				event.setCancelled(true);
				player.sendMessage(messageManager.getMessage("cannot_equip_elytra"));
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (configManager.isGlobalPvpEnabled() || !configManager.isLavaPlacementPreventionEnabled() || event.getBlock().getType() != Material.LAVA) return;
		Player placer = event.getPlayer();
		double radius = configManager.getLavaCheckRadius();
		for (Entity entity : placer.getNearbyEntities(radius, radius, radius)) {
			if (entity instanceof Player && !entity.getUniqueId().equals(placer.getUniqueId())) {
				event.setCancelled(true);
				placer.sendMessage(messageManager.getMessage("pvp_lava_blocked"));
				return;
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPotionSplash(PotionSplashEvent event) {
		if (configManager.isGlobalPvpEnabled() || !configManager.isHarmfulPotionPreventionEnabled() || !(event.getPotion().getShooter() instanceof Player)) return;
		Player shooter = (Player) event.getPotion().getShooter();
		boolean isHarmful = event.getPotion().getEffects().stream().anyMatch(effect -> configManager.getHarmfulPotionEffects().contains(effect.getType()));
		if (!isHarmful) return;
		boolean messageSent = false;
		for (LivingEntity entity : event.getAffectedEntities()) {
			if (entity instanceof Player && !entity.getUniqueId().equals(shooter.getUniqueId())) {
				event.setIntensity(entity, 0);
				if (!messageSent) {
					shooter.sendMessage(messageManager.getMessage("pvp_potion_blocked"));
					messageSent = true;
				}
			}
		}
	}

	@EventHandler
	public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
		Player player = event.getPlayer();
		if (player.hasPermission("vypnitocombat.bypass.combat") || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
		PlayerInventory inventory = player.getInventory();
		ItemStack chestplate = inventory.getChestplate();
		if (event.isFlying() && chestplate != null && chestplate.getType() == Material.ELYTRA) {
			if (combatManager.isInCombat(player)) {
				event.setCancelled(true);
				player.sendMessage(messageManager.getMessage("combat_block_elytra"));
			} else if (combatManager.isElytraOnCooldown(player)) {
				event.setCancelled(true);
				player.sendMessage(messageManager.getMessage("elytra_cooldown_active").replace("%time%", String.valueOf(combatManager.getElytraCooldownRemainingSeconds(player))));
			}
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (player.hasPermission("vypnitocombat.bypass.combat")) return;
		ItemStack item = event.getItem();
		if (item == null) return;

		if (item.getType() == Material.FIREWORK_ROCKET && player.isGliding() && combatManager.isElytraOnCooldown(player)) {
			event.setCancelled(true);
			player.sendMessage(messageManager.getMessage("elytra_cooldown_active").replace("%time%", String.valueOf(combatManager.getElytraCooldownRemainingSeconds(player))));
			return;
		}
		if (combatManager.isInCombat(player)) {
			if (item.getType() == Material.ENDER_PEARL) {
				event.setCancelled(true);
				player.sendMessage(messageManager.getMessage("ender_pearl_combat_cooldown_active"));
			} else if (item.getType() == Material.TRIDENT && item.hasItemMeta() && item.getEnchantmentLevel(Enchantment.RIPTIDE) > 0 && player.isInWaterOrRain()) {
				event.setCancelled(true);
				player.sendMessage(messageManager.getMessage("combat_block_riptide"));
			} else if (item.getType() == Material.FIREWORK_ROCKET && player.isGliding()) {
				event.setCancelled(true);
				player.sendMessage(messageManager.getMessage("combat_block_firework_rocket"));
			}
		}
	}

	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (player.hasPermission("vypnitocombat.bypass.combat") || !combatManager.isInCombat(player)) return;
		String command = event.getMessage().split(" ")[0].substring(1).toLowerCase();
		if (configManager.isCommandBlocked(command)) {
			event.setCancelled(true);
			player.sendMessage(messageManager.getMessage("combat_block_command"));
		}
	}
}