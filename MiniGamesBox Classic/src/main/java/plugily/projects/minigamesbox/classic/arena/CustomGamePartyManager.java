package plugily.projects.minigamesbox.classic.arena;

import lombok.Getter;
import org.bukkit.entity.Player;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.handlers.party.GameParty;

import java.util.*;

/**
 * @version 1.0
 * @Author Hai
 * @Date 2026/3/20 17:55
 * @description ???
 */
@Getter
public class CustomGamePartyManager {
    private static CustomGamePartyManager instance;

    private final PluginMain plugin;
    private final Map<UUID,GameParty> gamePartyMap = new HashMap<>();

    public CustomGamePartyManager(PluginMain plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static CustomGamePartyManager get() {
        return instance;
    }

    public GameParty getParty(Player player) {
        plugin.getLogger().info("玩家获取队伍:"+player.getName());
        gamePartyMap.forEach((uuid, gameParty)->{
            plugin.getLogger().info("队伍:"+uuid);
            plugin.getLogger().info("队伍成员:"+gameParty.getPlayers());
        });

        for (Map.Entry<UUID, GameParty> entry : gamePartyMap.entrySet()) {
            if (entry.getValue().getPlayers().contains(player.getName())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void addParty(UUID uuid,GameParty gameParty) {
        this.gamePartyMap.put(uuid,gameParty);
    }
    public void removeParty(UUID gamePartyUUID) {
        this.getGamePartyMap().remove(gamePartyUUID);
    }

}
