package pl.sunglasses.zymis.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import pl.sunglasses.zymis.ZymItemshopPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final ZymItemshopPlugin plugin;
    private File claimsFile;
    private FileConfiguration claimsConfig;

    public ConfigManager(ZymItemshopPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        loadClaimsConfig();
    }

    private void loadClaimsConfig() {
        claimsFile = new File(plugin.getDataFolder(), "claims.yml");
        if (!claimsFile.exists()) {
            try {
                claimsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        claimsConfig = YamlConfiguration.loadConfiguration(claimsFile);
    }

    public FileConfiguration getClaimsConfig() {
        return claimsConfig;
    }

    public void saveClaimsConfig() {
        try {
            claimsConfig.save(claimsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveClaimsConfigAsync(FileConfiguration configSnapshot) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                configSnapshot.save(claimsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void saveProduct(String name, ItemStack item) {
        plugin.getConfig().set("products." + name, item);
        plugin.saveConfig();
    }

    public ItemStack getProduct(String name) {
        return plugin.getConfig().getItemStack("products." + name);
    }

    public void removeProduct(String name) {
        plugin.getConfig().set("products." + name, null);
        plugin.saveConfig();
    }

    public List<String> getProducts() {
        if (plugin.getConfig().getConfigurationSection("products") == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(plugin.getConfig().getConfigurationSection("products").getKeys(false));
    }
}
