package com.vypnito.vypnitocombat;

import com.vypnito.vypnitocombat.integrations.RegionProvider;
import com.vypnito.vypnitocombat.managers.CombatManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BorderVisualizerTask extends BukkitRunnable {

	private final VypnitoCombat plugin;
	private final CombatManager combatManager;
	private final RegionProvider regionProvider;

	public BorderVisualizerTask(VypnitoCombat plugin) {
		this.plugin = plugin;
		this.combatManager = plugin.getCombatManager();
		this.regionProvider = plugin.getRegionProvider();
	}

	@Override
	public void run() {
		if (regionProvider == null || !plugin.getConfigManager().isSafeZoneVisualized()) {
			return;
		}

		for (UUID playerUUID : new HashSet<>(combatManager.getCombatTimers().keySet())) {
			Player player = plugin.getServer().getPlayer(playerUUID);
			if (player == null || !player.isOnline()) {
				combatManager.clearVisualBorder(player);
				continue;
			}

			if (player.hasPermission("vypnitocombat.bypass.combat")) {
				combatManager.clearVisualBorder(player);
				continue;
			}

			Set<Location> borderBlocksToShow = findNearbyBorderBlocks(player);
			updatePlayerView(player, borderBlocksToShow);
		}
	}

	private void updatePlayerView(Player player, Set<Location> newBlocks) {
		Set<Location> oldBlocks = combatManager.getVisibleBorderBlocks(player.getUniqueId());
		BlockData wallMaterial = plugin.getConfigManager().getVisualizerWallMaterial().createBlockData();

		// Revert blocks that are no longer part of the border view
		Set<Location> blocksToRevert = new HashSet<>(oldBlocks);
		blocksToRevert.removeAll(newBlocks);
		for (Location loc : blocksToRevert) {
			player.sendBlockChange(loc, loc.getBlock().getBlockData());
		}

		// Show new blocks that were not visible before
		Set<Location> blocksToShow = new HashSet<>(newBlocks);
		blocksToShow.removeAll(oldBlocks);
		for (Location loc : blocksToShow) {
			player.sendBlockChange(loc, wallMaterial);
		}

		combatManager.setVisibleBorderBlocks(player.getUniqueId(), newBlocks);
	}

	private Set<Location> findNearbyBorderBlocks(Player player) {
		Set<Location> borderBlocks = new HashSet<>();
		Location playerLoc = player.getLocation();
		int radius = plugin.getConfigManager().getVisualizerDisplayRadius();

		// If the player is not near any safe zone, no need to show a border
		if (!isNearSafeZone(playerLoc, radius)) {
			return borderBlocks;
		}

		// Iterate in a cube around the player
		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					Location blockLoc = playerLoc.clone().add(x, y, z);

					// Check if this block is a border (safe next to unsafe)
					if (isBorderBlock(blockLoc)) {
						// Only show the border if it's air or passable
						if (blockLoc.getBlock().getType().isAir() || blockLoc.getBlock().isPassable()) {
							borderBlocks.add(blockLoc.getBlock().getLocation());
						}
					}
				}
			}
		}
		return borderBlocks;
	}

	private boolean isBorderBlock(Location location) {
		boolean isCenterSafe = regionProvider.isLocationSafe(location);
		// Check immediate neighbors (N, S, E, W, Up, Down)
		if (isCenterSafe != regionProvider.isLocationSafe(location.clone().add(1, 0, 0))) return true;
		if (isCenterSafe != regionProvider.isLocationSafe(location.clone().add(-1, 0, 0))) return true;
		if (isCenterSafe != regionProvider.isLocationSafe(location.clone().add(0, 1, 0))) return true;
		if (isCenterSafe != regionProvider.isLocationSafe(location.clone().add(0, -1, 0))) return true;
		if (isCenterSafe != regionProvider.isLocationSafe(location.clone().add(0, 0, 1))) return true;
		if (isCenterSafe != regionProvider.isLocationSafe(location.clone().add(0, 0, -1))) return true;
		return false;
	}

	private boolean isNearSafeZone(Location location, int radius) {
		// Simple check to see if any safe zones are in the broader area to optimize
		for (int x = -radius; x <= radius; x += 4) { // Check less frequently for performance
			for (int y = -radius; y <= radius; y += 4) {
				for (int z = -radius; z <= radius; z += 4) {
					if (regionProvider.isLocationSafe(location.clone().add(x, y, z))) {
						return true;
					}
				}
			}
		}
		return false;
	}
}