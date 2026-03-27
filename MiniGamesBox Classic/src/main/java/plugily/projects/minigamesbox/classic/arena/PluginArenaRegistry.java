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

package plugily.projects.minigamesbox.classic.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.api.arena.IPluginArenaRegistry;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.configuration.ConfigUtils;
import plugily.projects.minigamesbox.classic.utils.serialization.LocationSerializer;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


/**
 * @author Tigerpanzer_02
 * <p>
 * Created at 01.11.2021
 */
public class PluginArenaRegistry implements IPluginArenaRegistry {

  private static class BungeeArenaMapping {
    private final Map<Integer, String> bungeeToArena = new HashMap<>();
    private final Map<String, Integer> arenaToBungee = new HashMap<>();

    public void put(int bungeeId, String arenaId) {
      bungeeToArena.put(bungeeId, arenaId);
      arenaToBungee.put(arenaId, bungeeId);
    }
    public String getArenaId(int bungeeId) {
      return bungeeToArena.get(bungeeId);
    }
    public Integer getBungeeId(@NotNull String arenaId) {
      return arenaToBungee.get(arenaId);
    }
    public boolean containsBungeeId(int bungeeId) {
      return bungeeToArena.containsKey(bungeeId);
    }

    public boolean containsArenaId(@NotNull String arenaId) {
      return arenaToBungee.containsKey(arenaId);
    }
    public void removeByBungeeId(int bungeeId) {
      String arenaId = bungeeToArena.remove(bungeeId);
      if (arenaId != null) {
        arenaToBungee.remove(arenaId);
      }
    }
    public void removeByArenaId(@NotNull String arenaId) {
      Integer bungeeId = arenaToBungee.remove(arenaId);
      if (bungeeId != null) {
        bungeeToArena.remove(bungeeId);
      }
    }
    public void clear() {
      bungeeToArena.clear();
      arenaToBungee.clear();
    }
  }


  private final List<IPluginArena> arenas = new ArrayList<>();
  private final PluginMain plugin;
  private final List<World> arenaIngameWorlds = new ArrayList<>();
  private final List<World> arenaWorlds = new ArrayList<>();
  private final BungeeArenaMapping bungeeArenaMapping = new BungeeArenaMapping();

  private int bungeeArena = -999;

