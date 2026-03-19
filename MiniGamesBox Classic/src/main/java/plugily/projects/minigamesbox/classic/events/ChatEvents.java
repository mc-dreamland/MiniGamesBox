package plugily.projects.minigamesbox.classic.events;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.handlers.chat.ChatManager;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;


/**
 * @author Tigerpanzer_02
 * <p>Created at 09.10.2021
 */
public class ChatEvents implements Listener {

    private final PluginMain plugin;
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    public ChatEvents(PluginMain plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void asyncChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        IPluginArena arena = plugin.getArenaRegistry().getArena(sender);
        if (plugin.getConfigPreferences().getOption("SEPARATE_ARENA_CHAT")) {
            event.viewers().removeIf(audience -> {
                if (!(audience instanceof Player viewer)) return false;
                if (plugin.getArgumentsRegistry().getSpyChat().isSpyChatEnabled(viewer)) return false;
                if (arena == null) {
                    return plugin.getArenaRegistry().getArena(viewer) != null;
                } else {
                    if (plugin.getConfigPreferences().getOption("SEPARATE_ARENA_SPECTATORS")) {
                        IUser user = plugin.getUserManager().getUser(sender);
                        boolean senderIsSpec = user.isSpectator();
                        boolean viewerIsSpec = arena.getPlayersLeft().contains(viewer);
                        if (senderIsSpec) return viewerIsSpec;
                        else return !viewerIsSpec;
                    }
                    return !arena.getPlayers().contains(viewer);
                }
            });
        }

        if (plugin.getConfigPreferences().getOption("PLUGIN_CHAT_FORMAT")) {
            event.renderer((source, sourceDisplayName, message, viewer) ->
                    buildFullChatComponent(source, arena, sourceDisplayName, message)
            );
        }
    }

    public @NotNull Component buildFullChatComponent(@NotNull Player sender, @Nullable IPluginArena arena, @NotNull Component displayName, @NotNull Component message) {
        IUser user = plugin.getUserManager().getUser(sender);
        String formatBase = this.getFormatStringWithStatistics(user, arena);

        Component base = LEGACY_SERIALIZER.deserialize(formatBase)
                .replaceText(b -> b.matchLiteral("%player%").replacement(displayName))
                .replaceText(b -> b.matchLiteral("%message%").replacement(message));

        ChatManager chatManager = plugin.getChatManager();
        base = chatManager.replace(sender, displayName, base);

        return base;
    }


    private String getFormatStringWithStatistics(IUser user, IPluginArena arena) {
        String rawFormat = new MessageBuilder("IN_GAME_GAME_CHAT_FORMAT").asKey().getRaw();
        if (user.isSpectator()) {
            String deathTag = new MessageBuilder("IN_GAME_DEATH_TAG").asKey().build();
            if (rawFormat.contains("%kit%")) {
                rawFormat = rawFormat.replace("%kit%", deathTag);
            } else {
                rawFormat = deathTag + rawFormat;
            }
        } else {
            String kitName = plugin.getConfigPreferences().getOption("KITS") ? user.getKit().getName() : "-";
            rawFormat = rawFormat.replace("%kit%", kitName);
        }
        return new MessageBuilder(rawFormat)
                .arena(arena)
                .player(user.getPlayer())
                .build();
    }
}