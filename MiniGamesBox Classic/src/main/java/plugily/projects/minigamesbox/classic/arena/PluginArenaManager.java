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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.api.arena.IPluginArenaRegistry;
import plugily.projects.minigamesbox.api.events.game.PlugilyGameJoinAttemptEvent;
import plugily.projects.minigamesbox.api.events.game.PlugilyGameLeaveAttemptEvent;
import plugily.projects.minigamesbox.api.events.game.PlugilyGamePlayerStartEvent;
import plugily.projects.minigamesbox.api.events.game.PlugilyGameStopEvent;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.arena.states.ArenaState;
import plugily.projects.minigamesbox.classic.handlers.items.SpecialItem;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.handlers.language.TitleBuilder;
import plugily.projects.minigamesbox.classic.handlers.party.GameParty;
import plugily.projects.minigamesbox.classic.utils.misc.MiscUtils;
import plugily.projects.minigamesbox.classic.utils.misc.complement.ComplementAccessor;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * @author Tigerpanzer_02
 * <p>
 * Created at 01.11.2021
 */
public class PluginArenaManager {

  private final PluginMain plugin;

  private final Cache<UUID, String> rejoinCache;     //重连房间id保存
  private final Cache<UUID, String> joiningPlayers;  //正在加入的玩家

  public void removeRejoinCache(Player player) {
    rejoinCache.invalidate(player.getUniqueId());
  }

  public void removeRejoinCache(String roomId){
    rejoinCache.asMap().values().removeIf(value -> value.equals(roomId));
  }

  public String getRejoinRoomId(Player player){
    String roomId = rejoinCache.getIfPresent(player.getUniqueId());
    if (roomId != null){
      return roomId;
    }
    return "";
  }

  public int getRejoinSize(IPluginArena arena, Player player) {
    return (int) rejoinCache.asMap().entrySet().stream()
            .filter(entry -> {
              if (player == null) return true;
              return !entry.getKey().equals(player.getUniqueId());
            })
            .filter(entry -> Objects.equals(entry.getValue(), arena.getId()))
            .count();
  }
  public int getJoiningSize(IPluginArena arena, Player player) {
    return (int) joiningPlayers.asMap().entrySet().stream()
            .filter(entry -> {
              if (player == null) return true;
              return !entry.getKey().equals(player.getUniqueId());
            })
            .filter(entry -> Objects.equals(entry.getValue(), arena.getId()))
            .count();
  }

  public PluginArenaManager(PluginMain plugin) {
    this.plugin = plugin;
    rejoinCache = CacheBuilder.newBuilder()
            .expireAfterWrite(plugin.getBungeeManager().getRejoinTime(), TimeUnit.SECONDS)
            .build();
    joiningPlayers = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();
  }

