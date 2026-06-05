package pl.sunglasses.zymis.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.sunglasses.zymis.ZymItemshopPlugin;

import java.util.Map;

public class TextUtil {

    public static Component fix(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty().decoration(TextDecoration.ITALIC, false);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public static void sendMessage(Player player, String key, ZymItemshopPlugin plugin) {
        sendMessage(player, key, plugin, null);
    }

    public static void sendMessage(Player player, String key, ZymItemshopPlugin plugin, Map<String, String> placeholders) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("messages." + key);
        if (section == null) {
            String legacy = plugin.getConfig().getString("messages." + key);
            if (legacy != null) player.sendMessage(fix(applyPlaceholders(legacy, placeholders)));
            return;
        }

        String type = section.getString("type", "CHAT").toUpperCase();
        String text = section.getString("text", "");
        if (text.isEmpty() && !type.equals("TITLE-SUBTITLE")) return;

        switch (type) {
            case "BOSSBAR":
                plugin.getBossBarManager().showTemporaryBossBar(player, section, placeholders);
                break;
            case "TITLE":
                player.showTitle(Title.title(fix(applyPlaceholders(text, placeholders)), Component.empty()));
                break;
            case "SUBTITLE":
                player.showTitle(Title.title(Component.empty(), fix(applyPlaceholders(text, placeholders))));
                break;
            case "TITLE-SUBTITLE":
                player.showTitle(Title.title(
                        fix(applyPlaceholders(section.getString("title", text), placeholders)),
                        fix(applyPlaceholders(section.getString("subtitle", ""), placeholders))
                ));
                break;
            case "ACTIONBAR":
                player.sendActionBar(fix(applyPlaceholders(text, placeholders)));
                break;
            case "CHAT":
            default:
                player.sendMessage(fix(applyPlaceholders(text, placeholders)));
                break;
        }
    }

    private static String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (placeholders == null || text == null) return text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }
}
