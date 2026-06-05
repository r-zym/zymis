package pl.sunglasses.zymis.command;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.sunglasses.zymis.util.CommandUtil.Arg;
import pl.sunglasses.zymis.util.CommandUtil.CommandInfo;
import pl.sunglasses.zymis.util.CommandUtil.ExecuteCommand;
import pl.sunglasses.zymis.util.TextUtil;
import pl.sunglasses.zymis.ZymItemshopPlugin;
import pl.sunglasses.zymis.config.ConfigManager;
import pl.sunglasses.zymis.manager.ClaimManager;
import pl.sunglasses.zymis.menu.ProductCreatorMenu;

import java.util.Map;

@CommandInfo(name = "zymis", permission = "zymis.admin")
public class ZymisCommand {

    private final ZymItemshopPlugin plugin;
    private final ConfigManager configManager;
    private final ClaimManager claimManager;

    public ZymisCommand(ZymItemshopPlugin plugin, ConfigManager configManager, ClaimManager claimManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.claimManager = claimManager;
    }

    @ExecuteCommand(subCommand = "create", usage = "/zymis create <nazwa>")
    public void create(Player player, @Arg("nazwa") String name) {
        new ProductCreatorMenu(plugin, configManager, name, null).open(player);
    }

    @ExecuteCommand(subCommand = "edit", usage = "/zymis edit <nazwa>")
    public void edit(Player player, @Arg("nazwa") String name) {
        ItemStack product = configManager.getProduct(name);
        if (product == null) {
            TextUtil.sendMessage(player, "product_not_found", plugin, Map.of("{product}", name));
            return;
        }

        new ProductCreatorMenu(plugin, configManager, name, product).open(player);
    }

    @ExecuteCommand(subCommand = "remove", usage = "/zymis remove <nazwa>")
    public void remove(Player player, @Arg("nazwa") String name) {
        if (configManager.getProduct(name) == null) {
            TextUtil.sendMessage(player, "product_not_found", plugin, Map.of("{product}", name));
            return;
        }

        configManager.removeProduct(name);
        TextUtil.sendMessage(player, "product_removed", plugin, Map.of("{product}", name));
    }

    @ExecuteCommand(subCommand = "give", usage = "/zymis give <nazwa_produktu> <ilosc> <gracz>")
    public void give(Player player, @Arg("nazwa_produktu") String name, @Arg("ilosc") int amount, @Arg("gracz") Player target) {
        ItemStack product = configManager.getProduct(name);
        if (product == null) {
            TextUtil.sendMessage(player, "product_not_found", plugin, Map.of("{product}", name));
            return;
        }

        if (target == null) {
            TextUtil.sendMessage(player, "player_not_found", plugin);
            return;
        }

        ItemStack clone = product.clone();
        clone.setAmount(amount);

        claimManager.addClaim(target.getUniqueId(), clone);

        TextUtil.sendMessage(player, "gave_product", plugin, Map.of(
                "{product}", name,
                "{amount}", String.valueOf(amount),
                "{player}", target.getName()
        ));
    }
}
