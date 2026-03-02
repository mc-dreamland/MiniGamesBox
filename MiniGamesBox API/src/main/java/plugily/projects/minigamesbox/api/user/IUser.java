package plugily.projects.minigamesbox.api.user;

import org.bukkit.entity.Player;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.api.kit.IKit;
import plugily.projects.minigamesbox.api.stats.IStatisticType;

import java.time.Duration;
import java.util.UUID;

/**
 * @author Lagggpixel
 * @since April 24, 2024
 */
public interface IUser {
  UUID getUniqueId();

  IKit getKit();

  void setKit(IKit kit);

  IPluginArena getArena();

  Player getPlayer();

  boolean isSpectator();

  void setSpectator(boolean spectator);

  boolean isPermanentSpectator();

  void setPermanentSpectator(boolean permanentSpectator);

  long getStatistic(String statistic);

  long getStatistic(IStatisticType statisticType);

  void setStatistic(IStatisticType statisticType, long value);

  void setStatistic(String statistic, long value);

  void adjustStatistic(IStatisticType statisticType, long value);

  void adjustStatistic(String statistic, long value);

  void resetNonePersistentStatistics();

  boolean checkCanCastCooldownAndMessage(String cooldown);

  void setCooldown(String key, long milliseconds);

  void setCooldown(String key, Duration duration);

  long getCooldown(String key);

  Duration getCooldownDuration(String key);

  boolean isInitialized();

  void setInitialized(boolean initialized);
}
