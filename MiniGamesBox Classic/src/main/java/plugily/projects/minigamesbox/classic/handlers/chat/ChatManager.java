package plugily.projects.minigamesbox.classic.handlers.chat;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import plugily.projects.minigamesbox.classic.PluginMain;

/**
 * @version 1.0
 * @Author Hai
 * @Date 2026/3/19 11:05
 * @description ???
 */
@Getter
public class ChatManager {
    private final PluginMain plugin;
    public ChatManager(PluginMain plugin) {
        this.plugin = plugin;
    }
    public @NotNull Component replace(@NotNull Player sender,Component displayName, @NotNull Component current) {
        return current;
    }
}
