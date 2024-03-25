package net.aniby.aura.service.discord.command;

import lombok.experimental.FieldDefaults;
import net.aniby.aura.util.AuraConfig;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.discord.hash.AuraHash;
import net.aniby.aura.discord.hash.DonateHash;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.service.discord.DiscordIRC;
import net.aniby.aura.service.user.UserService;
import net.aniby.aura.tool.Replacer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@FieldDefaults(makeFinal = true)
public class StatisticCommand implements ACommand {
    AuraConfig config;
    UserService userService;
    UserRepository userRepository;
    AuraDatabase database;
    DiscordIRC discordIRC;

    AuraHash auraHash;
    DonateHash donateHash;

    public StatisticCommand(AuraConfig config, @Lazy UserService userService, UserRepository userRepository, AuraDatabase database, @Lazy DiscordIRC discordIRC) {
        this.config = config;
        this.userService = userService;
        this.userRepository = userRepository;
        this.database = database;
        this.discordIRC = discordIRC;

        auraHash = new AuraHash(this.database, 10);
        donateHash = new DonateHash(this.database, 10);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        if (!discordIRC.hasDefaultPermission(event))
            return;

        String subcommandName = event.getSubcommandName();
        assert subcommandName != null;

        ArrayList<Replacer> replacers = new ArrayList<>();
        try {
            switch (subcommandName) {
                case "aura" -> {
                    auraHash.updateIfNeed();

                    List<Object> objects = auraHash.getList();
                    for (int i = 0; i < objects.size(); i++) {
                        AuraUser user = (AuraUser) objects.get(i);
                        String prefix = (i + 1) + "s_";
                        replacers.addAll(userService.getReplacers(user, prefix));
                        for (Replacer replacer : userService.getReplacers(user, prefix)) {
                            System.out.println(replacer.key());
                        }
                    }
                    event.getHook().editOriginalEmbeds(
                            config.getEmbed("statistic_aura", replacers)
                    ).queue();
                    return;
                }
                case "donate" -> {
                    donateHash.updateIfNeed();

                    List<Object> objects = donateHash.getList();
                    for (int i = 0; i < objects.size(); i++) {
                        Map.Entry<AuraUser, Double> entry = (Map.Entry<AuraUser, Double>) objects.get(i);
                        String prefix = (i + 1) + "s_";
                        replacers.addAll(userService.getReplacers(entry.getKey(), prefix));
                        replacers.add(Replacer.r(prefix + "total_donated", String.valueOf(entry.getValue())));
                    }
                    event.getHook().editOriginalEmbeds(
                            config.getEmbed("statistic_donate", replacers)
                    ).queue();
                    return;
                }
            }
        } catch (Exception ignored) {}
        event.getHook().editOriginal(config.getMessage("something_went_wrong")).queue();
    }

    @Override
    public SlashCommandData slashCommandData() {
        SubcommandData aura = new SubcommandData("aura", "Статистика по количеству ауры");
        SubcommandData donate = new SubcommandData("donate", "Статистика по донатам");

        return Commands.slash("statistic", "Статистика пользователей")
                .addSubcommands(aura, donate)
                .setGuildOnly(true);
    }


}