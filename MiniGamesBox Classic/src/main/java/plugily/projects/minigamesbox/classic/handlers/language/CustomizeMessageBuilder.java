package plugily.projects.minigamesbox.classic.handlers.language;

import org.bukkit.entity.Player;
import plugily.projects.minigamesbox.api.arena.IPluginArena;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Hai
 * @Date 2025/4/26 10:15
 * @description 自定义信息 例如: new CustomizeMessageBuilder("AAA").value("%team%","红队").value("%score%","1").getMessageBuilder().sendArenaBuilder(arena);
 * @version 1.0
 */
public class CustomizeMessageBuilder {

    private final String message;
    private final Map<String, String> placeholders = new HashMap<>();

    public CustomizeMessageBuilder(String message) {
        this.message = message;
    }

    public CustomizeMessageBuilder value(String placeholder, String value) {
        placeholders.put(placeholder, value);
        return this;
    }

    public MessageBuilder getMessageBuilder() {
        String build = new MessageBuilder(message).asKey().build();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            build = build.replace(entry.getKey(), entry.getValue());
        }
        return new MessageBuilder(build);
    }

    public String getStringBuilder() {
        String build = new MessageBuilder(message).asKey().build();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            build = build.replace(entry.getKey(), entry.getValue());
        }
        return build;
    }

    public void sendPlayerBuilder(Player player) {
        getMessageBuilder().player(player).sendPlayer();
    }

    public void sendArenaBuilder(IPluginArena arena) {
        getMessageBuilder().arena(arena).sendArena();
    }

    public MessageBuilder getMessage() {
        String newMessage = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            newMessage = newMessage.replace(entry.getKey(), entry.getValue());
        }
        return new MessageBuilder(newMessage);
    }

    public String getString() {
        String newMessage = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            newMessage = newMessage.replace(entry.getKey(), entry.getValue());
        }
        return newMessage;
    }

    public void sendPlayer(Player player) {
        getMessage().player(player).sendPlayer();
    }

    public void sendArena(IPluginArena arena) {
        getMessage().arena(arena).sendArena();
    }
}
