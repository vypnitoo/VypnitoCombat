package com.vypnito.vypnitocombat.expansions;

import com.vypnito.vypnitocombat.VypnitoCombat;
import com.vypnito.vypnitocombat.managers.CombatManager;
import com.vypnito.vypnitocombat.managers.MessageManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VypnitoCombatPlaceholders extends PlaceholderExpansion {

	private final VypnitoCombat plugin;
	private final CombatManager combatManager;
	private final MessageManager messageManager;

	public VypnitoCombatPlaceholders(VypnitoCombat plugin) {
		this.plugin = plugin;
		this.combatManager = plugin.getCombatManager();
		this.messageManager = plugin.getMessageManager();
	}

	@Override
	public @NotNull String getIdentifier() {
		return "combatlogv";
	}

	@Override
	public @NotNull String getAuthor() {
		return "Vypnito";
	}

	@Override
	public @NotNull String getVersion() {
		return plugin.getDescription().getVersion();
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
		if (player == null) {
			return "";
		}

		switch (params.toLowerCase()) {
			case "in_combat":
				return combatManager.isInCombat(player)
						? messageManager.getRawMessage("placeholder_in_combat_yes", "&aYes")
						: messageManager.getRawMessage("placeholder_in_combat_no", "&cNo");

			case "time_remaining":
				if (combatManager.isInCombat(player)) {
					Long endTime = combatManager.getCombatEndTime(player.getUniqueId());
					if (endTime != null) {
						long remainingMillis = endTime - System.currentTimeMillis();
						if (remainingMillis > 0) {
							return String.format("%.1f", remainingMillis / 1000.0);
						}
					}
				}
				return messageManager.getRawMessage("placeholder_not_in_combat_time", "0.0");

			default:
				return null;
		}
	}
}