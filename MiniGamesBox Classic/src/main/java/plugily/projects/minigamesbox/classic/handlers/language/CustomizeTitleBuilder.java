package plugily.projects.minigamesbox.classic.handlers.language;

import org.bukkit.entity.Player;
import plugily.projects.minigamesbox.api.arena.IPluginArena;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Hai
 * @Date 2025/5/3 11:11
 * @description 暂无
 * @version 1.0
 */
public class CustomizeTitleBuilder {

    private final String message;
    private final Map<String, String> placeholders = new HashMap<>();

    public CustomizeTitleBuilder(String message) {
        this.message = message;
    }

    public CustomizeTitleBuilder value(String placeholder, String value) {
        placeholders.put(placeholder, value);
        return this;
    }

    public TitleBuilder getTitleBuilder() {
        String build = new MessageBuilder(message).asKey().build();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            build = build.replace(entry.getKey(), entry.getValue());
        }
        return new TitleBuilder(build);
    }

    public String getStringBuilder() {
        String build = new MessageBuilder(message).asKey().build();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            build = build.replace(entry.getKey(), entry.getValue());
        }
        return build;
    }

    public void sendPlayerBuilder(Player player) {
        getTitleBuilder().player(player).sendPlayer();
    }

    public void sendArenaBuilder(IPluginArena arena) {
        getTitleBuilder().arena(arena).sendArena();
    }

    public TitleBuilder getTitle() {
        String newMessage = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            newMessage = newMessage.replace(entry.getKey(), entry.getValue());
        }
        return new TitleBuilder(newMessage);
    }

    public String getString() {
        String newMessage = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            newMessage = newMessage.replace(entry.getKey(), entry.getValue());
        }
        return newMessage;
    }

    public void sendPlayer(Player player) {
        getTitle().player(player).sendPlayer();
    }

    public void sendArena(IPluginArena arena) {
        getTitle().arena(arena).sendArena();
    }
}
