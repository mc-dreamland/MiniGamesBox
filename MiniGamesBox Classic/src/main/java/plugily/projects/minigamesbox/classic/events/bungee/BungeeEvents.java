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
package plugily.projects.minigamesbox.classic.events.bungee;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.api.arena.IPluginArenaRegistry;
import plugily.projects.minigamesbox.api.events.game.PlugilyGameStateChangeEvent;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.arena.states.ArenaState;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;

/**
 * @author Tigerpanzer_02
 * <p>
 * Created at 09.10.2021
 */
public class BungeeEvents implements Listener {

  private final PluginMain plugin;

  public BungeeEvents(PluginMain plugin) {
    this.plugin = plugin;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void onLogin(PlayerLoginEvent e) {
    if (e.getResult() != PlayerLoginEvent.Result.ALLOWED) {
      return;
    }
    if(!plugin.getServer().hasWhitelist() || e.getResult() != PlayerLoginEvent.Result.KICK_WHITELIST) {
      return;
    }
    IPluginArenaRegistry arenaRegistry = plugin.getArenaRegistry();
    IPluginArena iPluginArena = arenaRegistry.getArenas().get(arenaRegistry.getBungeeArena());
    if(e.getPlayer().hasPermission(plugin.getPluginNamePrefixLong() +".fullgames")) {
      e.setResult(PlayerLoginEvent.Result.ALLOWED);
    } else if (IArenaState.IN_GAME == iPluginArena.getArenaState() && iPluginArena.getPlayers().size() >= iPluginArena.getMaximumPlayers()) {
      e.setResult(PlayerLoginEvent.Result.KICK_FULL);
    }

    if(!arenaRegistry.getArenas().isEmpty()) {
      VersionUtils.teleport(e.getPlayer(), arenaRegistry.getArenas().get(arenaRegistry.getBungeeArena()).getLobbyLocation());
    }
  }


}
