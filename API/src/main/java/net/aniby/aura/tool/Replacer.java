package net.aniby.aura.tool;

public record Replacer(String key, Object value) {
    public String replace(String string) {
        return string.replace(key, String.valueOf(value));
    }

    public static Replacer r(String key, Object value) {
        return new Replacer("<" + key + ">", value);
    }
}
