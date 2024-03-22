package net.aniby.aura.service.discord;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.util.AuraConfig;
import net.aniby.aura.discord.CommandHandler;
import net.aniby.aura.service.discord.commands.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import ninja.leaping.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscordIRC extends ListenerAdapter {
    AuraConfig config;
    DiscordForm discordForm;

    @Getter
    final CommandHandler handler;

    @Getter
    final JDA jda;
    @Getter
    Guild defaultGuild = null;
    @Getter
    final HashMap<String, Role> roles = new HashMap<>();
    @Getter
    final HashMap<String, TextChannel> channels = new HashMap<>();

    public DiscordIRC(AuraConfig config, DiscordListener discordListener, DiscordForm discordForm,
                      AuraCommand auraCommand,
//                      StatisticCommand statisticCommand,
                      BuyCommand buyCommand,
                      LinkCommand linkCommand,
                      WhitelistCommand whitelistCommand,
                      ProfileCommand profileCommand,
                      DonateCommand donateCommand) {
        this.config = config;
        this.discordForm = discordForm;

        ConfigurationNode node = config.getRoot().getNode("discord");
        this.jda = JDABuilder.createDefault(
                        node.getNode("bot_token").getString()
                ).setActivity(Activity.of(Activity.ActivityType.STREAMING, "aura.aniby.net"))
                .addEventListeners(discordListener, this)
                .build();

        this.handler = new CommandHandler(this.getJda());
        this.handler.registerCommands(
                auraCommand,
                linkCommand,
//                statisticCommand,
                buyCommand,
                whitelistCommand,
                profileCommand,
                donateCommand
        );
        handler.confirm();
        handler.registerButton(discordForm.FORM_CREATE, discordForm::buttonFormCreate);
        handler.registerModal(discordForm.FORM_SUBMIT, discordForm::modalFormSubmit);
        handler.registerButton(discordForm.FORM_ACCEPT, discordForm::buttonFormAccept);
        handler.registerButton(discordForm.FORM_DECLINE, discordForm::buttonFormDecline);
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
        TextChannel startFormsChannel = this.getChannels().get("start_forms");
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
                                    return buttons.stream().filter(b -> Objects.equals(b.getId(), discordForm.FORM_CREATE))
                                            .findFirst().orElse(null) != null;
                                }
                                return false;
                            }
                    ).findFirst().orElse(null);
                    if (row != null) {
                        clear = false;
                    }
                }

                if (clear)
                    startFormsChannel.deleteMessages(msgs).queue();
            }
        } catch (Exception exception) {
        }
        if (clear) {
            ConfigurationNode formNode = config.getRoot().getNode("form");

            startFormsChannel.sendMessageEmbeds(
                    config.getEmbed("form_start")
            ).addActionRow(
                    Button.primary(discordForm.FORM_CREATE, formNode.getNode("button_label").getString())
            ).queue();
        }
    }

    public boolean hasDefaultPermission(SlashCommandInteractionEvent event) {
        // Is bot
        User user = event.getUser();
        if (user.isBot()) {
            event.getHook().editOriginal(config.getMessage("invalid_executor")).queue();
            return false;
        }

        // Check in guild
        try {
            this.getDefaultGuild().retrieveMember(user).complete();
        } catch (ErrorResponseException exception) {
            event.getHook().editOriginal(
                    config.getMessage("not_in_guild")
            ).queue();
            return false;
        }
        return true;
    }
}
