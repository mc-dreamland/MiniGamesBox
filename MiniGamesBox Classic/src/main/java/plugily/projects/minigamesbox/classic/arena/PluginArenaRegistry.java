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

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.api.arena.IPluginArenaRegistry;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.handlers.worlds.WorldHandler;
import plugily.projects.minigamesbox.classic.utils.configuration.ConfigUtils;
import plugily.projects.minigamesbox.classic.utils.serialization.LocationSerializer;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


/**
 * @author Tigerpanzer_02
 * <p>
 * Created at 01.11.2021
 */
public class PluginArenaRegistry implements IPluginArenaRegistry {

//  private static class RoomArenaMapping {
//    private final Map<Integer, String> roomToArena = new HashMap<>();
//    private final Map<String, Integer> arenaToRoom = new HashMap<>();
//
//    public void put(int bungeeId, String arenaId) {
//      roomToArena.put(bungeeId, arenaId);
//      arenaToRoom.put(arenaId, bungeeId);
//    }
//    public String getArenaId(int roomId) {
//      return roomToArena.get(roomId);
//    }
//    public Integer getRoomId(@NotNull String arenaId) {
//      return arenaToRoom.get(arenaId);
//    }
//    public boolean containsBungeeId(int bungeeId) {
//      return roomToArena.containsKey(bungeeId);
//    }
//
//    public boolean containsArenaId(@NotNull String arenaId) {
//      return arenaToRoom.containsKey(arenaId);
//    }
//    public void removeByRoomId(int roomId) {
//      String arenaId = roomToArena.remove(roomId);
//      if (arenaId != null) {
//        arenaToRoom.remove(arenaId);
//      }
//    }
//    public void removeByArenaId(@NotNull String arenaId) {
//      Integer bungeeId = arenaToRoom.remove(arenaId);
//      if (bungeeId != null) {
//        roomToArena.remove(bungeeId);
//      }
//    }
//    public void clear() {
//      roomToArena.clear();
//      arenaToRoom.clear();
//    }
//  }


