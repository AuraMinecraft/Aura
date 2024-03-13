package net.aniby.aura.discord;

import net.aniby.aura.AuraBackend;
import net.aniby.aura.modules.AuraUser;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.tool.Replacer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.data.DataObject;
import ninja.leaping.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class JoinForm {
    public static final String FORM_ACCEPT = "auralink:form:accept:";

    public static void buttonFormAccept(@NotNull ButtonInteractionEvent event) {
        AuraConfig config = AuraBackend.getConfig();
        User source = event.getUser();
        String[] args = event.getComponentId().split(":");
        String discordId = args[3];

        AuraUser CAuraUser = AuraUser.getByWith("discord_id", discordId);
        if (CAuraUser == null || !CAuraUser.inGuild()) {
            event.editMessage(config.getMessage("not_in_guild"))
                    .setEmbeds(new ArrayList<>()).setComponents(new ArrayList<>()).queue();
            return;
        }

        if (CAuraUser.getPlayerName() == null) {
            event.editMessage(config.getMessage("player_name_not_found"))
                    .setEmbeds(new ArrayList<>()).setComponents(new ArrayList<>()).queue();
            return;
        }

        CAuraUser.setWhitelisted(true);
        CAuraUser.save();

        List<Replacer> tags = CAuraUser.getReplacers();
        tags.add(Replacer.r("admin_mention", source.getAsMention()));
        // Logging
        AuraBackend.getLogger().info(
                "\u001B[36m" + CAuraUser.getPlayerName() + "\u001B[37m was \u001B[32madded \u001B[37mto server by \u001B[33m(" + source.getName() + "/" + source.getId() + ")\u001B[37m"
        );

        // Add role
        User target = CAuraUser.getDiscordUser();

        DiscordIRC irc = AuraBackend.getDiscord();
        Role addRole = irc.getRoles().get("player");
        irc.getDefaultGuild().addRoleToMember(target, addRole).queue();

        try {
            target.openPrivateChannel().flatMap(privateChannel ->
                    privateChannel.sendMessage(config.getMessage("jf_accepted_target", tags))
            ).queue();
        } catch (Exception ignored) {
        }

        event.editMessage(config.getMessage("jf_accepted", tags)).setEmbeds(new ArrayList<>()).setComponents(new ArrayList<>()).queue();
    }

    public static final String FORM_DECLINE = "auralink:form:decline:";

    public static void buttonFormDecline(@NotNull ButtonInteractionEvent event) {
        AuraConfig config = AuraBackend.getConfig();
        User source = event.getUser();
        String[] args = event.getComponentId().split(":");
        String discordId = args[3];

        AuraUser CAuraUser = AuraUser.getByWith("discord_id", discordId);
        if (CAuraUser == null || !CAuraUser.inGuild()) {
            event.editMessage(config.getMessage("not_in_guild"))
                    .setEmbeds(new ArrayList<>()).setComponents(new ArrayList<>()).queue();
            return;
        }

        List<Replacer> tags = CAuraUser.getReplacers();
        tags.add(Replacer.r("admin_mention", source.getAsMention()));
        // Logging
        AuraBackend.getLogger().info(
                "\u001B[36m" + CAuraUser.getPlayerName() + "\u001B[37m form was \u001B[31mdeclined \u001B[37mby \u001B[33m(" + source.getName() + "/" + source.getId() + ")\u001B[37m"
        );

        // Add role
        try {
            User target = CAuraUser.getDiscordUser();
            if (target != null)
                target.openPrivateChannel().flatMap(privateChannel ->
                        privateChannel.sendMessage(config.getMessage("jf_declined_target", tags))
                ).queue();
        } catch (Exception ignored) {}

        event.editMessage(config.getMessage("jf_declined", tags)).setEmbeds(new ArrayList<>()).setComponents(new ArrayList<>()).queue();
    }

    public static final String FORM_SUBMIT = "auralink:form:submit";
    public static void modalFormSubmit(@NotNull ModalInteractionEvent event) {
        event.deferReply(true).queue();

        User user = event.getUser();

        AuraConfig config = AuraBackend.getConfig();
        ConfigurationNode root = config.getRoot();
        ConfigurationNode section = root
                .getNode("form", "modal", "questions");
        Set<String> ids = AuraConfig.getNodeKeys(section);

        String logStringEmbed = root.getNode("form", "log_embed").getString();

        List<Replacer> tags = new ArrayList<>();
        tags.add(Replacer.r("discord_id", user.getId()));

        String nickname = null;
        for (String id : ids) {
            String value = event.getValue(id).getAsString();
            if (id.equals("nickname"))
                nickname = value;

            String label = section.getNode(id, "label").getString();

            tags.add(Replacer.r("field_" + id + "_label", label));
            tags.add(Replacer.r("field_" + id + "_value", value));
        }
        if (nickname == null) {
            event.getHook().editOriginal(config.getMessage("jf_nickname_error")).queue();
            return;
        }

        AuraUser auraUser = AuraUser.getByWith("discord_id", user.getId());
        assert auraUser != null;
        auraUser.setPlayerName(nickname);
        auraUser.save();

        tags.addAll(auraUser.getReplacers());
        logStringEmbed = config.replaceMessage(logStringEmbed, tags);


        DiscordIRC irc = AuraBackend.getDiscord();
        try {
            Role addRole = irc.getRoles().get("form_sent");
            irc.getDefaultGuild().addRoleToMember(user, addRole).queue();
        } catch (Exception ignored) {}

        try {
            Member member = auraUser.getGuildMember();
            assert member != null;
            irc.getDefaultGuild().modifyNickname(member, nickname).queue();
        } catch (Exception ignored) {}

        MessageEmbed embed = EmbedBuilder.fromData(
                DataObject.fromJson(logStringEmbed)
        ).build();

        String userId = user.getId();
        irc.getChannels().get("log_forms").sendMessageEmbeds(embed).setActionRow(
                Button.success(FORM_ACCEPT + userId, config.getMessage("jf_button_accept")),
                Button.danger(FORM_DECLINE + userId, config.getMessage("jf_button_decline"))
        ).queue();

        event.getHook().editOriginal(config.getMessage("jf_sent")).queue();
    }

    public static final String FORM_CREATE = "auralink:form:create";

    public static void buttonFormCreate(@NotNull ButtonInteractionEvent event) {
        AuraConfig config = AuraBackend.getConfig();

        Member member = event.getMember();
        if (member == null)
            return;

        AuraUser user = AuraUser.getByWith("discord_id", event.getUser().getId());
        if (user == null || user.getRefreshToken() == null) {
            event.reply(config.getMessage("need_linked_twitch")).setEphemeral(true).queue();
            return;
        }
        double needAura = config.getRoot().getNode("aura", "need_for_form").getDouble();

        List<Replacer> tags =user.getReplacers();
        tags.add(Replacer.r("need_aura", String.valueOf(Math.floor(needAura * 100) / 100)));

        if (user.getAura() < needAura) {
            event.reply(config.getMessage("not_enough_aura", tags)).setEphemeral(true).queue();
            return;
        }

        Role playerRole = AuraBackend.getDiscord().getRoles().get("player");
        Role declinedFormRole = AuraBackend.getDiscord().getRoles().get("form_sent");

        Role role = member.getRoles().stream()
                .filter(r -> r.getId().equals(declinedFormRole.getId()) || r.getId().equals(playerRole.getId()))
                .findFirst().orElse(null);
        if (role != null || user.isWhitelisted()) {
            event.reply(config.getMessage("jf_already_written", tags)).setEphemeral(true).queue();
            return;
        }

        event.replyModal(getModal()).queue();
    }

    public static Modal getModal() {
        ConfigurationNode modalSection = AuraBackend.getConfig().getRoot().getNode("form", "modal");
        List<ActionRow> rows = getSubjects(modalSection.getNode("questions"));

        return Modal.create(FORM_SUBMIT, modalSection.getNode("label").getString("Анкета"))
                .addComponents(rows)
                .build();
    }

    private static List<ActionRow> getSubjects(ConfigurationNode node) {
        List<ActionRow> subjects = new ArrayList<>();

        Set<String> keys = AuraConfig.getNodeKeys(node);
        HashMap<Integer, TextInput> inputs = new HashMap<>();
        for (String id : keys) {
            ConfigurationNode subject = node.getNode(id);
            TextInputStyle style = TextInputStyle.valueOf(subject.getNode("style").getString("SHORT"));
            String placeholder = subject.getNode("placeholder").getString(null);
            boolean required = subject.getNode("required").getBoolean(true);
            String label = subject.getNode("label").getString();
            int position = subject.getNode("position").getInt();
            TextInput input = TextInput.create(id, label, style)
                    .setMinLength(1)
                    .setMaxLength(style == TextInputStyle.SHORT ? 100 : 1000)
                    .setPlaceholder(placeholder)
                    .setRequired(required)
                    .build();
            inputs.put(position, input);
        }

        inputs.keySet().stream().sorted().forEach(index -> subjects.add(ActionRow.of(
                inputs.get(index)
        )));

        return subjects;
    }
}
