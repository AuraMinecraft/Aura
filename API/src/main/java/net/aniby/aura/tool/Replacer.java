package net.aniby.aura.tool;

import java.util.List;

public record Replacer(String key, Object value) {
    public String replace(String string) {
        return string.replace(key, String.valueOf(value));
    }

    public static Replacer r(String key, Object value) {
        return new Replacer("<" + key + ">", value);
    }

    public static String replaceAll(String string, List<Replacer> replacers) {
        for (Replacer replacer : replacers) {
            string = replacer.replace(string);
        }
        return string;
    }
}
