package pl.sunglasses.zymis.manager;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.sunglasses.zymis.util.TextUtil;
import pl.sunglasses.zymis.ZymItemshopPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {

    private final ZymItemshopPlugin plugin;
    private final Map<UUID, BossBar> persistentBars = new HashMap<>();

    public BossBarManager(ZymItemshopPlugin plugin) {
        this.plugin = plugin;
    }

    public void showPersistentBossBar(Player player) {
        if (persistentBars.containsKey(player.getUniqueId())) return;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("messages.items_waiting");
        if (section == null || !"BOSSBAR".equalsIgnoreCase(section.getString("type"))) return;

        BossBar bar = createBossBar(section, null);
        player.showBossBar(bar);
        persistentBars.put(player.getUniqueId(), bar);
    }

    public void hidePersistentBossBar(Player player) {
        BossBar bar = persistentBars.remove(player.getUniqueId());
        if (bar != null) player.hideBossBar(bar);
    }

    public void showTemporaryBossBar(Player player, ConfigurationSection section, Map<String, String> placeholders) {
        BossBar bar = createBossBar(section, placeholders);
        player.showBossBar(bar);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.hideBossBar(bar), 100L);
    }

    private BossBar createBossBar(ConfigurationSection section, Map<String, String> placeholders) {
        String text = section.getString("text", section.getString("title", ""));
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                text = text.replace(entry.getKey(), entry.getValue());
            }
        }

        BossBar.Color color;
        try {
            color = BossBar.Color.valueOf(section.getString("color", "GREEN").toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BossBar.Color.GREEN;
        }

        BossBar.Overlay overlay;
        try {
            overlay = BossBar.Overlay.valueOf(section.getString("style", "PROGRESS").toUpperCase());
        } catch (IllegalArgumentException e) {
            overlay = BossBar.Overlay.PROGRESS;
        }

        return BossBar.bossBar(TextUtil.fix(text), 1.0f, color, overlay);
    }

    public void removeAll() {
        persistentBars.forEach((uuid, bar) -> {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) p.hideBossBar(bar);
        });
        persistentBars.clear();
    }
}
