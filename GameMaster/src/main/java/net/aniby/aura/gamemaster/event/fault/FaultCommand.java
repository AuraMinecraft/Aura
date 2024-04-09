package net.aniby.aura.gamemaster.event.fault;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import net.aniby.aura.core.CoreConfig;
import net.aniby.aura.gamemaster.AuraGameMaster;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@CommandAlias("fault")
public class FaultCommand extends BaseCommand {
    void faultAction(CommandSender sender, String id, String fieldName, Consumer<? super Fault> faultConsumer) {
        FaultRepository repository = AuraGameMaster.getFaultRepository();
        FileConfiguration config = repository.getConfig().getConfiguration();
        List<Fault> faults = new ArrayList<>();

        if (id != null && !id.isEmpty()) {
            if (Objects.equals("all", id))
                faults = repository.getList();
            else {
                try {
                    int identifier = Integer.parseInt(id);
                    if (repository.getList().size() - 1 >= identifier) {
                        Fault fault = repository.getList().get(0);
                        faults.add(fault);
                    }
                } catch (Exception ignored) {}
            }
        } else {
            if (sender instanceof Player player) {
                Fault fault = repository.getNearest(player.getLocation(), 50);
                if (fault != null)
                    faults.add(fault);
            }
        }

        if (faults.isEmpty()) {
            sender.sendMessage(CoreConfig.getMessage(config,
                    "empty_fault_list"
            ));
            return;
        }

        List<Fault> inProcessing = faults.stream().filter(Fault::isProcessing).toList();
        faults.removeAll(inProcessing);

        if (!faults.isEmpty()) {
            faults.forEach(faultConsumer);
            sender.sendMessage(CoreConfig.getMessage(config,
                    fieldName
            ));
        }
        if (!inProcessing.isEmpty()) {
            sender.sendMessage(CoreConfig.getMessage(config,
                    "fault_in_processing",
                    Placeholder.unparsed("fault_in_processing_count", String.valueOf(inProcessing.size()))
            ));
        }
    }

    @Subcommand("clear")
    @CommandPermission("aura.event.fault.clear")
    public void clear(CommandSender sender, @Optional String id) {
        faultAction(sender, id, "command_clear", f -> new Fault.Narrower(f, f.getBlocks()).run());
    }

    @Subcommand("delete")
    @CommandPermission("aura.event.fault.delete")
    public void delete(CommandSender sender, @Optional String id) {
        faultAction(sender, id, "command_clear", f -> {
            f.getBlocks().forEach(f::clearPart);
        });
    }

    @Subcommand("narrow")
    @CommandPermission("aura.event.fault.narrow")
    public void narrow(CommandSender sender, @Optional String id) {
        faultAction(sender, id, "command_narrow", f -> new Fault.Narrower(f).run());
    }

    @Subcommand("create")
    @CommandPermission("aura.event.fault.create")
    public void create(Player player) {
        FaultRepository repository = AuraGameMaster.getFaultRepository();
        FileConfiguration config = repository.getConfig().getConfiguration();

        Fault fault = repository.getNearest(player.getLocation(), 10);
        if (fault != null) {
            player.sendMessage(CoreConfig.getMessage(config,
                    "fault_too_close"
            ));
            return;
        }

        repository.create(player.getLocation(), Fault.getRandomExitLocation(), "abyss");

        player.sendMessage(CoreConfig.getMessage(config,
                "command_create"
        ));
    }

    @Subcommand("extend")
    @CommandPermission("aura.event.fault.extend")
    public void extend(CommandSender sender, @Optional String id) {
        faultAction(sender, id, "command_extend", f -> new Fault.Extender(f).run());
    }
}
