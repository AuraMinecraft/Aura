package net.aniby.aura.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.discord.CommandHandler;
import net.aniby.aura.discord.DiscordListener;
import net.aniby.aura.discord.commands.*;
import net.aniby.aura.repository.UserRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.data.DataObject;
import ninja.leaping.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscordService extends ListenerAdapter {
    AuraConfig config;

    @Getter
    final JDA jda;
    @Getter
    Guild defaultGuild = null;
    @Getter
    final HashMap<String, Role> roles = new HashMap<>();
    @Getter
    final HashMap<String, TextChannel> channels = new HashMap<>();

    public DiscordService(AuraConfig config) {
        this.config = config;

        ConfigurationNode node = config.getRoot().getNode("discord");
        this.jda = JDABuilder.createDefault(
                node.getNode("bot_token").getString()
        ).setActivity(Activity.of(Activity.ActivityType.STREAMING, "aura.aniby.net"))
                .addEventListeners(new DiscordListener(), this)
                .build();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        ConfigurationNode root = config.getRoot();
        ConfigurationNode node = root.getNode("discord");

        // Guild
        defaultGuild = this.jda.getGuildById(
                node.getNode("default_guild").getString()
        );

        // Roles
        ConfigurationNode rolesNode = node.getNode("roles");
        for (String roleName : AuraConfig.getNodeKeys(rolesNode)) {
            roles.put(roleName, defaultGuild.getRoleById(
                    rolesNode.getNode(roleName).getString()
            ));
        }
        // Channels
        ConfigurationNode channelsNode = node.getNode("channels");
        for (String channelName : AuraConfig.getNodeKeys(channelsNode)) {
            channels.put(channelName, defaultGuild.getTextChannelById(
                    channelsNode.getNode(channelName).getString()
            ));
        }
    }
}