  /**
   * Attempts player to join arena.
   * Calls PlugilyGameJoinAttemptEvent.
   * Can be cancelled only via above-mentioned event
   *
   * @param player player to join
   * @param arena  arena to join
   * @see PlugilyGameJoinAttemptEvent
   */
  public void joinAttempt(@NotNull Player player, @NotNull IPluginArena arena) {
    plugin.getDebugger().debug("[{0}] Initial join attempt for {1}", arena.getId(), player.getName());
    long start = System.currentTimeMillis();
    if(!canJoinArenaAndMessage(player, arena) || !checkFullGamePermission(player, arena)) {
      plugin.getLogger().info("加入失败");
      arena.getPlayers().remove(player);
      return;
    }
    plugin.getDebugger().debug("[{0}] Checked join attempt for {1}", arena.getId(), player.getName());
    joiningPlayers.put(player.getUniqueId(), arena.getId());

    if(arena.getArenaState() == IArenaState.IN_GAME || arena.getArenaState() == IArenaState.ENDING) {
      if(!plugin.getConfigPreferences().getOption("SPECTATORS")) {
        new MessageBuilder("IN_GAME_SPECTATOR_BLOCKED").asKey().player(player).arena(arena).sendPlayer();
        arena.getPlayers().remove(player);
        joiningPlayers.invalidate(player.getUniqueId());
        return;
      }
      PluginArenaUtils.preparePlayerForGame(arena, player, arena.getSpectatorLocation(), true).thenAccept(success -> {
        if(!success) {
          handleJoinFailure(player, arena, "旁观传送失败");
          return;
        }
        if(!markJoinSuccess(player, arena)) {
          return;
        }
        new MessageBuilder("IN_GAME_SPECTATOR_YOU_ARE_SPECTATOR").asKey().player(player).arena(arena).sendPlayer();
        PluginArenaUtils.hidePlayer(player, arena);
        for(Player spectator : arena.getPlayers()) {
          if(plugin.getUserManager().getUser(spectator).isSpectator()) {
            VersionUtils.hidePlayer(plugin, player, spectator);
          } else {
            VersionUtils.showPlayer(plugin, player, spectator);
          }
        }
        additionalSpectatorSettings(player, arena);
        plugin.getDebugger().debug("[{0}] Final join attempt as spectator for {1} took {2}ms", arena.getId(), player.getName(), System.currentTimeMillis() - start);
      }).exceptionally(throwable -> {
        handleJoinFailure(player, arena, "旁观加入准备异常");
        plugin.getLogger().log(Level.WARNING, "[" + arena.getId() + "] 玩家 " + player.getName() + " 旁观加入准备异常", throwable);
        return null;
      });
      return;
    }

    PluginArenaUtils.preparePlayerForGame(arena, player, arena.getLobbyLocation(), false).thenAccept(success -> {
      if(!success) {
        handleJoinFailure(player, arena, "大厅传送失败");
        return;
      }
      if(!markJoinSuccess(player, arena)) {
        return;
      }
      new MessageBuilder(MessageBuilder.ActionType.JOIN).arena(arena).player(player).sendArena();
      new TitleBuilder("IN_GAME_JOIN_TITLE").asKey().arena(arena).player(player).sendPlayer();

      if(plugin.getConfigPreferences().getOption("KITS")) {
        plugin.getUserManager().getUser(player).setKit(plugin.getKitRegistry().getDefaultKit());
      }
      plugin.getSpecialItemManager().addSpecialItemsOfStage(player, SpecialItem.DisplayStage.LOBBY);
      if(arena.getArenaState() == IArenaState.WAITING_FOR_PLAYERS) {
        plugin.getSpecialItemManager().addSpecialItemsOfStage(player, SpecialItem.DisplayStage.WAITING_FOR_PLAYERS);
      } else if(ArenaState.isStartingStage(arena)) {
        plugin.getSpecialItemManager().addSpecialItemsOfStage(player, SpecialItem.DisplayStage.ENOUGH_PLAYERS_TO_START);
      }

      for(Player arenaPlayer : arena.getPlayers()) {
        PluginArenaUtils.showPlayer(arenaPlayer, arena);
      }

      plugin.getSignManager().updateSigns();
      plugin.getDebugger().debug("[{0}] Final join attempt as player for {1} took {2}ms", arena.getId(), player.getName(), System.currentTimeMillis() - start);
    }).exceptionally(throwable -> {
      handleJoinFailure(player, arena, "大厅加入准备异常");
      plugin.getLogger().log(Level.WARNING, "[" + arena.getId() + "] 玩家 " + player.getName() + " 大厅加入准备异常", throwable);
      return null;
    });
  }

  private boolean markJoinSuccess(Player player, IPluginArena arena) { //加入arena是否成功
    joiningPlayers.invalidate(player.getUniqueId());
    if(!player.isOnline()) {
      arena.getPlayers().remove(player);
      return false;
    }
    arena.getPlayers().add(player);
    removeRejoinCache(player);
    return true;
  }

  private void handleJoinFailure(Player player, IPluginArena arena, String reason) {
    joiningPlayers.invalidate(player.getUniqueId());
    arena.getPlayers().remove(player);
    plugin.getLogger().warning("玩家 " + player.getName() + " 加入房间 " + arena.getId() + " 失败，原因: " + reason);
    if(player.isOnline()) {
      player.kick(Component.text("加入游戏失败"), PlayerKickEvent.Cause.UNKNOWN);
    }
  }

