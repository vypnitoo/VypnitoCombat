package com.vypnito.vypnitocombat.integrations;

import org.bukkit.Location;

/**
 * An interface for checking region properties.
 * This allows for optional integration with region protection plugins like WorldGuard
 * without creating a hard dependency in the main listeners.
 */
public interface RegionProvider {

	/**
	 * Checks if combat tagging is allowed at a given location based on custom plugin flags.
	 * @param location The location to check.
	 * @return true if combat tagging is allowed, false otherwise.
	 */
	boolean isCombatAllowed(Location location);

	/**
	 * Checks if a location is considered a "safe zone" based on standard PvP flags.
	 * @param location The location to check.
	 * @return true if the location is a safe zone (e.g., pvp=deny), false otherwise.
	 */
	boolean isLocationSafe(Location location);
}