package net.aniby.aura.velocity.event;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.SneakyThrows;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.velocity.AuraVelocity;
import net.aniby.aura.velocity.VelocityConfig;

public class ConnectListener {
    @SneakyThrows
    @Subscribe(order = PostOrder.FIRST)
    void onPlayerJoin(ServerPreConnectEvent event) {
        RegisteredServer server = event.getPreviousServer();
        Player player = event.getPlayer();
        AuraVelocity instance = AuraVelocity.getInstance();
        VelocityConfig config = instance.getConfig();
        if (server == null) {
            AuraDatabase database = AuraVelocity.getInstance().getDatabase();
            AuraUser user = database.getUsers().queryBuilder()
                    .where()
                    .eq("whitelisted", true)
                    .and()
                    .eq("player_name", player.getUsername())
                    .queryForFirst();
            if (user == null) {
                player.disconnect(config.getMessage("not_in_whitelist"));
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
                return;
            }
        }
    }
}
