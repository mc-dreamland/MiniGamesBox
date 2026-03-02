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

package plugily.projects.minigamesbox.classic.user;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.api.events.player.PlugilyPlayerStatisticChangeEvent;
import plugily.projects.minigamesbox.api.kit.IKit;
import plugily.projects.minigamesbox.api.stats.IStatisticType;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Tigerpanzer_02
 * <p>
 * Created at 01.11.2021
 */
public class User implements IUser {

  private static PluginMain plugin;
//  private static long cooldownCounter = 0;
  private final UUID uuid;
  private boolean spectator = false;
  private boolean permanentSpectator = false;
  private IKit kit;
  private final Map<IStatisticType, Long> stats = new HashMap<>();
  private final Map<String, Long> cooldowns = new HashMap<>();
  private boolean initialized;

  public User(UUID uuid) {
    this.uuid = uuid;
  }

  @Override
  public UUID getUniqueId() {
    return uuid;
  }

  public static void init(PluginMain plugin) {
    User.plugin = plugin;
  }

//  public static void cooldownHandlerTask() {
//    Bukkit.getScheduler().runTaskTimer(plugin, () -> cooldownCounter++, 20, 20);
//  }

  @Override
  public IKit getKit() {
    if(kit == null) {
      return plugin.getKitRegistry().getDefaultKit();
    }
    return kit;
  }

  @Override
  public void setKit(IKit kit) {
    this.kit = kit;
  }

  @Override
  public IPluginArena getArena() {
    return plugin.getArenaRegistry().getArena(getPlayer());
  }

  @Override
  public Player getPlayer() {
    return Bukkit.getPlayer(uuid);
  }

  @Override
  public boolean isSpectator() {
    return spectator;
  }

  @Override
  public void setSpectator(boolean spectator) {
    this.spectator = spectator;
  }

  @Override
  public boolean isPermanentSpectator() {
    return permanentSpectator;
  }

  @Override
  public void setPermanentSpectator(boolean permanentSpectator) {
    this.permanentSpectator = permanentSpectator;
  }

  @Override
  public long getStatistic(String statistic) {
    return getStatistic(plugin.getStatsStorage().getStatisticType(statistic.toUpperCase()));
  }

  @Override
  public long getStatistic(IStatisticType statisticType) {
    return stats.computeIfAbsent(statisticType, t -> 0L);
  }

  @Override
  public void setStatistic(IStatisticType statisticType, long value) {
    changeUserStatistic(statisticType, value);
  }

  @Override
  public void setStatistic(String statistic, long value) {
    changeUserStatistic(plugin.getStatsStorage().getStatisticType(statistic), value);
  }

  private void changeUserStatistic(IStatisticType statisticType, long value) {
    stats.put(statisticType, value);

    Player player = getPlayer();

    if(player != null) {
      plugin.getDebugger().debug("Set User {0} statistic to {1} for {2} ", statisticType.getName(), value, player.getName());

      //statistics manipulation events are called async when using mysql
      Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new PlugilyPlayerStatisticChangeEvent(plugin.getArenaRegistry().getArena(player), player, statisticType, value)));
    }
  }

  @Override
  public void adjustStatistic(IStatisticType statisticType, long value) {
    changeUserStatistic(statisticType, getStatistic(statisticType) + value);
  }

  @Override
  public void adjustStatistic(String statistic, long value) {
    IStatisticType statisticType = plugin.getStatsStorage().getStatisticType(statistic);
    changeUserStatistic(statisticType, getStatistic(statisticType) + value);
  }

  @Override
  public void resetNonePersistentStatistics() {
    for(IStatisticType statisticType : plugin.getStatsStorage().getStatistics().values()) {
      if(!statisticType.isPersistent()) {
        setStatistic(statisticType, 0);
      }
    }
  }

  @Override
  public boolean checkCanCastCooldownAndMessage(String cooldown) {
    double time = getCooldown(cooldown);

    if(time <= 0) {
      return true;
    }
    new MessageBuilder("KIT_COOLDOWN").asKey().integer((int) time).player(getPlayer()).sendPlayer();
    return false;
  }

  @Override
  public void setCooldown(String key, long milliseconds) {
    cooldowns.put(key, milliseconds + System.currentTimeMillis());
  }

  @Override
  public double getCooldown(String key) {
    return Math.max(cooldowns.getOrDefault(key, 0L) - System.currentTimeMillis(), 0) * 0.001;
  }

  @Override
  public boolean isInitialized() {
    return initialized;
  }

  @Override
  public void setInitialized(boolean initialized) {
    this.initialized = initialized;
  }
}
