package pl.sunglasses.zymis;

import org.bukkit.plugin.java.JavaPlugin;
import pl.sunglasses.zymis.util.CommandUtil;
import pl.sunglasses.zymis.command.OdbierzCommand;
import pl.sunglasses.zymis.command.ZymisCommand;
import pl.sunglasses.zymis.config.ConfigManager;
import pl.sunglasses.zymis.listener.PlayerJoinListener;
import pl.sunglasses.zymis.manager.BossBarManager;
import pl.sunglasses.zymis.manager.ClaimManager;

public final class ZymItemshopPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ClaimManager claimManager;
    private BossBarManager bossBarManager;
    private CommandUtil commandUtil;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        this.configManager = new ConfigManager(this);
        this.bossBarManager = new BossBarManager(this);
        this.claimManager = new ClaimManager(this, configManager, bossBarManager);

        this.commandUtil = new CommandUtil(this);
        this.commandUtil.registerCommand(new ZymisCommand(this, configManager, claimManager));
        this.commandUtil.registerCommand(new OdbierzCommand(this, claimManager, configManager));

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(claimManager), this);
    }

    @Override
    public void onDisable() {
        if (claimManager != null) {
            claimManager.saveAll();
        }
        if (bossBarManager != null) {
            bossBarManager.removeAll();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }
}
