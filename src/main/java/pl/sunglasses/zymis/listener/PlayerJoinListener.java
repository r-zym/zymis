package pl.sunglasses.zymis.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import pl.sunglasses.zymis.manager.ClaimManager;

public class PlayerJoinListener implements Listener {

    private final ClaimManager claimManager;

    public PlayerJoinListener(ClaimManager claimManager) {
        this.claimManager = claimManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        claimManager.updateNotification(event.getPlayer());
    }
}
