package net.aniby.aura.service.discord.command;

import io.graversen.minecraft.rcon.MinecraftRcon;
import io.graversen.minecraft.rcon.commands.base.ICommand;
import io.graversen.minecraft.rcon.service.MinecraftRconService;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.util.AuraConfig;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.service.discord.DiscordIRC;
import net.aniby.aura.service.user.UserService;
import net.aniby.aura.tool.Replacer;
import net.aniby.aura.util.ShopConfig;
import net.aniby.aura.util.ShopGood;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@FieldDefaults(makeFinal = true)
public class BuyCommand implements ACommand {
    AuraConfig config;
    UserService userService;
    UserRepository userRepository;
    DiscordIRC discordIRC;
    MinecraftRconService rconService;

    List<ShopGood> goods;

    public BuyCommand(@Lazy AuraConfig config, @Lazy ShopConfig shopConfig, @Lazy UserService userService, UserRepository userRepository, @Lazy DiscordIRC discordIRC, MinecraftRconService rconService) {
        this.config = config;
        this.userService = userService;
        this.userRepository = userRepository;
        this.discordIRC = discordIRC;

        this.goods = shopConfig.getGoods();
        this.rconService = rconService;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        if (!discordIRC.hasDefaultPermission(event))
            return;

        String discordId = event.getUser().getId();
        AuraUser user = userService.getByWith("discord_id", discordId);
        List<Replacer> replacers = userService.getReplacers(user);
        if (user == null || user.getTwitchId() == null) {
            event.getHook().editOriginal(config.getMessage("need_linked_twitch", replacers)).queue();
            return;
        }

        String goodValue = event.getOption("good").getAsString();
        ShopGood good = goods.stream().filter(g -> Objects.equals(g.value(), goodValue)).findFirst().orElse(null);

        if (good == null) {
            event.getHook().editOriginal(
                    config.getMessage("unknown_action", replacers)
            ).queue();
            return;
        }

        replacers.add(Replacer.r("need_aura", good.cost()));

        if (good.cost() > user.getAura()) {
            event.getHook().editOriginal(
                    config.getMessage("not_enough_aura", replacers)
            ).queue();
            return;
        }

        user.setAura(user.getAura() - good.cost());
        userRepository.update(user);

        List<String> rconCommands = good.rconCommands();
        if (rconCommands != null && !rconCommands.isEmpty()) {
            MinecraftRcon rcon = rconService.minecraftRcon().orElse(null);
            if (rcon == null) {
                event.getHook().editOriginal(
                        config.getMessage("rcon_connection_error")
                ).queue();
                return;
            }
            ICommand[] commands = rconCommands.stream()
                    .map(d -> (ICommand) () -> d.replace("<player_name>", user.getPlayerName()))
                    .toList().toArray(new ICommand[0]);
            rcon.sendAsync(commands);
        }

        List<String> roles = good.discordRoles();
        if (roles != null && !roles.isEmpty()) {
            User discordUser = userService.getDiscordUser(user);
            assert discordUser != null;
            Guild guild = discordIRC.getDefaultGuild();

            for (String roleId : roles) {
                Role addRole = guild.getRoleById(roleId);
                if (addRole != null)
                    guild.addRoleToMember(discordUser, addRole).queue();
            }
        }

        event.getHook().editOriginal(
                config.getMessage("shop_bought", replacers)
        ).queue();
    }

    @Override
    public SlashCommandData slashCommandData() {
        OptionData option = new OptionData(OptionType.STRING, "good", "Доступный товар", true, false);

        for (ShopGood good : goods)
            option = option.addChoice(good.name(), good.value());

        return Commands.slash("buy", "Управление аккаунтом пользователя")
                .addOptions(option);
    }
}