  public void joinAttempt(@NotNull Player player) {
    plugin.getLogger().info("正在尝试加入游戏:" + player.getName());
    if (plugin.getArenaRegistry().isInArena(player) || isJoinPending(player)) {
      plugin.getLogger().info("已处于房间内或正在加入中，跳过加入:" + player.getName());
      return;
    }
    if (joinAsParty(player)) {
      return;
    }
    if (joinAsRejoin(player)) {
      return;
    }
    if (joinAsNormal(player)) {
      return;
    }
    plugin.getLogger().info("加入失败:" + player.getName());
    player.kick(Component.text("加入游戏失败"), PlayerKickEvent.Cause.UNKNOWN);
  }

  private boolean isJoinPending(@NotNull Player player) {
    return joiningPlayers.getIfPresent(player.getUniqueId()) != null;
  }

  public boolean joinAsNormal(@NotNull Player player){
//    int bungeeArena = plugin.getArenaRegistry().getBungeeArena();
//    IPluginArena iPluginArena = plugin.getArenaRegistry().getArenas().get(bungeeArena);
    IPluginArenaRegistry arenaRegistry = plugin.getArenaRegistry();
    IPluginArena iPluginArena = arenaRegistry.getCurrentBungeeArena();
    if (iPluginArena == null) {
      plugin.getLogger().warning("房间异常");
      return false;
    }
    this.joinAttempt(player, iPluginArena);

    if (arenaRegistry.isInArena(player) || isJoinPending(player)) {
      onJoinComplete(player, iPluginArena);
      onNormalJoinComplete(player, iPluginArena);
      return true;
    }
    return false;
  }

  private boolean joinAsRejoin(@NotNull Player player){
    if(!plugin.getBungeeManager().isRejoinEnabled()) {
      plugin.getLogger().info("未开启重连");
      return false;
    }
    String rejoinRoomId = getRejoinRoomId(player);
    if (rejoinRoomId.isEmpty()){
//      plugin.getLogger().info("无重连记录");
      return false;
    }
//    String arenaId = plugin.getArenaRegistry().getArenaId(rejoinRoomId);
    IPluginArena iPluginArena = plugin.getArenaRegistry().getArena(rejoinRoomId);
    if (iPluginArena == null) {
      return false;
    }
    this.joinAttempt(player, iPluginArena);

    if (plugin.getArenaRegistry().isInArena(player) || isJoinPending(player)) {
      onJoinComplete(player, iPluginArena);
      onRejoinComplete(player, iPluginArena);
      return true;
    }
    return false;
  }

  private boolean joinAsParty(@NotNull Player player) {
    //队伍加入
    if(!plugin.getBungeeManager().isPartyJoinEnabled()) {
      plugin.getLogger().info("未开启组队加入");
      return false;
    }
    GameParty party = plugin.getPartyHandler().getParty(player);
    if(party == null) {
//      plugin.getLogger().info("无队伍信息");
      return false;
    }
    Player leader = Bukkit.getPlayer(party.getLeader());
    if (leader == null || !leader.isOnline()){
      plugin.getLogger().info("队长不在线");
      return false;
    }
    IPluginArena leaderArena = plugin.getArenaRegistry().getArena(leader);
    if (leaderArena == null) {
      plugin.getLogger().info("队长不在房间中");
      return false;
    }
    if (!plugin.getBungeeManager().isPartyJoinInGame() && leaderArena.getArenaState() == IArenaState.IN_GAME ) {
      plugin.getLogger().info("队长房间已开始游戏");
      return false;
    }

    this.joinAttempt(player, leaderArena);

    if (plugin.getArenaRegistry().isInArena(player) || isJoinPending(player)) {
      onJoinComplete(player, leaderArena);
      onPartyJoinComplete(player, leaderArena, leader);
      return true;
    }
    return false;
  }

  public void onJoinComplete(Player player, IPluginArena arena) {
  }

