package pl.sunglasses.zymis.menu;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.sunglasses.zymis.util.TextUtil;
import pl.sunglasses.zymis.ZymItemshopPlugin;
import pl.sunglasses.zymis.manager.ClaimManager;

import java.util.HashMap;
import java.util.List;

public class ClaimMenu {

    private final ZymItemshopPlugin plugin;
    private final ClaimManager claimManager;

    public ClaimMenu(ZymItemshopPlugin plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
    }

    public void open(Player player) {
        ConfigurationSection menuSection = plugin.getConfig().getConfigurationSection("menus.claim");
        
        String titleStr = menuSection != null ? menuSection.getString("title", "&8Odbierz przedmioty") : "&8Odbierz przedmioty";
        int rows = menuSection != null ? menuSection.getInt("rows", 6) : 6;

        Gui gui = Gui.gui()
                .title(TextUtil.fix(titleStr))
                .rows(rows)
                .disableAllInteractions()
                .create();

        List<ItemStack> itemsToClaim = claimManager.getClaims(player.getUniqueId());

        for (ItemStack item : itemsToClaim) {
            GuiItem guiItem = new GuiItem(item, event -> {
                event.setCancelled(true);
                
                HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(item);
                if (!leftOvers.isEmpty()) {
                    TextUtil.sendMessage(player, "inventory_full", plugin);
                    return;
                }

                claimManager.removeClaim(player.getUniqueId(), item);
                TextUtil.sendMessage(player, "claimed_item", plugin);
                
                gui.close(player);
                open(player);
            });
            gui.addItem(guiItem);
        }

        if (menuSection != null && menuSection.contains("filler")) {
            ConfigurationSection fillerSection = menuSection.getConfigurationSection("filler");
            ItemStack fillerItem = new ItemStack(Material.valueOf(fillerSection.getString("material", "LIGHT_GRAY_STAINED_GLASS_PANE")));
            ItemMeta fillerMeta = fillerItem.getItemMeta();
            if (fillerMeta != null) {
                fillerMeta.displayName(TextUtil.fix(fillerSection.getString("name", " ")));
                fillerItem.setItemMeta(fillerMeta);
            }
            gui.getFiller().fill(new GuiItem(fillerItem));
        }

        gui.open(player);
    }
}
