package net.aniby.aura.util;

import javax.annotation.Nullable;
import java.util.List;

public record ShopGood(String name, String value, double cost, @Nullable List<String> rconCommands,
                       @Nullable List<String> discordRoles) {
}
