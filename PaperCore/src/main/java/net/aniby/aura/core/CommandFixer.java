package net.aniby.aura.core;

import lombok.RequiredArgsConstructor;
import net.aniby.aura.core.AuraCore;
import net.aniby.aura.core.CoreConfig;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.HashMap;

@RequiredArgsConstructor
public class CommandFixer {
    static final HashMap<String, Long> commandCooldown = new HashMap<>();

    public static boolean checkCooldown(Player source) {
        String sourceName = source.getName();
        if (commandCooldown.containsKey(sourceName)) {
            long leaved = commandCooldown.get(sourceName) -  new Date().getTime();
            if (leaved <= 0) {
                commandCooldown.remove(sourceName);
            } else {
                source.sendMessage(CoreConfig.getMessage(
                        "command_cooldown", Placeholder.unparsed("cooldown",
                                String.valueOf(Math.round((float) leaved / 1000)))
                ));
                return false;
            }
        }
        long commandUsageCooldown =
                1000L * AuraCore.getInstance().getConfig().getRoot().getInt("command_usage_cooldown");
        commandCooldown.put(sourceName, commandUsageCooldown + new Date().getTime());
        return true;
    }
}
