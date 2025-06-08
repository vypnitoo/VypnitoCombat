package com.vypnito.vypnitocombat.integrations;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.vypnito.vypnitocombat.VypnitoCombat;

public class WorldGuardBootstrapper {

	/**
	 * This method is called during the onLoad phase. It contains all direct
	 * WorldGuard API calls for registering the custom flag.
	 * @param plugin The main plugin instance, used for logging.
	 */
	public static void registerFlag(VypnitoCombat plugin) {
		try {
			FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
			StateFlag flag = new StateFlag("combatlog-enabled", true);
			registry.register(flag);
			plugin.getLogger().info("Successfully registered WorldGuard flag: combatlog-enabled");
		} catch (FlagConflictException e) {
			// This is okay, it means the flag is already registered.
			plugin.getLogger().info("WorldGuard flag 'combatlog-enabled' was already registered.");
		} catch (Throwable e) {
			plugin.getLogger().severe("A critical error occurred while registering the WorldGuard flag.");
			e.printStackTrace();
		}
	}

	/**
	 * This method is called during the onEnable phase to safely initialize our hook.
	 * @param plugin The main plugin instance.
	 * @return A RegionProvider instance if successful, otherwise null.
	 */
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