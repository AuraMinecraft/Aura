package net.aniby.aura.gamemaster.event.abyss;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import lombok.SneakyThrows;
import net.aniby.aura.core.CoreConfig;
import net.aniby.aura.gamemaster.AuraGameMaster;
import net.aniby.aura.gamemaster.CustomConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CommandAlias("abyss|ab")
public class AbyssCommand extends BaseCommand {
    @SneakyThrows
    @Subcommand("answer")
    @Description("Answer to cult invite")
    public void answer(Player player, @NotNull String answer) {
        AbyssManager manager = AuraGameMaster.getAbyss();
        CustomConfig customConfig = manager.getConfig();
        FileConfiguration config = customConfig.getConfiguration();

        List<String> abyss = config.getStringList("cult.abyss");
        List<String> samovar = config.getStringList("cult.samovar");

        String playerName = player.getName();
        if (abyss.contains(playerName) || samovar.contains(playerName)) {
            player.sendMessage(CoreConfig.getMessage(config, "you_in_cult"));
            return;
        }
        if (!manager.getInvites().containsKey(player)) {
            player.sendMessage(CoreConfig.getMessage(config, "no_invite"));
            return;
        }
        Player inviter = manager.getInvites().get(player);
        if (!inviter.isOnline()) {
            player.sendMessage(CoreConfig.getMessage(config, "player_is_offline"));
            return;
        }


        String inviterName = inviter.getName();

        ArrayList<TagResolver.Single> placeholders = new ArrayList<>(List.of(
                Placeholder.unparsed("inviter_name", inviterName),
                Placeholder.unparsed("player_name", playerName)
        ));

        boolean accepted;
        if (Objects.equals("yes", answer))
            accepted = true;
        else if (Objects.equals("no", answer))
            accepted = false;
        else {
            player.sendMessage(CoreConfig.getMessage("invalid_arguments"));
            return;
        }

        if (abyss.contains(inviterName)) {
            placeholders.add(Placeholder.component("cult_name",
                    CoreConfig.getMessage(config, "abyss_cult"))
            );
            if (accepted) {
                manager.plusAbyss(player);

                abyss.add(playerName);
                config.set("cult.abyss", abyss);
                customConfig.save();
            }
            else
                manager.minusAbyss(player);
        } else if (samovar.contains(inviterName)) {
            placeholders.add(Placeholder.component("cult_name",
                    CoreConfig.getMessage(config, "samovar_cult"))
            );
            if (accepted) {
                manager.plusSamovar(player);

                samovar.add(playerName);
                config.set("cult.samovar", samovar);
                customConfig.save();
            }
            else {
                manager.minusSamovar(player);
            }
        } else {
            player.sendMessage(CoreConfig.getMessage(config, "player_not_in_cult"));
            return;
        }

        manager.getInvites().remove(player);

        TagResolver.Single[] resolvers = placeholders.toArray(new TagResolver.Single[0]);

        if (accepted) {
            player.sendMessage(CoreConfig.getMessage(config, "side_set_player", resolvers));
            inviter.sendMessage(CoreConfig.getMessage(config, "side_set_inviter", resolvers));
        } else {
            player.sendMessage(CoreConfig.getMessage(config, "side_punish_player", resolvers));
            inviter.sendMessage(CoreConfig.getMessage(config, "side_punish_inviter", resolvers));
        }
    }

    @Subcommand("invite")
    @CommandPermission("aura.event.abyss.invite")
    @Description("Invites player to inviter's cult side")
    public void invite(Player inviter, OnlinePlayer onlinePlayer) {
        AbyssManager manager = AuraGameMaster.getAbyss();
        FileConfiguration config = manager.getConfig().getConfiguration();
        List<String> abyss = config.getStringList("cult.abyss");
        List<String> samovar = config.getStringList("cult.samovar");

        Player player = onlinePlayer.getPlayer();

        String playerName = player.getName();
        if (abyss.contains(playerName) || samovar.contains(playerName)) {
            inviter.sendMessage(CoreConfig.getMessage(config, "player_already_in_cult"));
            return;
        }
        String inviterName = inviter.getName();

        ArrayList<TagResolver.Single> placeholders = new ArrayList<>(List.of(
                Placeholder.unparsed("inviter_name", inviterName),
                Placeholder.unparsed("player_name", playerName)
        ));

        if (abyss.contains(inviterName)) {
            placeholders.add(Placeholder.component("cult_name",
                    CoreConfig.getMessage(config, "abyss_cult"))
            );
        } else if (samovar.contains(inviterName)) {
            placeholders.add(Placeholder.component("cult_name",
                    CoreConfig.getMessage(config, "samovar_cult"))
            );
        } else {
            inviter.sendMessage(CoreConfig.getMessage(config, "you_not_in_cult"));
            return;
        }
        manager.getInvites().put(player, inviter);

        placeholders.addAll(List.of(
                Placeholder.component("invite_answer_yes", CoreConfig.getMessage(config, "answer_yes")),
                Placeholder.component("invite_answer_no", CoreConfig.getMessage(config, "answer_no"))
        ));

        TagResolver.Single[] resolvers = placeholders.toArray(new TagResolver.Single[0]);
        inviter.sendMessage(CoreConfig.getMessage(config, "invited_to_inviter", resolvers));
        player.sendMessage(CoreConfig.getMessage(config, "invited_to", resolvers));
    }




    @Subcommand("punish")
    @CommandPermission("aura.event.abyss.punish")
    @Description("Punish player to as inviter's god")
    public void punish(CommandSender sender, OnlinePlayer onlinePlayer, OnlinePlayer onlineInviter) {
        AbyssManager manager = AuraGameMaster.getAbyss();
        FileConfiguration config = manager.getConfig().getConfiguration();
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
        CustomConfig customConfig = manager.getConfig();
        FileConfiguration config = customConfig.getConfiguration();
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
            customConfig.save();

            placeholders.add(Placeholder.component("cult_name",
                    CoreConfig.getMessage(config, "abyss_cult"))
            );

            manager.plusAbyss(player);
        } else if (samovar.contains(inviterName)) {
            samovar.add(playerName);
            config.set("cult.samovar", samovar);
            customConfig.save();

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
        FileConfiguration config = AuraGameMaster.getAbyss().getConfig().getConfiguration();
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

        player.sendMessage(CoreConfig.getMessage(config, "cult_info", Placeholder.component(
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
        FileConfiguration config = AuraGameMaster.getAbyss().getConfig().getConfiguration();
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
