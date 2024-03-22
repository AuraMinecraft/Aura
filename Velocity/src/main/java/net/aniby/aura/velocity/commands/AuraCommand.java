package net.aniby.aura.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.velocity.AuraVelocity;
import net.aniby.aura.velocity.VelocityConfig;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuraCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!AuraVelocity.getInstance().checkCooldown(source))
            return;

        VelocityConfig config = AuraVelocity.getInstance().getConfig();
        if (!(source instanceof Player player)) {
            source.sendMessage(config.getMessage(
                    "invalid_executor"
            ));
            return;
        }

        AuraUser user = AuraVelocity.getInstance().getUserRepository()
                .findByPlayerName(player.getUsername());
        if (user == null) {
            source.sendMessage(config.getMessage(
                    "database_error"
            ));
            return;
        }
        source.sendMessage(config.getMessage(
                "aura_command", Placeholder.unparsed("aura", String.valueOf(user.getFormattedAura()))
        ));
        return;
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        ArrayList<String> list = new ArrayList<>();
        return CompletableFuture.completedFuture(list);
    }
}
