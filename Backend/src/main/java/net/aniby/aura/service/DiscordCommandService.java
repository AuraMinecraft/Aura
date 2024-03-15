package net.aniby.aura.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.discord.CommandHandler;
import net.aniby.aura.discord.commands.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.data.DataObject;
import ninja.leaping.configurate.ConfigurationNode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscordCommandService {
    @Getter
    final CommandHandler handler;
    AuraConfig config;
    DiscordFormService discordFormService;
    DiscordService discordService;

    public DiscordCommandService(AuraConfig config, DiscordService discordService, DiscordFormService discordFormService) {
        this.discordFormService = discordFormService;
        this.discordService = discordService;
        this.config = config;

        this.handler = new CommandHandler(discordService.getJda());
        this.handler.registerCommands(
                new AuraCommand(),
                new LinkCommand(),
                new ForceLinkCommand(),
                new UnlinkCommand(),
                new AuraLinkCommand(),
                new ProfileCommand(),
                new DonateCommand()
        );
        handler.confirm();
        handler.registerButton(discordFormService.FORM_CREATE, discordFormService::buttonFormCreate);
        handler.registerModal(discordFormService.FORM_SUBMIT, discordFormService::modalFormSubmit);
        handler.registerButton(discordFormService.FORM_ACCEPT, discordFormService::buttonFormAccept);
        handler.registerButton(discordFormService.FORM_DECLINE, discordFormService::buttonFormDecline);

        // Start messages
        TextChannel startFormsChannel = discordService.getChannels().get("start_forms");
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
                        clear = false;
                    }
                }

                if (clear)
                    startFormsChannel.deleteMessages(msgs).queue();
            }
        } catch (Exception exception) {}
        if (clear) {
            ConfigurationNode formNode = config.getRoot().getNode("form");

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
