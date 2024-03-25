package net.aniby.aura.gamemaster.event.abyss;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import lombok.SneakyThrows;
import net.aniby.aura.core.CoreConfig;
import net.aniby.aura.gamemaster.AuraGameMaster;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("abyss|ab")
public class AbyssCommand extends BaseCommand {
    @Subcommand("punish")
    @CommandPermission("aura.event.abyss.punish")
    @Description("Punish player to as inviter's god")
    public void punish(CommandSender sender, OnlinePlayer onlinePlayer, OnlinePlayer onlineInviter) {
        AbyssManager manager = AuraGameMaster.getAbyss();
        FileConfiguration config = manager.getConfig();
        List<String> abyss = config.getStringList("cult.abyss");
        List<String> samovar = config.getStringList("cult.samovar");

        Player inviter = onlineInviter.getPlayer();
        String inviterName = inviter.getName();

        Player player = onlinePlayer.getPlayer();
        String playerName = player.getName();

        ArrayList<TagResolver.Single> placeholders = new ArrayList<>(List.of(
                Placeholder.unparsed("inviter_name", inviterName),
                Placeholder.unparsed("player_name", playerName)
        ));

        if (abyss.contains(inviterName)) {
            placeholders.add(Placeholder.component("cult_name",
                    CoreConfig.getMessage(config, "abyss_cult"))
            );
            manager.minusAbyss(player);
        } else if (samovar.contains(inviterName)) {
            placeholders.add(Placeholder.component("cult_name",
                    CoreConfig.getMessage(config, "samovar_cult"))
            );
            manager.minusSamovar(player);
        } else {
            sender.sendMessage(CoreConfig.getMessage(config, "player_not_in_cult"));
            return;
        }

        TagResolver.Single[] resolvers = placeholders.toArray(new TagResolver.Single[0]);
        sender.sendMessage(CoreConfig.getMessage(config, "side_punish_sender", resolvers));
        player.sendMessage(CoreConfig.getMessage(config, "side_punish_player", resolvers));
        inviter.sendMessage(CoreConfig.getMessage(config,"side_punish_inviter", resolvers));
    }



    @SneakyThrows
    @Subcommand("side")
    @CommandPermission("aura.event.abyss.side")
    @Description("Set side for player to inviter's")
    public void side(CommandSender sender, OnlinePlayer onlinePlayer, OnlinePlayer onlineInviter) {
        AbyssManager manager = AuraGameMaster.getAbyss();
        FileConfiguration config = manager.getConfig();
        List<String> abyss = config.getStringList("cult.abyss");
        List<String> samovar = config.getStringList("cult.samovar");

        Player player = onlinePlayer.getPlayer();
        Player inviter = onlineInviter.getPlayer();

        String playerName = player.getName();
        if (abyss.contains(playerName) || samovar.contains(playerName)) {
            sender.sendMessage(CoreConfig.getMessage(config, "player_already_in_cult"));
            return;
        }
        String inviterName = inviter.getName();

        ArrayList<TagResolver.Single> placeholders = new ArrayList<>(List.of(
                Placeholder.unparsed("inviter_name", inviterName),
                Placeholder.unparsed("player_name", playerName)
        ));

        if (abyss.contains(inviterName)) {
            abyss.add(playerName);
            config.set("cult.abyss", abyss);
            config.save(manager.getConfigFile());

            placeholders.add(Placeholder.component("cult_name",
                    CoreConfig.getMessage(config, "abyss_cult"))
            );

            manager.plusAbyss(player);
        } else if (samovar.contains(inviterName)) {
            samovar.add(playerName);
            config.set("cult.samovar", samovar);
            config.save(manager.getConfigFile());

            placeholders.add(Placeholder.component("cult_name",
                    CoreConfig.getMessage(config, "samovar_cult"))
            );

            manager.plusSamovar(player);
        } else {
            sender.sendMessage(CoreConfig.getMessage(config, "player_not_in_cult"));
            return;
        }

        TagResolver.Single[] resolvers = placeholders.toArray(new TagResolver.Single[0]);
        sender.sendMessage(CoreConfig.getMessage(config, "side_set_sender", resolvers));
        player.sendMessage(CoreConfig.getMessage(config, "side_set_player", resolvers));
        inviter.sendMessage(CoreConfig.getMessage(config, "side_set_inviter", resolvers));
    }

    @Subcommand("info")
    @CommandPermission("aura.event.abyss.info")
    @Description("Shows cult info about player")
    public void info(Player player, @Optional OnlinePlayer onlinePlayer) {
        FileConfiguration config = AuraGameMaster.getAbyss().getConfig();
        List<String> abyss = config.getStringList("cult.abyss");
        List<String> samovar = config.getStringList("cult.samovar");

        Player target = player;
        if (onlinePlayer != null) {
            if (!player.hasPermission("aura.event.abyss.info.other")) {
                player.sendMessage(CoreConfig.getMessage("no_permission"));
                return;
            }
            target = onlinePlayer.getPlayer();
        }

        Component side;
        String targetName = target.getName();
        if (abyss.contains(targetName))
            side = CoreConfig.getMessage(config, "abyss_cult");
        else if (samovar.contains(targetName))
            side = CoreConfig.getMessage(config, "samovar_cult");
        else
            side = CoreConfig.getMessage(config, "no_cult");

        player.sendMessage(CoreConfig.getMessage(config, "cult_list", Placeholder.component(
                "cult_name",
                side
        ), Placeholder.unparsed(
                "target_name",
                targetName
        )));
    }

    @Subcommand("list")
    @CommandPermission("aura.event.abyss.list")
    @Description("Shows player list of side")
    public void list(Player player, @Optional String side) {
        FileConfiguration config = AuraGameMaster.getAbyss().getConfig();
        List<String> abyss = config.getStringList("cult.abyss");
        List<String> samovar = config.getStringList("cult.samovar");

        String playerName = player.getName();

        if (side != null) {
            if (!player.hasPermission("aura.event.abyss.list.other")) {
                player.sendMessage(CoreConfig.getMessage("no_permission"));
                return;
            }
        } else {
            if (abyss.contains(playerName))
                side = "abyss";
            else if (samovar.contains(playerName))
                side = "samovar";
            else {
                player.sendMessage(CoreConfig.getMessage(config, "not_in_cult"));
                return;
            }
        }

        String name;
        String list;
        switch (side) {
            case "abyss" -> {
                name = CoreConfig.getPlainMessage(config, "abyss_cult");
                list = String.join(", ", abyss);
            }
            case "samovar" -> {
                name = CoreConfig.getPlainMessage(config, "samovar_cult");
                list = String.join(", ", samovar);
            }
            default -> {
                player.sendMessage(AuraGameMaster.getMessage("invalid_arguments"));
                return;
            }
        }

        player.sendMessage(CoreConfig.getMessage(config, "cult_list", Placeholder.component(
                "cult_name",
                CoreConfig.getMiniMessage().deserialize(name)
        ), Placeholder.unparsed(
                "cult_list",
                list
        )));
    }
}
