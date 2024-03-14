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
    DiscordFormService discordFormService;

    @Getter
    final JDA jda;
    @Getter
    Guild defaultGuild = null;
    @Getter
    final HashMap<String, Role> roles = new HashMap<>();
    @Getter
    final HashMap<String, TextChannel> channels = new HashMap<>();
    @Getter
    final CommandHandler handler;

    public DiscordService(AuraConfig config, UserService userService, UserRepository userRepository, TwitchService twitchService, YooMoneyService yooMoneyService, DiscordFormService discordFormService) {
        this.config = config;

        ConfigurationNode node = config.getRoot().getNode("discord");
        this.jda = JDABuilder.createDefault(
                node.getNode("bot_token").getString()
        ).setActivity(Activity.of(Activity.ActivityType.STREAMING, "aura.aniby.net"))
                .addEventListeners(new DiscordListener(), this)
                .build();

        this.discordFormService = discordFormService;

        this.handler = new CommandHandler(this.jda);
        this.handler.registerCommands(
                new AuraCommand(config, userService),
                new LinkCommand(config, userService, userRepository, twitchService),
                new ForceLinkCommand(config, userService, userRepository),
                new UnlinkCommand(config, userService, userRepository, twitchService),
                new AuraLinkCommand(config),
                new ProfileCommand(config, userService, userRepository),
                new DonateCommand(config, userService, userRepository, yooMoneyService)
        );
        handler.confirm();
        handler.registerButton(discordFormService.FORM_CREATE, discordFormService::buttonFormCreate);
        handler.registerModal(discordFormService.FORM_SUBMIT, discordFormService::modalFormSubmit);
        handler.registerButton(discordFormService.FORM_ACCEPT, discordFormService::buttonFormAccept);
        handler.registerButton(discordFormService.FORM_DECLINE, discordFormService::buttonFormDecline);
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

        // Start messages
        TextChannel startFormsChannel = channels.get("start_forms");
        boolean clear = true;
        try {
            MessageHistory history = new MessageHistory(startFormsChannel);
            List<Message> msgs = history.retrievePast(100).complete();

            if (!msgs.isEmpty()) {
                if (msgs.size() == 1) {
                    Message message = msgs.get(0);
                    ActionRow row = message.getActionRows().stream().filter(
                            a -> {
                                List<Button> buttons = a.getButtons();
                                if (!buttons.isEmpty()) {
                                    return buttons.stream().filter(b -> Objects.equals(b.getId(), discordFormService.FORM_CREATE))
                                            .findFirst().orElse(null) != null;
                                }
                                return false;
                            }
                    ).findFirst().orElse(null);
                    if (row != null) {
                        AuraBackend.getLogger().info("Founded start message for creating form");
                        clear = false;
                    }
                }

                if (clear)
                    startFormsChannel.deleteMessages(msgs).queue();
            }
        } catch (Exception exception) {
            AuraBackend.getLogger().info(
                    "\u001B[31mMessages in form channel can't be deleted! Delete it yourself!\u001B[37m"
            );
        }
        if (clear) {
            ConfigurationNode formNode = root.getNode("form");

            MessageEmbed embed = EmbedBuilder.fromData(
                    DataObject.fromJson(
                            formNode.getNode("start_embed").getString()
                    )
            ).build();
            startFormsChannel.sendMessageEmbeds(embed).addActionRow(
                    Button.primary(discordFormService.FORM_CREATE, formNode.getNode("button_label").getString())
            ).queue();
        }
    }
}
