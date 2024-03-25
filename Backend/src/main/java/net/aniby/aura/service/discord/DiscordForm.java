package net.aniby.aura.service.discord;

import io.graversen.minecraft.rcon.service.MinecraftRconService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.util.AuraConfig;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.service.user.UserService;
import net.aniby.aura.tool.Replacer;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import ninja.leaping.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DiscordForm {
    Logger logger = LoggerFactory.getLogger(DiscordForm.class);

    AuraConfig config;
    DiscordIRC discordIRC;
    UserService userService;
    UserRepository userRepository;

    public DiscordForm(AuraConfig config, @Lazy DiscordIRC discordIRC, @Lazy UserService userService, UserRepository userRepository) {
        this.config = config;
        this.discordIRC = discordIRC;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public final String FORM_ACCEPT = "auralink:form:accept:";

    public void buttonFormAccept(@NotNull ButtonInteractionEvent event) {
        User source = event.getUser();
        String[] args = event.getComponentId().split(":");
        String discordId = args[3];

        AuraUser auraUser = userService.getByWith("discord_id", discordId);
        if (auraUser == null || !userService.inGuild(auraUser)) {
            event.editMessage(config.getMessage("not_in_guild"))
                    .setEmbeds(new ArrayList<>()).setComponents(new ArrayList<>()).queue();
            return;
        }

        if (auraUser.getPlayerName() == null) {
            event.editMessage(config.getMessage("player_name_not_found"))
                    .setEmbeds(new ArrayList<>()).setComponents(new ArrayList<>()).queue();
            return;
        }

        userService.setWhitelist(auraUser, true);
        userRepository.update(auraUser);

        List<Replacer> tags = userService.getReplacers(auraUser);
        tags.add(Replacer.r("admin_mention", source.getAsMention()));
        // Logging
        logger.info(
                "\u001B[36m" + auraUser.getPlayerName() + "\u001B[37m was \u001B[32madded \u001B[37mto server by \u001B[33m(" + source.getName() + "/" + source.getId() + ")\u001B[37m"
        );

        // Add role
        User target = userService.getDiscordUser(auraUser);

        Role addRole = discordIRC.getRoles().get("player");
        discordIRC.getDefaultGuild().addRoleToMember(target, addRole).queue();

        try {
            target.openPrivateChannel().flatMap(privateChannel ->
                    privateChannel.sendMessage(config.getMessage("jf_accepted_target", tags))
            ).queue();
        } catch (Exception ignored) {
        }

        event.editMessage(config.getMessage("jf_accepted", tags)).setEmbeds(new ArrayList<>()).setComponents(new ArrayList<>()).queue();
    }

    public final String FORM_DECLINE = "auralink:form:decline:";

    public void buttonFormDecline(@NotNull ButtonInteractionEvent event) {
        User source = event.getUser();
        String[] args = event.getComponentId().split(":");
        String discordId = args[3];

        AuraUser auraUser = userService.getByWith("discord_id", discordId);
        if (auraUser == null || !userService.inGuild(auraUser)) {
            event.editMessage(config.getMessage("not_in_guild"))
                    .setEmbeds(new ArrayList<>()).setComponents(new ArrayList<>()).queue();
            return;
        }

        List<Replacer> tags = userService.getReplacers(auraUser);
        tags.add(Replacer.r("admin_mention", source.getAsMention()));
        // Logging
        logger.info(
                "\u001B[36m" + auraUser.getPlayerName() + "\u001B[37m form was \u001B[31mdeclined \u001B[37mby \u001B[33m(" + source.getName() + "/" + source.getId() + ")\u001B[37m"
        );

        // Add role
        try {
            User target = userService.getDiscordUser(auraUser);
            if (target != null)
                target.openPrivateChannel().flatMap(privateChannel ->
                        privateChannel.sendMessage(config.getMessage("jf_declined_target", tags))
                ).queue();
        } catch (Exception ignored) {}

        event.editMessage(config.getMessage("jf_declined", tags)).setEmbeds(new ArrayList<>()).setComponents(new ArrayList<>()).queue();
    }

    public final String FORM_SUBMIT = "auralink:form:submit";
    public void modalFormSubmit(@NotNull ModalInteractionEvent event) {
        event.deferReply(true).queue();

        User user = event.getUser();

        ConfigurationNode root = config.getRoot();
        ConfigurationNode section = root
                .getNode("form", "modal", "questions");
        Set<String> ids = AuraConfig.getNodeKeys(section);

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

        AuraUser auraUser = userService.getByWith("discord_id", user.getId());
        assert auraUser != null;
        auraUser.setPlayerName(nickname);
        userRepository.update(auraUser);

        tags.addAll(userService.getReplacers(auraUser));

        try {
            Role addRole = discordIRC.getRoles().get("form_sent");
            discordIRC.getDefaultGuild().addRoleToMember(user, addRole).queue();
        } catch (Exception ignored) {}

        try {
            Member member = userService.getGuildMember(auraUser);
            assert member != null;
            discordIRC.getDefaultGuild().modifyNickname(member, nickname).queue();
        } catch (Exception ignored) {}

        MessageEmbed embed = config.getEmbed("form_log", tags);

        String userId = user.getId();
        discordIRC.getChannels().get("log_forms").sendMessageEmbeds(embed).setActionRow(
                Button.success(FORM_ACCEPT + userId, config.getMessage("jf_button_accept")),
                Button.danger(FORM_DECLINE + userId, config.getMessage("jf_button_decline"))
        ).queue();

        event.getHook().editOriginal(config.getMessage("jf_sent")).queue();
    }

    public final String FORM_CREATE = "auralink:form:create";

    public void buttonFormCreate(@NotNull ButtonInteractionEvent event) {
        Member member = event.getMember();
        if (member == null)
            return;

        AuraUser user = userService.getByWith("discord_id", event.getUser().getId());
        if (user == null || user.getTwitchId() == null) {
            event.reply(config.getMessage("need_linked_twitch")).setEphemeral(true).queue();
            return;
        }
        double needAura = config.getRoot().getNode("aura", "need_for_form").getDouble();

        List<Replacer> tags = userService.getReplacers(user);
        tags.add(Replacer.r("need_aura", String.valueOf(Math.floor(needAura * 100) / 100)));

        if (user.getAura() < needAura) {
            event.reply(config.getMessage("not_enough_aura", tags)).setEphemeral(true).queue();
            return;
        }

        Role playerRole = discordIRC.getRoles().get("player");
        Role declinedFormRole = discordIRC.getRoles().get("form_sent");

        Role role = member.getRoles().stream()
                .filter(r -> r.getId().equals(declinedFormRole.getId()) || r.getId().equals(playerRole.getId()))
                .findFirst().orElse(null);
        if (role != null || user.isWhitelisted()) {
            event.reply(config.getMessage("jf_already_written", tags)).setEphemeral(true).queue();
            return;
        }

        event.replyModal(getModal()).queue();
    }

    public Modal getModal() {
        ConfigurationNode modalSection = config.getRoot().getNode("form", "modal");
        List<ActionRow> rows = getSubjects(modalSection.getNode("questions"));

        return Modal.create(FORM_SUBMIT, modalSection.getNode("label").getString("Анкета"))
                .addComponents(rows)
                .build();
    }

    private List<ActionRow> getSubjects(ConfigurationNode node) {
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
