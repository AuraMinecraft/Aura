package net.aniby.aura.discord;

import lombok.Getter;
import net.aniby.aura.AuraAPI;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.tools.AuraUtils;
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

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Getter
public class DiscordIRC extends ListenerAdapter {
    final JDA jda;
    Guild defaultGuild = null;
    final HashMap<String, Role> roles = new HashMap<>();
    final HashMap<String, TextChannel> channels = new HashMap<>();
    DiscordLogger logger = null;

    public DiscordIRC() {
        ConfigurationNode node = AuraBackend.getConfig().getRoot().getNode("discord");
        this.jda = JDABuilder.createDefault(
                node.getNode("bot_token").getString()
        ).setActivity(Activity.of(Activity.ActivityType.STREAMING, "aura.aniby.net"))
                .addEventListeners(new DiscordListener(), this)
                .build();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        ConfigurationNode root = AuraBackend.getConfig().getRoot();
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
        logger = new DiscordLogger();

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
                                    return buttons.stream().filter(b -> Objects.equals(b.getId(), JoinForm.FORM_CREATE))
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
                    Button.primary(JoinForm.FORM_CREATE, formNode.getNode("button_label").getString())
            ).queue();
        }
    }
}
