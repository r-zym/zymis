package pl.sunglasses.zymis.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.sunglasses.zymis.ZymItemshopPlugin;
import pl.sunglasses.zymis.config.ConfigManager;
import pl.sunglasses.zymis.util.TextUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClaimManager {

    private final ZymItemshopPlugin plugin;
    private final ConfigManager configManager;
    private final BossBarManager bossBarManager;

    private final Map<UUID, List<ItemStack>> claims = new HashMap<>();

    public ClaimManager(ZymItemshopPlugin plugin, ConfigManager configManager, BossBarManager bossBarManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.bossBarManager = bossBarManager;
        loadClaims();
    }

    private void loadClaims() {
        ConfigurationSection section = configManager.getClaimsConfig().getConfigurationSection("claims");
        if (section == null) return;

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                List<ItemStack> items = (List<ItemStack>) section.getList(uuidStr);
                if (items != null) {
                    claims.put(uuid, new ArrayList<>(items));
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveAll() {
        configManager.getClaimsConfig().set("claims", null);
        for (Map.Entry<UUID, List<ItemStack>> entry : claims.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                configManager.getClaimsConfig().set("claims." + entry.getKey().toString(), entry.getValue());
            }
        }
        configManager.saveClaimsConfig();
    }

    public void saveAllAsync() {
        YamlConfiguration snapshot = new YamlConfiguration();
        for (Map.Entry<UUID, List<ItemStack>> entry : claims.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                snapshot.set("claims." + entry.getKey().toString(), new ArrayList<>(entry.getValue()));
            }
        }
        configManager.saveClaimsConfigAsync(snapshot);
    }

    public void addClaim(UUID uuid, ItemStack item) {
        claims.computeIfAbsent(uuid, k -> new ArrayList<>()).add(item.clone());
        saveAllAsync();

        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null && player.isOnline()) {
            updateNotification(player);
        }
    }

    public List<ItemStack> getClaims(UUID uuid) {
        return claims.getOrDefault(uuid, new ArrayList<>());
    }

    public void removeClaim(UUID uuid, ItemStack item) {
        List<ItemStack> playerClaims = claims.get(uuid);
        if (playerClaims != null) {
            playerClaims.remove(item);
            saveAllAsync();

            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                updateNotification(player);
            }
        }
    }

    public void updateNotification(Player player) {
        List<ItemStack> playerClaims = claims.get(player.getUniqueId());
        if (playerClaims != null && !playerClaims.isEmpty()) {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("messages.items_waiting");
            if (section != null && "BOSSBAR".equalsIgnoreCase(section.getString("type"))) {
                bossBarManager.showPersistentBossBar(player);
            } else {
                bossBarManager.hidePersistentBossBar(player);
                if (section != null) {
                    TextUtil.sendMessage(player, "items_waiting", plugin);
                }
            }
        } else {
            bossBarManager.hidePersistentBossBar(player);
        }
    }
}
