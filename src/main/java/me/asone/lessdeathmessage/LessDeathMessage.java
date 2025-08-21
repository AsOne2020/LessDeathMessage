/*
 * This file is part of the LessDeathMessage project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2025  As_One and contributors
 *
 * LessDeathMessage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LessDeathMessage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with LessDeathMessage.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.asone.lessdeathmessage;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LessDeathMessage extends JavaPlugin implements Listener {

	// Player -> timestamps of deaths (thread-safe)
	private final Map<UUID, List<Long>> deathRecords = new ConcurrentHashMap<>();

	// Player -> death message -> timestamps of repeated messages (thread-safe)
	private final Map<UUID, Map<String, List<Long>>> repeatDeathRecords = new ConcurrentHashMap<>();

	private int timeRange;
	private int limit;
	private int repeatTimeRange;
	private int repeatLimit;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		loadConfigValues();
		Bukkit.getPluginManager().registerEvents(this, this);

		PluginCommand cmd = this.getCommand("lessdeath");
		if (cmd != null) {
			cmd.setExecutor((sender, command, label, args) -> {
				if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
					saveDefaultConfig();
					reloadConfig();
					loadConfigValues();
					sender.sendMessage("§aLessDeathMessage config reloaded!");
					return true;
				}
				sender.sendMessage("§eUsage: /lessdeath reload");
				return true;
			});
		} else {
			getLogger().warning("Command 'lessdeath' not found in plugin.yml!");
		}

		getLogger().info("LessDeathMessage enabled!");
	}

	@Override
	public void onDisable() {
		deathRecords.clear();
		repeatDeathRecords.clear();
		getLogger().info("LessDeathMessage disabled!");
	}

	private void loadConfigValues() {
		this.timeRange = getConfig().getInt("time-range", 60);
		this.limit = getConfig().getInt("limit", 6);
		this.repeatTimeRange = getConfig().getInt("repeat-time-range", 20);
		this.repeatLimit = getConfig().getInt("repeat-limit", 3);
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();

		// bypass permission
		if (player.hasPermission("lessdeathmessage.bypass")) {
			return;
		}

		String deathMessage = event.getDeathMessage();
		if (deathMessage == null) return;

		long now = System.currentTimeMillis();

		// --- repeated message limiter ---
		repeatDeathRecords.putIfAbsent(player.getUniqueId(), new ConcurrentHashMap<>());
		Map<String, List<Long>> playerRepeats = repeatDeathRecords.get(player.getUniqueId());
		playerRepeats.putIfAbsent(deathMessage, new CopyOnWriteArrayList<>());
		List<Long> repeatRecords = playerRepeats.get(deathMessage);

		repeatRecords.removeIf(ts -> (now - ts) > (repeatTimeRange * 1000L));
		repeatRecords.add(now);

		if (repeatRecords.size() > repeatLimit) {
			event.setDeathMessage(null);
			return;
		}

		// --- overall frequency limiter ---
		deathRecords.putIfAbsent(player.getUniqueId(), new CopyOnWriteArrayList<>());
		List<Long> records = deathRecords.get(player.getUniqueId());

		records.removeIf(ts -> (now - ts) > (timeRange * 1000L));
		records.add(now);

		if (records.size() > limit) {
			event.setDeathMessage(null);
		}
	}
}
