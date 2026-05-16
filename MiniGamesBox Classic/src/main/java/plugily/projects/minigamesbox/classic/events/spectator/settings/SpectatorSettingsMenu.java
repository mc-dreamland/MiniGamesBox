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

package plugily.projects.minigamesbox.classic.events.spectator.settings;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.handlers.reward.Reward;
import plugily.projects.minigamesbox.classic.handlers.reward.RewardType;
import plugily.projects.minigamesbox.classic.utils.actionbar.ActionBar;
import plugily.projects.minigamesbox.classic.utils.configuration.ConfigUtils;
import plugily.projects.minigamesbox.classic.utils.helper.ItemBuilder;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.events.api.PlugilyPlayerInteractEntityEvent;
import plugily.projects.minigamesbox.inventory.normal.NormalFastInv;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Tigerpanzer_02
 * <p>
 * Created at 10.12.2021
 */
public class SpectatorSettingsMenu implements Listener {

  private final PluginMain plugin;
  private final NormalFastInv inventory;
  private final FileConfiguration config;
  private final List<SpectatorSettingsItem> settingsItems = new ArrayList<>();
  private final Set<UUID> firstPersonMode = new HashSet<>();
  private final Set<UUID> autoTeleport = new HashSet<>();
  private final Map<UUID, UUID> targetPlayer = new HashMap<>();
  private final Set<UUID> invisibleSpectators = new HashSet<>();

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    plugin.getSpectatorItemsManager().getSpectatorSettingsMenu().clearPlayer(event.getPlayer());
  }

  public void clearPlayer(Player player) {
    clearPlayer(player.getUniqueId());
  }

  private void clearPlayer(UUID playerId) {
    firstPersonMode.remove(playerId);
    autoTeleport.remove(playerId);
    targetPlayer.remove(playerId);
    targetPlayer.entrySet().removeIf(entry -> entry.getValue().equals(playerId));
    invisibleSpectators.remove(playerId);
  }


  public SpectatorSettingsMenu(PluginMain plugin) {
    this.plugin = plugin;
    config = ConfigUtils.getConfig(plugin, "spectator");
    loadSpectatorSettingsItems();
    inventory = setupSpectatorSettings();
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickSpectatorSettings, 20L, 20L);
  }

  private void loadSpectatorSettingsItems() {
    ConfigurationSection section = config.getConfigurationSection("Settings-Menu.Content");
    if(section == null) {
      return;
    }
    for(String type : section.getKeys(false)) {
      String path = "Settings-Menu.Content." + type;
      Material mat = XMaterial.matchXMaterial(config.getString(path + ".material", "BEDROCK").toUpperCase()).orElse(XMaterial.BEDROCK).parseMaterial();
      String name = new MessageBuilder(config.getString(path + ".name", "Error!")).build();
      int slot = config.getInt(path + ".slot", -1);
      List<String> description = config.getStringList(path + ".description").stream()
          .map(itemLore -> itemLore = new MessageBuilder(itemLore).build())
          .collect(Collectors.toList());
      SpectatorSettingsItem.Type function = SpectatorSettingsItem.Type.NONE;
      try {
        function = SpectatorSettingsItem.Type.valueOf(type.toUpperCase());
      } catch(Exception ignored) {
      }
      Set<Reward> rewards = new HashSet<>();
      for(String reward : config.getStringList(path + ".execute")) {
        rewards.add(new Reward(new RewardType(path), reward));
      }
      settingsItems.add(new SpectatorSettingsItem(new ItemBuilder(mat).name(name).lore(description).build(), slot, config.getString(path + ".permission", null), rewards, function));
    }
  }

  private NormalFastInv setupSpectatorSettings() {
    NormalFastInv gui = new NormalFastInv(plugin.getBukkitHelper().serializeInt(45), new MessageBuilder(config.getString("Settings-Menu.Inventory-name", "Settings Menu")).build());
    for(SpectatorSettingsItem item : settingsItems) {
      gui.setItem(item.getSlot(), item.getItemStack(), event -> {
        Player player = (Player) event.getWhoClicked();
        IPluginArena arena = plugin.getArenaRegistry().getArena(player);
        if(arena == null) {
          return;
        }
        if(item.getPermission() != null && !item.getPermission().equalsIgnoreCase("")) {
          if(!plugin.getBukkitHelper().hasPermission(player, item.getPermission())) {
            return;
          }
        }
        plugin.getDebugger().debug("!!! SpectatorSettings " + item.getType());
        switch(item.getType()) {
          case DEFAULT_SPEED:
            new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_CHANGED_SPEED").asKey().arena(arena).integer(0).player(player).sendPlayer();
            player.removePotionEffect(XPotion.SPEED.getPotionEffectType());
            player.setFlySpeed(0.15f);
            break;
          case SPEED1:
            new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_CHANGED_SPEED").asKey().arena(arena).integer(1).player(player).sendPlayer();
            player.removePotionEffect(XPotion.SPEED.getPotionEffectType());
            player.setFlySpeed(0.2f);
            XPotion.SPEED.buildPotionEffect(Integer.MAX_VALUE, 1).apply(player);
            break;
          case SPEED2:
            new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_CHANGED_SPEED").asKey().arena(arena).integer(2).player(player).sendPlayer();
            player.removePotionEffect(XPotion.SPEED.getPotionEffectType());
            player.setFlySpeed(0.25f);
            XPotion.SPEED.buildPotionEffect(Integer.MAX_VALUE, 2).apply(player);
            break;
          case SPEED3:
            new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_CHANGED_SPEED").asKey().arena(arena).integer(3).player(player).sendPlayer();
            player.removePotionEffect(XPotion.SPEED.getPotionEffectType());
            player.setFlySpeed(0.3f);
            XPotion.SPEED.buildPotionEffect(Integer.MAX_VALUE, 3).apply(player);
            break;
          case SPEED4:
            new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_CHANGED_SPEED").asKey().arena(arena).integer(4).player(player).sendPlayer();
            player.removePotionEffect(XPotion.SPEED.getPotionEffectType());
            player.setFlySpeed(0.35f);
            XPotion.SPEED.buildPotionEffect(Integer.MAX_VALUE, 4).apply(player);
            break;
          case AUTO_TELEPORT:
            if(autoTeleport.contains(player.getUniqueId())) {
              autoTeleport.remove(player.getUniqueId());
              new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_AUTO_TELEPORT").asKey().arena(arena).value(new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_STATUS_DISABLED").asKey().build()).player(player).sendPlayer();
            } else {
              autoTeleport.add(player.getUniqueId());
              new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_AUTO_TELEPORT").asKey().arena(arena).value(new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_STATUS_ENABLED").asKey().build()).player(player).sendPlayer();
            }
            break;
          case NIGHT_VISION:
            if(player.getActivePotionEffects().stream().anyMatch(potionEffect -> potionEffect.getType().equals(XPotion.NIGHT_VISION.getPotionEffectType()))) {
              player.removePotionEffect(XPotion.NIGHT_VISION.getPotionEffectType());
              new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_NIGHT_VISION").asKey().arena(arena).value(new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_STATUS_DISABLED").asKey().build()).player(player).sendPlayer();
            } else {
              XPotion.NIGHT_VISION.buildPotionEffect(Integer.MAX_VALUE, 1).apply(player);
              new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_NIGHT_VISION").asKey().arena(arena).value(new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_STATUS_ENABLED").asKey().build()).player(player).sendPlayer();
            }
            break;
          case FIRST_PERSON_MODE:
            if(!targetPlayer.containsKey(player.getUniqueId())) {
              return;
            }
            autoTeleport.remove(player.getUniqueId());
            firstPersonMode.add(player.getUniqueId());
            VersionUtils.sendTitle(player, new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_FIRST_PERSON_MODE_TITLE").asKey().player(player).arena(arena).build(), 5, 20, 5);
            Player target = getValidTarget(player, arena);
            if(target == null) {
              targetPlayer.remove(player.getUniqueId());
              firstPersonMode.remove(player.getUniqueId());
              return;
            }
            player.setGameMode(GameMode.SPECTATOR);
            player.setSpectatorTarget(target);
            break;
          case SPECTATORS_VISIBILITY:
            if(invisibleSpectators.contains(player.getUniqueId())) {
              invisibleSpectators.remove(player.getUniqueId());
              for(Player players : arena.getPlayers()) {
                VersionUtils.showPlayer(plugin, player, players);
              }
              new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_VISIBILITY").asKey().arena(arena).value(new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_STATUS_ENABLED").asKey().build()).player(player).sendPlayer();
            } else {
              invisibleSpectators.add(player.getUniqueId());
              for(Player players : arena.getPlayers()) {
                if(!plugin.getUserManager().getUser(players).isSpectator()) {
                  continue;
                }
                VersionUtils.hidePlayer(plugin, player, players);
              }
              new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_VISIBILITY").asKey().arena(arena).value(new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_STATUS_DISABLED").asKey().build()).player(player).sendPlayer();
            }
            break;
          case NONE:
            break;
        }
        plugin.getRewardsHandler().performReward(player, new HashSet<>(item.getRewards()));
      });
    }
    return gui;
  }

  private void tickSpectatorSettings() {
    Set<UUID> trackedPlayers = new HashSet<>();
    trackedPlayers.addAll(firstPersonMode);
    trackedPlayers.addAll(autoTeleport);
    trackedPlayers.addAll(targetPlayer.keySet());
    trackedPlayers.addAll(invisibleSpectators);

    for(UUID playerId : trackedPlayers) {
      Player player = plugin.getServer().getPlayer(playerId);
      if(player == null || !player.isOnline()) {
        clearPlayer(playerId);
        continue;
      }
      IUser user = plugin.getUserManager().getUser(player);
      IPluginArena arena = user.getArena();
      if(arena == null || !user.isSpectator()) {
        clearPlayer(playerId);
        continue;
      }

      Player target = getValidTarget(player, arena);
      if(target == null) {
        targetPlayer.remove(playerId);
        firstPersonMode.remove(playerId);
        autoTeleport.remove(playerId);
        continue;
      }

      if(firstPersonMode.contains(playerId)) {
        if(!target.equals(player.getSpectatorTarget())) {
          firstPersonMode.remove(playerId);
        } else {
          plugin.getActionBarManager().addActionBar(player, new ActionBar(new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_FIRST_PERSON_MODE_ACTION_BAR").asKey().arena(arena).player(target), ActionBar.ActionBarType.DISPLAY));
          continue;
        }
      }

      if(player.getLocation().getWorld() != target.getLocation().getWorld()) {
        continue;
      }
      double distance = player.getLocation().distance(target.getLocation());
      plugin.getActionBarManager().addActionBar(player, new ActionBar(new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_TARGET_PLAYER_ACTION_BAR").asKey().arena(arena).integer((int) distance).player(target), ActionBar.ActionBarType.DISPLAY));
      if(distance > 15 && autoTeleport.contains(playerId)) {
        VersionUtils.teleport(player, target.getLocation());
      }
    }
  }

  private Player getValidTarget(Player player, IPluginArena arena) {
    UUID targetId = targetPlayer.get(player.getUniqueId());
    if(targetId == null) {
      return null;
    }
    Player target = plugin.getServer().getPlayer(targetId);
    if(target == null || !target.isOnline()) {
      return null;
    }
    if(!arena.equals(plugin.getArenaRegistry().getArena(target))) {
      return null;
    }
    if(plugin.getUserManager().getUser(target).isSpectator()) {
      return null;
    }
    return target;
  }

  @EventHandler
  public void onPlayerClick(PlugilyPlayerInteractEntityEvent event) {
    Player player = event.getPlayer();
    if(!plugin.getUserManager().getUser(player).isSpectator()) {
      return;
    }
    if(!(event.getRightClicked() instanceof Player)) {
      return;
    }
    Player target = (Player) event.getRightClicked();
    if(!plugin.getArenaRegistry().isInArena(target)) {
      return;
    }
    IPluginArena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null || !arena.equals(plugin.getArenaRegistry().getArena(target))) {
      return;
    }
    targetPlayer.put(player.getUniqueId(), target.getUniqueId());
    if(!autoTeleport.contains(player.getUniqueId())) {
      firstPersonMode.add(player.getUniqueId());
      VersionUtils.sendTitle(player, new MessageBuilder("IN_GAME_SPECTATOR_SPECTATOR_MENU_SETTINGS_FIRST_PERSON_MODE_TITLE").asKey().player(player).build(), 5, 20, 5);
      player.setGameMode(GameMode.SPECTATOR);
      player.setSpectatorTarget(target);
    }
  }

  @EventHandler
  public void onPlayerSneak(PlayerToggleSneakEvent event) {
    Player player = event.getPlayer();
    if(!plugin.getUserManager().getUser(player).isSpectator()) {
      return;
    }
    if(!(player.getSpectatorTarget() instanceof Player)) {
      return;
    }
    firstPersonMode.remove(player.getUniqueId());
    player.setSpectatorTarget(null);
    player.setGameMode(GameMode.SURVIVAL);
    player.setAllowFlight(true);
    player.setFlying(true);
    VersionUtils.setCollidable(player, false);
  }

  public NormalFastInv getInventory() {
    return inventory;
  }
}