  public PluginArenaRegistry(PluginMain plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean isInArena(@NotNull Player player) {
    return getArena(player) != null;
  }


  @Nullable
  @Override
  public IPluginArena getArena(Player player) {
    if(player == null) {
      return null;
    }

    return getArena(player.getUniqueId());
  }

  @Override
  @Nullable
  public IPluginArena getArena(UUID playerUUID) {
    if(playerUUID == null) {
      return null;
    }

    for(IPluginArena loopArena : arenas) {
      for(Player arenaPlayer : loopArena.getPlayers()) {
        if(arenaPlayer.getUniqueId().equals(playerUUID)) {
          return loopArena;
        }
      }
    }

    return null;
  }

  @Override
  @Nullable
  public IPluginArena getArena(String id) {
    for(IPluginArena loopArena : arenas) {
      if(loopArena.getId().equalsIgnoreCase(id)) {
        return loopArena;
      }
    }
    return null;
  }

  @Override
  public int getArenaPlayersOnline() {
    int players = 0;
    for(IPluginArena arena : arenas) {
      players += arena.getPlayers().size();
    }
    return players;
  }

  @Override
  public void registerArena(IPluginArena arena) {
    plugin.getDebugger().debug("[{0}] Instance registered", arena.getId());
    arenas.add(arena);
    World startWorld = arena.getStartLocation().getWorld();
    World endWorld = arena.getEndLocation().getWorld();
    World lobbyWorld = arena.getLobbyLocation().getWorld();
    if(startWorld != null) {
      arenaIngameWorlds.add(startWorld);
      arenaWorlds.add(startWorld);
    }
    if(endWorld != null) {
      arenaWorlds.add(endWorld);
    }
    if(lobbyWorld != null) {
      arenaWorlds.add(lobbyWorld);
    }
  }

  @Override
  public void unregisterArena(IPluginArena arena) {
    plugin.getArenaManager().stopGame(true, arena);
    for(Player player : new HashSet<>(arena.getPlayers())) {
      plugin.getArenaManager().leaveAttempt(player, arena);
      new MessageBuilder("COMMANDS_TELEPORTED_TO_LOBBY").asKey().player(player).arena(arena).sendPlayer();
    }
    plugin.getDebugger().debug("[{0}] Instance unregistered", arena.getId());
    arenas.remove(arena);

    World startWorld = arena.getStartLocation().getWorld();
    World endWorld = arena.getEndLocation().getWorld();
    World lobbyWorld = arena.getLobbyLocation().getWorld();
    if(startWorld != null) {
      arenaIngameWorlds.remove(startWorld);
      arenaWorlds.remove(startWorld);
    }
    if(endWorld != null) {
      arenaWorlds.remove(endWorld);
    }
    if(lobbyWorld != null) {
      arenaWorlds.remove(lobbyWorld);
    }
    plugin.getSignManager().loadSigns();
  }

  public PluginArena getNewArena(String id) {
    return new PluginArena(id);
  }

  @Override
  public void registerArenas() {
    plugin.getDebugger().debug("[ArenaRegistry] Initial arenas registration");
    long start = System.currentTimeMillis();
    if(!arenas.isEmpty()) {
      for(IPluginArena arena : new ArrayList<>(arenas)) {
        unregisterArena(arena);
      }
    }
    FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
    ConfigurationSection section = config.getConfigurationSection("instances");
    if(section == null) {
      plugin.getDebugger().sendConsoleMsg(new MessageBuilder("VALIDATOR_NO_INSTANCES_CREATED").asKey().build());
      return;
    }
    int amount = ConfigUtils.getConfig(plugin, "config").getInt("arenaAmount",1);
    int count = 0;
    Set<String> keys = section.getKeys(false);
    ArrayList<String> list = new ArrayList<>(keys);
    Collections.shuffle(list);
    for (String key : list) {
      if (count>=amount){
        return;
      }
      if(key.equalsIgnoreCase("default")) {
        continue;
      }
      registerArena(key);
      bungeeArenaMapping.put(count, key);
      count++;
    }
//    for(String id : section.getKeys(false)) {
//      if(id.equalsIgnoreCase("default")) {
//        continue;
//      }
//      registerArena(id);
//    }
    plugin.getDebugger().debug("[ArenaRegistry] Arenas registration completed took {0}ms", System.currentTimeMillis() - start);
  }

  @Override
  public void registerArena(String key) {
    plugin.getDebugger().debug("[ArenaRegistry] Initial arena registration for " + key);
    long start = System.currentTimeMillis();
    if(!arenas.isEmpty()) {
      List<IPluginArena> sameArenas = arenas.stream().filter(pluginArena -> pluginArena.getId().equals(key)).collect(Collectors.toList());
      if(!sameArenas.isEmpty()) {
        for(IPluginArena arena : new ArrayList<>(sameArenas)) {
          unregisterArena(arena);
        }
      }
    }

    FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
    ConfigurationSection section = config.getConfigurationSection("instances");
    if(section == null) {
      plugin.getDebugger().sendConsoleMsg(new MessageBuilder("VALIDATOR_NO_INSTANCES_CREATED").asKey().build());
      return;
    }

    PluginArena arena = getNewArena(key);

    if(!validatorChecks(section, arena, key) || !additionalValidatorChecks(section, arena, key)) {
      section.set(key + ".isdone", false);
      ConfigUtils.saveConfig(plugin, config, "arenas");
      registerArena(arena);
    } else {
      arena.setReady(true);
      registerArena(arena);
      arena.start();
      plugin.getDebugger().sendConsoleMsg(new MessageBuilder("VALIDATOR_INSTANCE_STARTED").asKey().arena(arena).build());
    }

    ConfigUtils.saveConfig(plugin, config, "arenas");
    plugin.getSignManager().loadSigns();

    plugin.getDebugger().debug("[ArenaRegistry] Arena registration for " + key + " completed took {0}ms", System.currentTimeMillis() - start);
  }

  public boolean additionalValidatorChecks(ConfigurationSection section, PluginArena arena, String id) {
    return true;
  }

  private boolean validatorChecks(ConfigurationSection section, PluginArena arena, String id) {

    arena.setMinimumPlayers(section.getInt(id + ".minimumplayers", 3));
    arena.setMaximumPlayers(section.getInt(id + ".maximumplayers", 16));
    arena.setMapName(section.getString(id + ".mapname", id));

    Location lobbyLoc = LocationSerializer.getLocation(section.getString(id + ".lobbylocation", null));
    if(lobbyLoc != null) {
      arena.setLobbyLocation(lobbyLoc);
    }
    Location startLoc = LocationSerializer.getLocation(section.getString(id + ".startlocation", null));
    if(startLoc != null) {
      arena.setStartLocation(startLoc);
    }
    Location endLoc = LocationSerializer.getLocation(section.getString(id + ".endlocation", null));
    if(endLoc != null) {
      arena.setEndLocation(endLoc);
    }
    Location spectatorLoc = LocationSerializer.getLocation(section.getString(id + ".spectatorlocation", null));
    if(spectatorLoc != null) {
      arena.setSpectatorLocation(spectatorLoc);
    }
    if(lobbyLoc == null || startLoc == null || endLoc == null || spectatorLoc == null) {
      plugin.getDebugger().sendConsoleMsg(new MessageBuilder("VALIDATOR_INVALID_ARENA_CONFIGURATION").asKey().value("LOCATIONS ARE INVALID").arena(arena).build());
      return false;
    }

    if(!section.getBoolean(id + ".isdone", false)) {
      plugin.getDebugger().sendConsoleMsg(new MessageBuilder("VALIDATOR_INVALID_ARENA_CONFIGURATION").asKey().value("NOT VALIDATED").arena(arena).build());
      return false;
    }

    return true;
  }

  @NotNull
  @Override
  public List<IPluginArena> getArenas() {
    return new ArrayList<>(arenas);
  }

  @Override
  public List<World> getArenaIngameWorlds() {
    return arenaIngameWorlds;
  }

  @Override
  public List<World> getArenaWorlds() {
    return arenaWorlds;
  }

  @Override
  public void shuffleBungeeArena() {
      if(!arenas.isEmpty()) {
          if (bungeeArena != -999){
            List<IPluginArena> pluginArenas = plugin.getArenaRegistry().getArenas();
            IPluginArena iPluginArena = pluginArenas.get(bungeeArena);
              if ((iPluginArena.getArenaState() != IArenaState.WAITING_FOR_PLAYERS && iPluginArena.getArenaState() != IArenaState.STARTING) || iPluginArena.getPlayers().size() >= iPluginArena.getMaximumPlayers()) {
                for (int i = 0; i < pluginArenas.size(); i++) {
                  IPluginArena arena = pluginArenas.get(i);
                  if ((arena.getArenaState() == IArenaState.WAITING_FOR_PLAYERS || arena.getArenaState() == IArenaState.STARTING) && arena.getPlayers().size() < arena.getMaximumPlayers()){
                    bungeeArena = i;
                    break;
                  }
                }
              }
          }
      }
  }

  @Override
  public int getBungeeArena() {
    if(bungeeArena == -999 && !arenas.isEmpty()) {
      bungeeArena = ThreadLocalRandom.current().nextInt(arenas.size());
    }
    return bungeeArena;
  }

  @Override
  public void addBungeeArenaMapping(int bungeeId, String arenaId) {
    bungeeArenaMapping.put(bungeeId, arenaId);
  }

  @Override
  public String getArenaId(int bungeeId) {
    return bungeeArenaMapping.getArenaId(bungeeId);
  }

  @Override
  public int getBungeeId(String arenaId) {
    return bungeeArenaMapping.getBungeeId(arenaId);
  }
}