  public void onPartyJoinComplete(Player player, IPluginArena arena, Player partyLeader) {
    plugin.getLogger().info("组队加入:" + player.getName() + " -> " + arena.getId());
  }

  public void onRejoinComplete(Player player, IPluginArena arena) {
    plugin.getLogger().info("重连加入:" + player.getName() + " -> " + arena.getId());
    new PlugilyGamePlayerStartEvent(player, arena).callEvent();
  }

  public void onNormalJoinComplete(Player player, IPluginArena arena) {
    plugin.getLogger().info("普通加入:" + player.getName() + " -> " + arena.getId());
  }

  public void additionalPartyJoin(Player player, IPluginArena arena, Player partyLeader) {

  }

  public void additionalSpectatorSettings(Player player, IPluginArena arena) {

  }

  private boolean checkFullGamePermission(Player player, IPluginArena arena) {
    if(arena.getPlayers().size() + getJoiningSize(arena,player) + getRejoinSize(arena,player) + 1 <= arena.getMaximumPlayers()) {
      return true;
    }
    if(!player.hasPermission(plugin.getPluginNamePrefixLong() + ".fullgames")) {
      player.kickPlayer(new MessageBuilder("IN_GAME_JOIN_FULL_GAME").asKey().player(player).arena(arena).build());
      return false;
    }
    for(Player arenaPlayer : arena.getPlayers()) {
      if(arenaPlayer.hasPermission(plugin.getPluginNamePrefixLong() + ".fullgames")) {
        continue;
      }
      if(ArenaState.isLobbyStage(arena)) {
        leaveAttempt(arenaPlayer, arena);
        new MessageBuilder("IN_GAME_MESSAGES_LOBBY_YOU_WERE_KICKED_FOR_PREMIUM").asKey().player(player).arena(arena).sendPlayer();
        new MessageBuilder("IN_GAME_MESSAGES_LOBBY_KICKED_FOR_PREMIUM").asKey().player(arenaPlayer).arena(arena).sendArena();
      }
      return true;
    }
    player.kickPlayer(new MessageBuilder("IN_GAME_JOIN_NO_SLOTS_FOR_PREMIUM").asKey().player(player).arena(arena).build());
    return false;
  }

  private boolean canJoinArenaAndMessage(Player player, IPluginArena arena) {
    if(!arena.isReady()) {
      new MessageBuilder("IN_GAME_JOIN_ARENA_NOT_CONFIGURED").asKey().player(player).arena(arena).sendPlayer();
      return false;
    }

    PlugilyGameJoinAttemptEvent event = new PlugilyGameJoinAttemptEvent(player, arena);
    Bukkit.getPluginManager().callEvent(event);
    if(event.isCancelled()) {
      new MessageBuilder("IN_GAME_JOIN_CANCEL_API").asKey().player(player).arena(arena).sendPlayer();
      return false;
    }

    if(plugin.getArenaRegistry().isInArena(player)) {
      new MessageBuilder("IN_GAME_JOIN_ALREADY_PLAYING").asKey().arena(arena).player(player).sendPlayer();
      return false;
    }
    if(arena.getArenaState() == IArenaState.RESTARTING) {
      if(plugin.getConfigPreferences().getOption("BUNGEEMODE")) {
        ComplementAccessor.getComplement().kickPlayer(player, new MessageBuilder(arena.getArenaState().getFormattedName() + "...").prefix().build());
        return false;
      }
      new MessageBuilder(arena.getArenaState().getFormattedName() + "...").prefix().player(player).sendPlayer();
      return false;
    }
    return true;
  }

