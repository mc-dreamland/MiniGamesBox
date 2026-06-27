package plugily.projects.minigamesbox.api.events.game;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.api.events.PlugilyEvent;

/**
 * @version 1.0
 * @Author Hai
 * @Date 2026/6/27 13:46
 * @description ???
 */
public class PlugilyGamePlayerStartEvent extends PlugilyEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;

    public PlugilyGamePlayerStartEvent(Player player, IPluginArena arena) {
        super(arena);
        this.player = player;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

}
