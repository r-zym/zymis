package pl.sunglasses.zymis.command;

import org.bukkit.entity.Player;
import pl.sunglasses.zymis.util.CommandUtil.CommandInfo;
import pl.sunglasses.zymis.util.CommandUtil.ExecuteCommand;
import pl.sunglasses.zymis.util.TextUtil;
import pl.sunglasses.zymis.ZymItemshopPlugin;
import pl.sunglasses.zymis.config.ConfigManager;
import pl.sunglasses.zymis.manager.ClaimManager;
import pl.sunglasses.zymis.menu.ClaimMenu;

@CommandInfo(name = "odbierz", aliases = {"claim"})
public class OdbierzCommand {

    private final ZymItemshopPlugin plugin;
    private final ClaimManager claimManager;
    private final ConfigManager configManager;

    public OdbierzCommand(ZymItemshopPlugin plugin, ClaimManager claimManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.configManager = configManager;
    }

    @ExecuteCommand(usage = "/odbierz")
    public void execute(Player player) {
        if (claimManager.getClaims(player.getUniqueId()).isEmpty()) {
            TextUtil.sendMessage(player, "no_items_to_claim", plugin);
            return;
        }

        new ClaimMenu(plugin, claimManager).open(player);
    }
}
