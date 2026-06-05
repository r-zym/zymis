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
import pl.sunglasses.zymis.config.ConfigManager;

import java.util.Map;

public class ProductCreatorMenu {

    private final ZymItemshopPlugin plugin;
    private final ConfigManager configManager;
    private final String productName;
    private final ItemStack currentItem;

    public ProductCreatorMenu(ZymItemshopPlugin plugin, ConfigManager configManager, String productName, ItemStack currentItem) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.productName = productName;
        this.currentItem = currentItem;
    }

    public void open(Player player) {
        ConfigurationSection menuSection = plugin.getConfig().getConfigurationSection("menus.creator");
        String titleStr = menuSection.getString("title", "&8Kreator: {product}").replace("{product}", productName);
        
        Gui gui = Gui.gui()
                .type(dev.triumphteam.gui.components.GuiType.HOPPER)
                .title(TextUtil.fix(titleStr))
                .create();

        gui.setOutsideClickAction(event -> event.setCancelled(true));
        
        gui.setDragAction(event -> {
            if (event.getRawSlots().stream().anyMatch(slot -> slot < 5 && slot != 2)) {
                event.setCancelled(true);
            }
        });

        ConfigurationSection cancelSection = menuSection.getConfigurationSection("items.cancel");
        if (cancelSection != null) {
            ItemStack cancelItem = new ItemStack(Material.valueOf(cancelSection.getString("material", "BARRIER")));
            ItemMeta cancelMeta = cancelItem.getItemMeta();
            if (cancelMeta != null) {
                cancelMeta.displayName(TextUtil.fix(cancelSection.getString("name", "&cAnuluj")));
                cancelItem.setItemMeta(cancelMeta);
            }
            gui.setItem(0, new GuiItem(cancelItem, event -> {
                event.setCancelled(true);
                player.closeInventory();
            }));
        }

        ConfigurationSection fillerSection = menuSection.getConfigurationSection("items.filler");
        if (fillerSection != null) {
            ItemStack fillerItem = new ItemStack(Material.valueOf(fillerSection.getString("material", "GRAY_STAINED_GLASS_PANE")));
            ItemMeta fillerMeta = fillerItem.getItemMeta();
            if (fillerMeta != null) {
                fillerMeta.displayName(TextUtil.fix(fillerSection.getString("name", " ")));
                fillerItem.setItemMeta(fillerMeta);
            }
            GuiItem fillerGuiItem = new GuiItem(fillerItem, event -> event.setCancelled(true));
            for (int slot : fillerSection.getIntegerList("slots")) {
                gui.setItem(slot, fillerGuiItem);
            }
        }

        ConfigurationSection confirmSection = menuSection.getConfigurationSection("items.confirm");
        if (confirmSection != null) {
            ItemStack confirmItem = new ItemStack(Material.valueOf(confirmSection.getString("material", "OAK_BUTTON")));
            ItemMeta confirmMeta = confirmItem.getItemMeta();
            if (confirmMeta != null) {
                confirmMeta.displayName(TextUtil.fix(confirmSection.getString("name", "&aZatwierdź")));
                confirmItem.setItemMeta(confirmMeta);
            }
            gui.setItem(4, new GuiItem(confirmItem, event -> {
                event.setCancelled(true);
                ItemStack itemInSlot = gui.getInventory().getItem(2);
                if (itemInSlot == null || itemInSlot.getType() == Material.AIR) {
                    TextUtil.sendMessage(player, "must_place_item", plugin);
                    return;
                }
                
                ItemStack savedItem = itemInSlot.clone();
                configManager.saveProduct(productName, savedItem);
                
                player.closeInventory();
                player.getInventory().addItem(itemInSlot);
                
                Map<String, String> placeholders = Map.of("{product}", productName);
                if (currentItem == null) {
                    TextUtil.sendMessage(player, "product_created", plugin, placeholders);
                } else {
                    TextUtil.sendMessage(player, "product_edited", plugin, placeholders);
                }
            }));
        }

        if (currentItem != null) {
            gui.setItem(2, new GuiItem(currentItem, event -> event.setCancelled(false)));
        }

        gui.setDefaultClickAction(event -> {
            if (event.getRawSlot() == 2) {
                event.setCancelled(false);
            } else if (event.getRawSlot() < 5) {
                event.setCancelled(true);
            } else {
                event.setCancelled(event.isShiftClick());
            }
        });

        gui.open(player);
    }
}
