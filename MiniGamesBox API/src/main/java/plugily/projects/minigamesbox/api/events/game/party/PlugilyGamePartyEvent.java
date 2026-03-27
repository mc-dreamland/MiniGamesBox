package plugily.projects.minigamesbox.api.events.game.party;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * @version 1.0
 * @Author Hai
 * @Date 2026/3/21 13:13
 * @description ???
 */
@Getter
public class PlugilyGamePartyEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID gamePartyUUID;
    private final String leader;
    private final Set<String> players;
    private final Type type;

    public enum Type{
        ADD,DEL
    }

    public PlugilyGamePartyEvent(UUID gamePartyUUID, String leader, Set<String> players, Type type) {
        this.gamePartyUUID = gamePartyUUID;
        this.leader = leader;
        this.players = players;
        this.type = type;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
