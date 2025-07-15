package com.vypnito.vypnitocombat.integrations;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.vypnito.vypnitocombat.VypnitoCombat;

public class WorldGuardBootstrapper {
	public static void registerFlag(VypnitoCombat plugin) {
		try {
			FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
			StateFlag flag = new StateFlag("combatlog-enabled", true);
			registry.register(flag);
			plugin.getLogger().info("Successfully registered WorldGuard flag: combatlog-enabled");
		} catch (FlagConflictException e) {
			plugin.getLogger().info("WorldGuard flag 'combatlog-enabled' was already registered.");
		} catch (Throwable e) {
			plugin.getLogger().severe("A critical error occurred while registering the WorldGuard flag.");
			e.printStackTrace();
		}
	}
	public static RegionProvider initialize(VypnitoCombat plugin) {
		try {
			RegionProvider provider = new WorldGuardHook();
			plugin.getLogger().info("Successfully hooked into WorldGuard. Region protections are active.");
			return provider;
		} catch (Throwable e) {
			plugin.getLogger().warning("Failed to initialize WorldGuard hook. Is it compatible? Region features will be disabled.");
			e.printStackTrace();
			return null;
		}
	}
}