package plugily.projects.minigamesbox.classic.handlers.party;

import org.bukkit.entity.Player;
import plugily.projects.minigamesbox.classic.arena.CustomGamePartyManager;

/**
 * @version 1.0
 * @Author Hai
 * @Date 2026/3/20 17:06
 * @description ???
 */
public class CustomGamePartyHandlerImpl implements PartyHandler{
    @Override
    public GameParty getParty(Player player) {
        return CustomGamePartyManager.get().getParty(player);
    }

    @Override
    public boolean partiesSupported() {
        return true;
    }

    @Override
    public PartyPluginType getPartyPluginType() {
        return PartyPluginType.CUSTOM;
    }
}
