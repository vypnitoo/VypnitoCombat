package com.vypnito.vypnitocombat.integrations;

import org.bukkit.Location;
public interface RegionProvider {
	boolean isCombatAllowed(Location location);
	boolean isLocationSafe(Location location);
}