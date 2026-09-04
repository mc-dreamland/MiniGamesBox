/*
 *  MiniGamesBox - Library box with massive content that could be seen as minigames core.
 *  Copyright (C) 2023 Plugily Projects - maintained by Tigerpanzer_02 and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package plugily.projects.minigamesbox.classic.user.data;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import plugily.projects.minigamesbox.api.stats.IStatisticType;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.api.user.data.UserDatabase;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.utils.configuration.ConfigUtils;
import plugily.projects.minigamesbox.database.MysqlDatabase;
import plugily.projects.minigamesbox.sorter.SortUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * @author Tigerpanzer_02
 * <p>
 * Created at 01.11.2021
 */
public class FileStats implements UserDatabase {

  private final PluginMain plugin;
  private final ExecutorService saveExecutor;
  private final Object statsFileLock = new Object();

  public FileStats(PluginMain plugin) {
    this.plugin = plugin;
    this.saveExecutor = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "MiniGamesBox-FileStats-Save");
      thread.setDaemon(true);
      return thread;
    });
  }

  @Override
  public void saveStatistic(IUser user, IStatisticType stat) {
    String uuid = user.getUniqueId().toString();
    String statisticName = stat.getName();
    long value = user.getStatistic(stat);
    saveAsync(() -> {
      FileConfiguration config = ConfigUtils.getConfig(plugin, "stats");
      config.set(uuid + "." + statisticName, value);
      ConfigUtils.saveConfig(plugin, config, "stats");
    });
  }

  @Override
  public void saveAllStatistic(IUser user) {
    updateStats(user);
  }

  @Override
  public void loadStatistics(IUser user) {
    String uuid = user.getUniqueId().toString();
    synchronized(statsFileLock) {
      FileConfiguration config = ConfigUtils.getConfig(plugin, "stats");
      plugin.getStatsStorage().getStatistics().forEach((s, statisticType) ->
          user.setStatistic(statisticType, config.getInt(uuid + "." + statisticType.getName())));
    }
  }

  @Override
  public void addColumn(String columnName, String columnProperties) {
    //skip
  }

  @Override
  public void dropColumn(String columnName) {
    //skip
  }

  @NotNull
  @Override
  public Map<UUID, Long> getStats(IStatisticType stat) {
    Map<UUID, Integer> stats = new TreeMap<>();
    synchronized(statsFileLock) {
      FileConfiguration config = ConfigUtils.getConfig(plugin, "stats");
      for(String string : config.getKeys(false)) {
        if(string.equals("data-version")) {
          continue;
        }
        try {
          stats.put(UUID.fromString(string), config.getInt(string + "." + stat.getName()));
        } catch(IllegalArgumentException ex) {
          plugin.getLogger().log(Level.WARNING, "Cannot load the UUID for {0}", string);
        }
      }
    }
    return SortUtils.sortByValue(stats);
  }

  @Override
  public void disable() {
    for(Player player : plugin.getServer().getOnlinePlayers()) {
      updateStats(plugin.getUserManager().getUser(player));
    }
    saveExecutor.shutdown();
    try {
      if(!saveExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
        plugin.getLogger().warning("Timed out while saving player statistics.");
      }
    } catch(InterruptedException exception) {
      Thread.currentThread().interrupt();
      plugin.getLogger().warning("Interrupted while saving player statistics.");
    }
  }

  @Override
  public MysqlDatabase getMySQLDatabase() {
    return null;
  }

  @Override
  public String getPlayerName(UUID uuid) {
    synchronized(statsFileLock) {
      return ConfigUtils.getConfig(plugin, "stats").getString(uuid + ".playername", Bukkit.getOfflinePlayer(uuid).getName());
    }
  }

  private void updateStats(IUser user) {
    String uuid = user.getUniqueId().toString();
    Map<String, Long> statistics = new HashMap<>();
    plugin.getStatsStorage().getStatistics().forEach((s, statisticType) -> {
      if(statisticType.isPersistent()) {
        statistics.put(statisticType.getName(), user.getStatistic(statisticType));
      }
    });
    String playerName = user.getPlayer() == null ? null : user.getPlayer().getName();
    saveAsync(() -> {
      FileConfiguration config = ConfigUtils.getConfig(plugin, "stats");
      statistics.forEach((statisticName, value) -> {
        String path = uuid + "." + statisticName;
        if(value > 0 || config.contains(path)) {
          config.set(path, value);
        }
      });
      if(playerName != null) {
        config.set(uuid + ".playername", playerName);
      }
      ConfigUtils.saveConfig(plugin, config, "stats");
    });
  }

  private void saveAsync(Runnable saveTask) {
    saveExecutor.execute(() -> {
      synchronized(statsFileLock) {
        saveTask.run();
      }
    });
  }
}
