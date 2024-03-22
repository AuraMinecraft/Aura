package net.aniby.aura.paper.votes.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.core.AuraCore;
import net.aniby.aura.paper.votes.AuraVotes;
import net.aniby.aura.paper.votes.logic.AuraCollection;
import net.aniby.aura.repository.UserRepository;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Date;

@CommandAlias("collection")
public class CollectionCommand extends BaseCommand {
    @Subcommand("create")
    @CommandPermission("aura.votes.collection.create")
    public void create(CommandSender sender, AuraCollection.Action action, double cost, long minutes) {
        String admin = sender.getName();

        ArrayList<TagResolver> resolvers = new ArrayList<>();
        resolvers.add(Placeholder.unparsed("admin", admin));

        AuraCollection collection = AuraVotes.getInstance().getActiveCollection();
        if (collection != null) {
            resolvers.addAll(collection.getResolvers());
            sender.sendMessage(
                    AuraVotes.getMessage("collection_exists", resolvers)
            );
            return;
        }

        long expiresAt = new Date().getTime() + minutes * 60 * 1000L;
        collection = new AuraCollection(action, cost, expiresAt);
        AuraVotes.getInstance().setActiveCollection(collection);

        resolvers.addAll(collection.getResolvers());
        sender.sendMessage(
                AuraVotes.getMessage("collection_create_command", resolvers)
        );
        Bukkit.broadcast(
                AuraVotes.getMessage("collection_create", resolvers)
        );
    }
    
    @Subcommand("end")
    @CommandPermission("aura.votes.collection.end")
    public void end(CommandSender sender) {
        AuraCollection collection = AuraVotes.getInstance().getActiveCollection();
        if (collection == null) {
            sender.sendMessage(
                    AuraVotes.getMessage("collection_not_exists")
            );
            return;
        }
        
        collection.end(sender.getName());
    }

    @Subcommand("deposit")
    public void deposit(Player player, double deposit) {
        if (deposit <= 0) {
            player.sendMessage(
                    AuraVotes.getMessage("invalid_aura")
            );
            return;
        }

        AuraCollection collection = AuraVotes.getInstance().getActiveCollection();
        if (collection == null) {
            player.sendMessage(
                    AuraVotes.getMessage("collection_not_exists")
            );
            return;
        }

        UserRepository repository = AuraCore.getInstance().getUserRepository();;
        String name = player.getName();
        AuraUser user = repository.findByPlayerName(name);
        if (user == null) {
            player.sendMessage(
                    AuraVotes.getMessage("database_error")
            );
            return;
        }
        double auraLeft = user.getAura() - deposit;
        if (auraLeft < 0) {
            player.sendMessage(
                    AuraVotes.getMessage("not_enough_aura")
            );
            return;
        }

        collection.collect(name, deposit);
        user.setAura(auraLeft);
        repository.update(user);

        ArrayList<TagResolver> resolvers = collection.getResolvers();
        resolvers.add(Placeholder.unparsed("deposit_aura", String.valueOf(deposit)));

        player.sendMessage(
                AuraVotes.getMessage("collection_deposit", resolvers)
        );
    }
}
