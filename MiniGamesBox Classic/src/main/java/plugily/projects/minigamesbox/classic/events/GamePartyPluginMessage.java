package plugily.projects.minigamesbox.classic.events;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import plugily.projects.minigamesbox.api.events.game.party.PlugilyGamePartyEvent;
import plugily.projects.minigamesbox.classic.PluginMain;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Author Hai
 * @Date 2026/3/21 12:55
 * @description ???
 */
public class GamePartyPluginMessage implements PluginMessageListener {

    private final PluginMain plugin;

    public GamePartyPluginMessage(PluginMain plugin) {
        this.plugin = plugin;
    }


    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        plugin.getLogger().info("GamePartyPluginMessage: 收到组队信息");
        if (channel.equalsIgnoreCase("gameteam:redisteam")) {
            ByteArrayDataInput bis = ByteStreams.newDataInput(message);
            String subChannel = bis.readUTF();
            if (subChannel.equalsIgnoreCase("PlayerList")) {
                List<String> players = Arrays.stream(bis.readUTF().replaceAll("[\\[\\]]", "").split(",")).map(String::trim).toList();
                String owner = bis.readUTF();

                plugin.getLogger().info("队长:" + owner);
                plugin.getLogger().info("队员:" + players);

                UUID gamePartyUUID = UUID.randomUUID();
                Bukkit.getPluginManager().callEvent(new PlugilyGamePartyEvent(gamePartyUUID, owner, new HashSet<>(players), PlugilyGamePartyEvent.Type.ADD));


                players.forEach(p -> {
                    Player p2 = Bukkit.getPlayer(p);
                    if (p2 != null){
                        plugin.getArenaManager().joinAttempt(p2);
                    }
                });

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    Bukkit.getPluginManager().callEvent(new PlugilyGamePartyEvent(gamePartyUUID, owner, new HashSet<>(players), PlugilyGamePartyEvent.Type.DEL));
                }, 20 * 10);

            }
        }
    }

}
