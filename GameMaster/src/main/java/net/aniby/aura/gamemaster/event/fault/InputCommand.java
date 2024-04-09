package net.aniby.aura.gamemaster.event.fault;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import net.aniby.aura.core.AuraCore;
import net.aniby.aura.core.CommandFixer;
import net.aniby.aura.core.CoreConfig;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.gamemaster.AuraGameMaster;
import net.aniby.aura.repository.UserRepository;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

@CommandAlias("input")
public class InputCommand extends BaseCommand {
    @Default
    public void input(Player player, double aura) {
        if (!CommandFixer.checkCooldown(player))
            return;

        UserRepository userRepository = AuraCore.getInstance().getUserRepository();
        AuraUser user = userRepository.findByWhitelistedPlayerName(player.getName());
        if (user == null) {
            player.sendMessage(CoreConfig.getMessage(
                    "database_error"
            ));
            return;
        }

        if (user.getAura() <= aura) {
            player.sendMessage(CoreConfig.getMessage(
                    "not_enough_aura"
            ));
            return;
        }

        FaultRepository manager = AuraGameMaster.getFaultRepository();
        FileConfiguration config = manager.getConfig().getConfiguration();
        Fault fault =
                manager.getNearest(player.getLocation(), config.getDouble("find_range"));

        if (fault == null) {
            player.sendMessage(CoreConfig.getMessage(config, "too_far_to_fault"));
            return;
        }

        // Change user aura
        user.setAura(user.getAura() - aura);
        userRepository.update(user);

        // Change fault aura
        double mod = config.getDouble("fault_aura_mod");
        int oldAuraDiv = (int) Math.ceil(fault.getAura() / mod);

        fault.setAura(fault.getAura() + aura);
        manager.save(fault);

        int newAuraDiv = (int) Math.ceil(fault.getAura() / mod);

        // Messages and compressing
        player.sendMessage(CoreConfig.getMessage(config, "fault_input",
                Placeholder.unparsed("input_aura", String.valueOf(aura))));
        if (newAuraDiv > oldAuraDiv) {
            player.sendMessage(CoreConfig.getMessage(
                    config, "fault_compress"
            ));
            new Fault.Narrower(fault).run();
        }
    }
}
