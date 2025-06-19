package com.vypnito.vypnitocombat.managers;

import com.vypnito.vypnitocombat.VypnitoCombat;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HealthIndicatorManager {

	private final VypnitoCombat plugin;
	private final Map<UUID, BossBar> activeBossBars = new HashMap<>();

	public HealthIndicatorManager(VypnitoCombat plugin) {
		this.plugin = plugin;
	}

	public void showOrUpdateBossBar(Player attacker, Player target) {
		BossBar bossBar = activeBossBars.computeIfAbsent(attacker.getUniqueId(), k -> {
			BossBar newBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID);
			newBar.addPlayer(attacker);
			return newBar;
		});

		double healthPercentage = Math.max(0, target.getHealth() / target.getMaxHealth());
		String formattedHealth = String.format("%.1f", target.getHealth());
		String maxHealth = String.format("%.1f", target.getMaxHealth());

		String barTitle = plugin.getMessageManager().getRawMessage("health_indicator_boss_bar", "&f%target_name% - &c%current_health% / %max_health% ❤")
				.replace("%target_name%", target.getName())
				.replace("%current_health%", formattedHealth)
				.replace("%max_health%", maxHealth);

		bossBar.setTitle(barTitle);
		bossBar.setProgress(healthPercentage);
	}

	public void hideBossBar(Player player) {
		BossBar bossBar = activeBossBars.remove(player.getUniqueId());
		if (bossBar != null) {
			bossBar.removeAll();
		}
	}
}