package net.aniby.aura.core.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.aniby.aura.core.AuraCore;
import net.aniby.aura.core.CoreConfig;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.tool.AuraUtils;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("aura")
public class AuraCommand extends BaseCommand {
    @Subcommand("give")
    @CommandPermission("aura.core.aura.give")
    @Syntax("[player] [aura]")
    public void give(CommandSender sender, OnlinePlayer onlinePlayer, double amount) {
        Player player = onlinePlayer.getPlayer();
        String playerName = player.getName();

        // Database
        UserRepository repository = AuraCore.getInstance().getUserRepository();
        AuraUser user = repository.findByPlayerName(playerName);
        if (user == null) {
            sender.sendMessage(CoreConfig.getMessage(
                    "database_error"
            ));
            return;
        }
        user.setAura(user.getAura() + amount);
        repository.update(user);

        // Message
        TagResolver.Single[] resolvers = new TagResolver.Single[]{
                Placeholder.unparsed("sender_name", sender.getName()),
                Placeholder.unparsed("player_name", playerName),
                Placeholder.unparsed("aura", String.valueOf(AuraUtils.roundDouble(amount)))
        };
        player.sendMessage(CoreConfig.getMessage("aura_give_target", resolvers));
        sender.sendMessage(CoreConfig.getMessage("aura_give_sender", resolvers));
    }

    @Default
    public void execute(Player source, @Optional OnlinePlayer onlineTarget) {
        if (!CommandFixer.checkCooldown(source))
            return;

        if (onlineTarget != null) {
            if (!source.hasPermission("aura.core.aura.other")) {
                source.sendMessage(CoreConfig.getMessage(
                        "no_permission"
                ));
                return;
            }
        }

        AuraUser user = AuraCore.getInstance().getUserRepository()
                .findByPlayerName(source.getName());
        if (user == null) {
            source.sendMessage(CoreConfig.getMessage(
                    "database_error"
            ));
            return;
        }

        source.sendMessage(CoreConfig.getMessage(
                "aura_command", Placeholder.unparsed("aura", String.valueOf(user.getFormattedAura()))
        ));
    }
}
