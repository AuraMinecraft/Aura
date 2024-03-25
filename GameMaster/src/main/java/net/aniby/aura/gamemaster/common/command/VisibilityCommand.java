package net.aniby.aura.gamemaster.common.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.aniby.aura.gamemaster.AuraGameMaster;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

@CommandAlias("visibility")
public class VisibilityCommand extends BaseCommand {
    @Subcommand("hide")
    @CommandPermission("aura.gamemaster.visibility")
    @Description("Hide from player")
    public void hide(Player player, @Optional OnlinePlayer target) {
        Collection<? extends Player> players = target == null ? Bukkit.getOnlinePlayers() : List.of(target.getPlayer());
        TagResolver.Single placeholder = target == null ? Placeholder.unparsed("target", "All") : Placeholder.unparsed("target", target.getPlayer().getName());

        players.forEach(p -> p.hidePlayer(AuraGameMaster.getInstance(), player));

        player.sendMessage(AuraGameMaster.getMessage("visibility_hide", placeholder));
    }

    @Subcommand("show")
    @CommandPermission("aura.gamemaster.visibility")
    @Description("Show to player")
    public void show(Player player, @Optional OnlinePlayer target) {
        Collection<? extends Player> players = target == null ? Bukkit.getOnlinePlayers() : List.of(target.getPlayer());
        TagResolver.Single placeholder = target == null ? Placeholder.unparsed("target", "All") : Placeholder.unparsed("target", target.getPlayer().getName());

        players.forEach(p -> p.showPlayer(AuraGameMaster.getInstance(), player));

        player.sendMessage(AuraGameMaster.getMessage("visibility_show", placeholder));
    }


}
