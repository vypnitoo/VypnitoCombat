package com.vypnito.vypnitocombat.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;

public class WorldGuardHook implements RegionProvider {

	private final StateFlag combatLogEnabledFlag;

	public WorldGuardHook() {
		// The flag is now registered by the bootstrapper. We just retrieve it here.
		FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		this.combatLogEnabledFlag = (StateFlag) registry.get("combatlog-enabled");

		// This safety check ensures the integration doesn't proceed if the flag is missing for any reason.
		if (this.combatLogEnabledFlag == null) {
			throw new IllegalStateException("The 'combatlog-enabled' WorldGuard flag could not be found. It may not have been registered correctly on load.");
		}
	}

	@Override
	public boolean isCombatAllowed(Location location) {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));
		if (regions == null) {
			return true;
		}
		return regions.getApplicableRegions(BukkitAdapter.asBlockVector(location)).testState(null, combatLogEnabledFlag);
	}

	@Override
	public boolean isLocationSafe(Location location) {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));
		if (regions == null) {
			return false;
		}

		for (ProtectedRegion region : regions.getApplicableRegions(BukkitAdapter.asBlockVector(location))) {
			if (region.getFlag(Flags.PVP) == State.DENY) {
				return true;
			}
			if (region.getFlag(Flags.INVINCIBILITY) == State.ALLOW) {
				return true;
			}
		}
		return false;
	}
}