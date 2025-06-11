// In your ConfigManager.java file

package com.vypnito.vypnitocombat.managers;

import com.vypnito.vypnitocombat.RegionEntryPrevention;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigManager {

	private final FileConfiguration config;
	private Set<PotionEffectType> cachedHarmfulEffects;

	public ConfigManager(FileConfiguration config) {
		this.config = config;
		cacheHarmfulEffects();
	}

	private void cacheHarmfulEffects() {
		List<String> effectNames = config.getStringList("pvp_protections.harmful_potion_effects");
		this.cachedHarmfulEffects = effectNames.stream()
				.map(PotionEffectType::getByName)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());
	}

	// --- ADD THIS METHOD ---
	public boolean isSafeZoneVisualized() {
		// Returns true if the entry prevention method is set to VISUAL_BORDER
		return getRegionEntryPreventionMethod() == RegionEntryPrevention.VISUAL_BORDER;
	}

	// --- UPDATED REGION GETTERS ---
	public RegionEntryPrevention getRegionEntryPreventionMethod() {
		String method = config.getString("region_protections.entry_prevention_method", "PUSHBACK").toUpperCase(); // Changed default to PUSHBACK based on config
		try {
			return RegionEntryPrevention.valueOf(method);
		} catch (IllegalArgumentException e) {
			return RegionEntryPrevention.PUSHBACK; // Default to the safer option if config is invalid
		}
	}

	public double getRegionPushbackStrength() {
		return config.getDouble("region_protections.pushback_strength", 0.8);
	}

	public int getVisualizerCheckInterval() {
		return config.getInt("region_protections.visualization_check_interval_ticks", 10);
	}

	public int getVisualizerDisplayRadius() {
		return config.getInt("region_protections.visualization_display_radius", 15);
	}

	public Material getVisualizerWallMaterial() {
		try {
			return Material.valueOf(config.getString("region_protections.visualization_wall_material", "RED_STAINED_GLASS").toUpperCase());
		} catch (IllegalArgumentException e) {
			return Material.RED_STAINED_GLASS;
		}
	}

	// --- Other Getters remain the same ---
	public boolean useCustomCombatDeathMessage() { return config.getBoolean("death_messages.use_custom_when_in_combat", false); }
	public boolean isGlobalPvpEnabled() { return config.getBoolean("global_pvp_enabled", true); }
	public boolean isLavaPlacementPreventionEnabled() { return config.getBoolean("pvp_protections.prevent_lava_placement_near_players", true); }
	public int getLavaCheckRadius() { return config.getInt("pvp_protections.lava_check_radius", 3); }
	public boolean isHarmfulPotionPreventionEnabled() { return config.getBoolean("pvp_protections.prevent_harmful_splash_potions", true); }
	public Set<PotionEffectType> getHarmfulPotionEffects() { return this.cachedHarmfulEffects; }
	public int getCombatDurationSeconds() { return config.getInt("combat_duration_seconds", 10); }
	public boolean isActionBarTimerEnabled() { return config.getBoolean("action_bar_timer", true); }
	public boolean shouldEndCombatOnDeath() { return config.getBoolean("end_combat_on_death", true); }
	public boolean isRiptideBlockedInCombat() { return config.getBoolean("combat_restrictions.block_riptide", true); }
	public List<String> getBlockedItems() { return config.getStringList("combat_restrictions.blocked_items_in_combat"); }
	public boolean isItemBlocked(String itemName) { return getBlockedItems().contains(itemName.toUpperCase()); }
	public List<String> getBlockedCommands() { return config.getStringList("combat_restrictions.blocked_commands_in_combat"); }
	public boolean isCommandBlocked(String command) { return getBlockedCommands().contains(command.toLowerCase()); }
	public int getElytraCooldownSeconds() { return config.getInt("cooldowns_after_combat.elytra_seconds", 20); }
	public int getEnderPearlCombatCooldownSeconds() { return config.getInt("cooldowns_after_combat.ender_pearl_seconds", 15); }
	public boolean shouldPunishmentKillPlayer() { return config.getBoolean("punishments.kill_player", true); }
	public List<String> getPunishmentCommands() { return config.getStringList("punishments.execute_commands"); }
	public boolean isWorldGuardIntegrationEnabled() { return config.getBoolean("integrations.worldguard.enabled", true); }
	public boolean isUpdateCheckerEnabled() { return config.getBoolean("update_checker.enabled", true); }
	public int getSpigotResourceId() { return config.getInt("update_checker.spigot_resource_id", 0); }
}