  private final Map<String,IPluginArena> arenas = new LinkedHashMap<>();
  private final PluginMain plugin;
  private final List<World> arenaIngameWorlds = new ArrayList<>();
  private final List<World> arenaWorlds = new ArrayList<>();
//  private final RoomArenaMapping roomArenaMapping = new RoomArenaMapping();

//  private int bungeeArena = -999;
  private String roomId = "";

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
      for (Map.Entry<String, IPluginArena> entry : arenas.entrySet()) {
          IPluginArena value = entry.getValue();
          Set<Player> players = value.getPlayers();
          for (Player arenaPlayer : players) {
              if(arenaPlayer.getUniqueId().equals(playerUUID)) {
                  return value;
              }
          }
      }
//    for(IPluginArena loopArena : arenas) {
//      for(Player arenaPlayer : loopArena.getPlayers()) {
//        if(arenaPlayer.getUniqueId().equals(playerUUID)) {
//          return loopArena;
//        }
//      }
//    }
    return null;
  }

  @Override
  @Nullable
  public IPluginArena getArena(String id) {

    IPluginArena iPluginArena = arenas.get(id);
    if (iPluginArena != null){
      return iPluginArena;
    }
    for (Map.Entry<String, IPluginArena> entry : arenas.entrySet()) {
        if (entry.getKey().equalsIgnoreCase(id)) {
            return entry.getValue();
        }
    }

    return null;
//    for(IPluginArena loopArena : arenas) {
//      if(loopArena.getId().equalsIgnoreCase(id)) {
//        return loopArena;
//      }
//    }
//    return null;
  }

  public void addArena(IPluginArena arena){
    arenas.put(arena.getId(),arena);
  }
  public void removeArena(IPluginArena arena) {
    arenas.values().removeIf(value -> value.getId().equals(arena.getId()));
  }


  @Override
  public int getArenaPlayersOnline() {
    int players = 0;
    for (Map.Entry<String, IPluginArena> entry : arenas.entrySet()) {
        IPluginArena value = entry.getValue();
        Set<Player> arenaPlayers = value.getPlayers();
        players += arenaPlayers.size();
    }
//    for(IPluginArena arena : arenas) {
//      players += arena.getPlayers().size();
//    }
    return players;
  }

  @Override
  public void registerArena(IPluginArena arena) {
    plugin.getDebugger().debug("[{0}] Instance registered", arena.getId());
    addArena(arena);
    World startWorld = arena.getStartLocation().getWorld();
    World endWorld = arena.getEndLocation().getWorld();
    World lobbyWorld = arena.getLobbyLocation().getWorld();
    if(startWorld != null) {
      arenaIngameWorlds.add(startWorld);
      arenaWorlds.add(startWorld);
      loadGameRule(arena);
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

    removeArena(arena);

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
  @Override
  public void restartArena(IPluginArena arena){
    plugin.getLogger().info("重置地图房间:"+arena.getId());
    unregisterArena(arena);
    unregisterWorld(arena);
    Bukkit.getScheduler().runTaskLater(plugin, () -> registerArena(arena.getId()), 300);
  }

  public void unregisterWorld(IPluginArena arena) {
    World world = arena.getStartLocation().getWorld();
    if(world == null) {
      return;
    }
    world.getPlayers().forEach(player -> {
      plugin.getLogger().info("重置地图:"+arena.getId()+" - hub: "+player.getName());
      plugin.getBungeeManager().connectToHub(player);
    });
    Bukkit.getScheduler().runTaskLater(plugin, () -> world.getPlayers().forEach(player -> {
      IPluginArena playerArena = getArena(player);
      plugin.getLogger().warning("重置地图:"+arena.getId()+" - kick: "+player.getName() + " arena:" + (playerArena!=null?playerArena.getId():"无") + " isOnline:"+player.isOnline());
      player.kick(Component.text("世界重启"), PlayerKickEvent.Cause.UNKNOWN);
    }), 100);
    Bukkit.getScheduler().runTaskLater(plugin, () ->{
      if(!WorldHandler.deleteWorld(world)) {
        plugin.getDebugger().debug("[{0}] 卸载,删除 世界 失败 {1}", arena.getId(), world.getName());
      }
    } , 200);
  }

  public PluginArena getNewArena(String id) {
    return new PluginArena(id);
  }

  @Override
  public void registerArenas() {
    plugin.getDebugger().debug("[ArenaRegistry] Initial arenas registration");
    long start = System.currentTimeMillis();
    if(!arenas.isEmpty()) {
      for(IPluginArena arena : new ArrayList<>(arenas.values())) {
        unregisterArena(arena);
      }
//      for(IPluginArena arena : new ArrayList<>(arenas)) {
//        unregisterArena(arena);
//      }
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
//      roomArenaMapping.put(count, key);
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
        IPluginArena iPluginArena = arenas.get(key);
        if(iPluginArena != null) {
          unregisterArena(iPluginArena);
        }
//      List<IPluginArena> sameArenas = arenas.stream().filter(pluginArena -> pluginArena.getId().equals(key)).collect(Collectors.toList());
//      if(!sameArenas.isEmpty()) {
//        for(IPluginArena arena : new ArrayList<>(sameArenas)) {
//          unregisterArena(arena);
//        }
//      }
    }

    FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
    ConfigurationSection section = config.getConfigurationSection("instances");
    if(section == null) {
      plugin.getDebugger().sendConsoleMsg(new MessageBuilder("VALIDATOR_NO_INSTANCES_CREATED").asKey().build());
      return;
    }
    if (!WorldHandler.copyWorldFile(plugin,key)) {
      plugin.getLogger().warning("地图文件异常");
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

    shuffleBungeeArena();
    ConfigUtils.saveConfig(plugin, config, "arenas");
    plugin.getSignManager().loadSigns();

    plugin.getDebugger().debug("[ArenaRegistry] Arena registration for " + key + " completed took {0}ms", System.currentTimeMillis() - start);
  }
  public void loadGameRule(IPluginArena arena) {
    plugin.getGameRuleManager().loadGameRule(arena);
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
    return new ArrayList<>(arenas.values());
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
          IPluginArena currentBungeeArena = getCurrentBungeeArena();

          if (currentBungeeArena == null || (currentBungeeArena.getArenaState() != IArenaState.WAITING_FOR_PLAYERS && currentBungeeArena.getArenaState() != IArenaState.STARTING) || currentBungeeArena.getPlayers().size() >= currentBungeeArena.getMaximumPlayers()) {
              for (IPluginArena value : arenas.values()) {
                  if ((value.getArenaState() == IArenaState.WAITING_FOR_PLAYERS || value.getArenaState() == IArenaState.STARTING) && value.getPlayers().size() < value.getMaximumPlayers()){
                      String originalRoomId =  roomId;
                      roomId = value.getId();
                      plugin.getLogger().info("随机房间 " + originalRoomId + " -> " + roomId);
                      break;
                  }
              }
          }

//          if (!roomId.isEmpty()){
//            List<IPluginArena> pluginArenas = plugin.getArenaRegistry().getArenas();
//            IPluginArena iPluginArena = pluginArenas.get(bungeeArena);
//              if ((iPluginArena.getArenaState() != IArenaState.WAITING_FOR_PLAYERS && iPluginArena.getArenaState() != IArenaState.STARTING) || iPluginArena.getPlayers().size() >= iPluginArena.getMaximumPlayers()) {
//                for (int i = 0; i < pluginArenas.size(); i++) {
//                  IPluginArena arena = pluginArenas.get(i);
//                  if ((arena.getArenaState() == IArenaState.WAITING_FOR_PLAYERS || arena.getArenaState() == IArenaState.STARTING) && arena.getPlayers().size() < arena.getMaximumPlayers()){
//                    bungeeArena = i;
//                    break;
//                  }
//                }
//              }
//          }
      }
  }


  @Override
  public @Nullable IPluginArena getCurrentBungeeArena(){
      return arenas.get(roomId);
  }

  @Override
  public void setRoomId(String roomId) {
      this.roomId = roomId;
  }

  @Override
  public String getRoomId() {
    return this.roomId;
  }
  //  @Override
//  public int getBungeeArena() {
//    if(bungeeArena == -999 && !arenas.isEmpty()) {
//      bungeeArena = ThreadLocalRandom.current().nextInt(arenas.size());
//    }
//    return bungeeArena;
//  }

//  @Override
//  public void addBungeeArenaMapping(int bungeeId, String arenaId) {
//    roomArenaMapping.put(bungeeId, arenaId);
//  }

//  @Override
//  public String getArenaId(int roomId) {
//    return roomArenaMapping.getArenaId(roomId);
//  }

//  @Override
//  public int getRoomId(String arenaId) {
//    return roomArenaMapping.getRoomId(arenaId);
//  }
}
