package plugily.projects.minigamesbox.classic.events;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import plugily.projects.minigamesbox.api.events.game.party.PlugilyGamePartyEvent;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.arena.CustomGamePartyManager;
import plugily.projects.minigamesbox.classic.handlers.party.GameParty;

import java.util.Map;
import java.util.UUID;

/**
 * @version 1.0
 * @Author Hai
 * @Date 2026/3/21 13:34
 * @description ???
 */
@Getter
public class GamePartyEvent  implements Listener {
    private final PluginMain plugin;
    public GamePartyEvent(PluginMain plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    @EventHandler
    public void onGamePartyEvent(PlugilyGamePartyEvent event){
        PlugilyGamePartyEvent.Type type = event.getType();
        switch (type){
            case ADD:
                CustomGamePartyManager.get().addParty(event.getGamePartyUUID(),new GameParty(event.getGamePartyUUID(),event.getLeader(),event.getPlayers()));
                break;
            case DEL:
                CustomGamePartyManager.get().removeParty(event.getGamePartyUUID());
                break;
        }
    }

}
