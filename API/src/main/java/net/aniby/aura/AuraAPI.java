package net.aniby.aura;

import net.aniby.aura.tool.DiscordWebhook;
import net.aniby.aura.tool.Replacer;

import java.io.IOException;
import java.util.List;

public class AuraAPI {
    public static void webhook(String json, List<Replacer> replacers, String url) throws IOException {
        DiscordWebhook.execute(url, Replacer.replaceAll(json, replacers));
    }
}
