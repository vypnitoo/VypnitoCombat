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

	// --- PVP & REGION PROTECTION HANDLERS ---

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (!configManager.isSafeZoneEntryPreventionEnabled() || regionProvider == null) {
			return;
		}

		Player player = event.getPlayer();
		if (!combatManager.isInCombat(player)) {
			return;
		}

		Location from = event.getFrom();
		Location to = event.getTo();

		if (from.distance(to) < 0.1) {
			return;
		}

		if (regionProvider.isLocationSafe(to)) {
			if (!regionProvider.isLocationSafe(from)) {
				event.setCancelled(true);
				player.sendMessage(messageManager.getMessage("cannot_enter_safe_zone"));

				// Calculate the bounce-back direction
				Vector direction = from.toVector().subtract(to.toVector()).normalize();
				direction.setY(0.4); // Add a slight upward boost
				direction.multiply(configManager.getRegionPushbackStrength());

				// Set velocity in a delayed task to prevent it from being overridden
				plugin.getServer().getScheduler().runTask(plugin, () -> player.setVelocity(direction));
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
			// Master switch for direct PvP damage
			if (!configManager.isGlobalPvpEnabled()) {
				event.setCancelled(true);
				event.getDamager().sendMessage(messageManager.getMessage("pvp_is_disabled"));
				return;
			}

			Player damaged = (Player) event.getEntity();
			Player damager = (Player) event.getDamager();

			// WorldGuard region check for combat tagging
			if (configManager.isWorldGuardIntegrationEnabled() && regionProvider != null) {
				if (!regionProvider.isCombatAllowed(damaged.getLocation()) || !regionProvider.isCombatAllowed(damager.getLocation())) {
					return;
				}
			}
			combatManager.enterCombat(damaged);
			combatManager.enterCombat(damager);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (configManager.isGlobalPvpEnabled() || !configManager.isLavaPlacementPreventionEnabled() || event.getBlock().getType() != Material.LAVA) {
			return;
		}

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
		if (configManager.isGlobalPvpEnabled() || !configManager.isHarmfulPotionPreventionEnabled()) {
			return;
		}

		Projectile potion = event.getPotion();
		if (!(potion.getShooter() instanceof Player)) {
			return;
		}

		Player shooter = (Player) potion.getShooter();
		boolean isHarmful = false;

		for (PotionEffect effect : event.getPotion().getEffects()) {
			if (configManager.getHarmfulPotionEffects().contains(effect.getType())) {
				isHarmful = true;
				break;
			}
		}

		if (!isHarmful) return;

		boolean messageSent = false;
		for (LivingEntity entity : event.getAffectedEntities()) {
			if (entity instanceof Player && !entity.getUniqueId().equals(shooter.getUniqueId())) {
				event.setIntensity(entity, 0); // Nullify the effect for this player
				if (!messageSent) {
					shooter.sendMessage(messageManager.getMessage("pvp_potion_blocked"));
					messageSent = true;
				}
			}
		}
	}

	// --- IN-COMBAT RESTRICTION HANDLERS ---

	@EventHandler
	public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
		Player player = event.getPlayer();
		if (player.hasPermission("combatlogv.bypass.combat") || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
			return;
		}

		if (event.isFlying()) {
			PlayerInventory inventory = player.getInventory();
			ItemStack chestplate = inventory.getChestplate();
			if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
				if (combatManager.isInCombat(player)) {
					event.setCancelled(true);
					player.sendMessage(messageManager.getMessage("combat_block_elytra"));
					return;
				}
				if (combatManager.isElytraOnCooldown(player)) {
					event.setCancelled(true);
					long remaining = combatManager.getElytraCooldownRemainingSeconds(player);
					player.sendMessage(messageManager.getMessage("elytra_cooldown_active").replace("%time%", String.valueOf(remaining)));
				}
			}
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (player.hasPermission("combatlogv.bypass.combat") || !combatManager.isInCombat(player)) return;

		ItemStack item = event.getItem();
		if (item == null) return;

		if (item.getType() == Material.ENDER_PEARL) {
			event.setCancelled(true);
			player.sendMessage(messageManager.getMessage("ender_pearl_combat_cooldown_active"));
			return;
		}

		if (item.getType() == Material.TRIDENT && item.hasItemMeta() && item.getItemMeta().hasEnchant(Enchantment.RIPTIDE)) {
			if (configManager.isRiptideBlockedInCombat() && (player.isInWaterOrRain())) {
				event.setCancelled(true);
				player.sendMessage(messageManager.getMessage("combat_block_riptide"));
				return;
			}
		}

		if (configManager.isItemBlocked(item.getType().name())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (player.hasPermission("combatlogv.bypass.combat") || !combatManager.isInCombat(player)) return;

		String command = event.getMessage().split(" ")[0].substring(1);
		if (configManager.isCommandBlocked(command)) {
			event.setCancelled(true);
			player.sendMessage(messageManager.getMessage("combat_block_command"));
		}
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (!configManager.shouldEndCombatOnDeath()) return;

		Player killed = event.getEntity();
		if (combatManager.isInCombat(killed)) {
			combatManager.exitCombat(killed, CombatExitReason.DEATH);
		}

		LivingEntity killer = killed.getKiller();
		if (killer instanceof Player) {
			Player playerKiller = (Player) killer;
			if (combatManager.isInCombat(playerKiller)) {
				combatManager.exitCombat(playerKiller, CombatExitReason.DEATH);
			}
		}
	}
}