  /**
   * Attempts player to leave arena.
   * Calls PlugilyGameLeaveAttemptEvent event.
   *
   * @param player player to leave
   * @param arena  arena to leave
   * @see PlugilyGameLeaveAttemptEvent
   */
  public void leaveAttempt(@NotNull Player player, @NotNull IPluginArena arena) {
    plugin.getDebugger().debug("[{0}] Initial leave attempt of {1}", arena.getId(), player.getName());
    long start = System.currentTimeMillis();

    Bukkit.getPluginManager().callEvent(new PlugilyGameLeaveAttemptEvent(player, arena));
    if (plugin.getBungeeManager().isRejoinEnabled()){
      addPlayerQuitData(player,arena);
    }
    IUser user = plugin.getUserManager().getUser(player);

    if(!user.isSpectator()) {
      if(arena.getArenaState() != IArenaState.FULL_GAME && arena.getArenaState() != IArenaState.WAITING_FOR_PLAYERS && arena.getArenaState() != IArenaState.STARTING && arena.getPlayers().isEmpty()) {
        stopGame(true, arena);
        plugin.getDebugger().debug(Level.INFO, "[{0}] Game stopped due to lack of players", arena.getId());
      }
    }
    plugin.getUserManager().saveAllStatistic(user);
    PluginArenaUtils.resetPlayerAfterGame(arena, player);
    if(!user.isSpectator()) {
      new MessageBuilder(MessageBuilder.ActionType.LEAVE).arena(arena).player(player).sendArena();
      new MessageBuilder(MessageBuilder.ActionType.LEAVE).arena(arena).player(player).sendPlayer();
    }
    plugin.getSignManager().updateSigns();
    plugin.getDebugger().debug("[{0}] Final leave attempt for {1} took {2}ms", arena.getId(), player.getName(), System.currentTimeMillis() - start);
    arena.getPlayers().remove(player);
  }

  public void addPlayerQuitData(@NotNull Player player, @NotNull IPluginArena arena) {
    if (arena.getArenaState() == IArenaState.IN_GAME) {
//      int roomId = plugin.getArenaRegistry().getRoomId(arena.getId());
      rejoinCache.put(player.getUniqueId(), arena.getId());
    }
  }

  /**
   * Stops current arena. Calls PlugilyGameStopEvent event
   *
   * @param quickStop should arena be stopped immediately? (use only in important cases)
   * @param arena     which arena should stop
   * @see PlugilyGameStopEvent
   */
  public void stopGame(boolean quickStop, @NotNull IPluginArena arena) {
    plugin.getDebugger().debug("[{0}] Game stop event start", arena.getId());
    long start = System.currentTimeMillis();
    removeRejoinCache(arena.getId());
    Bukkit.getPluginManager().callEvent(new PlugilyGameStopEvent(arena));
    for(Player player : arena.getPlayers()) {
      if(quickStop) {
        new MessageBuilder("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_PLAYERS").asKey().arena(arena).sendArena();
      } else {
        spawnFireworks(arena, player);
        for(String msg : plugin.getLanguageManager().getLanguageList("In-Game.Messages.Game-End.Summary")) {
          MiscUtils.sendCenteredMessage(player, new MessageBuilder(msg).player(player).arena(arena).build());
        }
      }
    }
    if(quickStop) {
      arena.setTimer(0, true);
      arena.setArenaState(IArenaState.RESTARTING, true);
    } else {
      arena.setTimer(plugin.getConfig().getInt("Time-Manager.Ending", 10), true);
      arena.setArenaState(IArenaState.ENDING, true);
    }

    for(Player players : arena.getPlayers()) {
      plugin.getSpecialItemManager().removeSpecialItemsOfStage(players, SpecialItem.DisplayStage.IN_GAME);
      plugin.getSpecialItemManager().addSpecialItemsOfStage(players, SpecialItem.DisplayStage.ENDING);
    }
    plugin.getDebugger().debug("[{0}] Game stop event finished took {1}ms", arena.getId(), System.currentTimeMillis() - start);
  }

  private void spawnFireworks(IPluginArena arena, Player player) {
    if(!plugin.getConfigPreferences().getOption("FIREWORK")) {
      return;
    }
    new BukkitRunnable() {
      int i = 0;

      @Override
      public void run() {
        if(i == 4 || arena.getArenaState() == IArenaState.RESTARTING || !arena.getPlayers().contains(player)) {
          cancel();
          return;
        }
        MiscUtils.spawnRandomFirework(player.getLocation());
        i++;
      }
    }.runTaskTimer(plugin, 30, 30);
  }

}
