package net.aniby.aura.core.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import net.aniby.aura.core.AuraCore;
import net.aniby.aura.core.CommandFixer;
import net.aniby.aura.core.CoreConfig;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.repository.TransactionRepository;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.tool.DiscordWebhook;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("pay")
public class PayCommand extends BaseCommand {
    @Default
    @CommandCompletion("@players @range:1-20")
    public void execute(Player source, String receiverPlayerName, double amount, @Optional String comment) {
        if (!CommandFixer.checkCooldown(source))
            return;

        UserRepository users = AuraCore.getInstance().getUserRepository();
        TransactionRepository transactions = AuraCore.getInstance().getTransactionRepository();

        AuraUser sender = users.findByPlayerName(source.getName());
        if (sender == null) {
            source.sendMessage(CoreConfig.getMessage(
                    "database_error"
            ));
            return;
        }

        if (sender.getAura() < amount) {
            source.sendMessage(CoreConfig.getMessage(
                    "not_enough_aura"
            ));
            return;
        }

        AuraUser receiver = users.findByWhitelistedPlayerName(receiverPlayerName);
        if (receiver == null) {
            source.sendMessage(CoreConfig.getMessage(
                    "database_error"
            ));
            return;
        }

        TagResolver.Single[] resolvers = new TagResolver.Single[]{
                Placeholder.unparsed("sender", sender.getPlayerName()),
                Placeholder.unparsed("receiver", receiverPlayerName),
                Placeholder.unparsed("aura", String.valueOf(amount)),
                Placeholder.unparsed("comment", comment == null ? "" : comment)
        };

        transactions.create(sender, receiver, amount, comment);
        DiscordWebhook webhook = new DiscordWebhook(
                AuraCore.getInstance().getConfig().getString("webhook.transactions")
        );
        webhook.setContent("");
        webhook.addEmbed(
                new DiscordWebhook.EmbedObject()
        );

        sender.setAura(sender.getAura() - amount);
        source.sendMessage(CoreConfig.getMessage(
                "pay_sender",
                resolvers
        ));
        receiver.setAura(receiver.getAura() + amount);
        Player receiverPlayer = Bukkit.getPlayer(receiverPlayerName);
        if (receiverPlayer != null && receiverPlayer.isOnline()) {
            receiverPlayer.sendMessage(CoreConfig.getMessage(
                    "pay_receiver",
                    resolvers
            ));
            if (comment != null && !comment.isEmpty())
                receiverPlayer.sendMessage(CoreConfig.getMessage(
                        "pay_comment",
                        resolvers
                ));
        }
    }
}
