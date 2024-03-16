package net.aniby.aura.service.twitch;

import lombok.Getter;
import net.aniby.aura.tool.AuraUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TwitchLinkState {
    public static List<TwitchLinkState> active = new ArrayList<>();

    @Getter
    private final @NotNull String uuid;
    @Getter
    private final @NotNull String code;

    private final long expiresIn;
    @Getter
    private final @NotNull String discordId;

    public TwitchLinkState(@NotNull String discordId, long expireTime) {
        active.removeIf(s -> Objects.equals(discordId, s.discordId));

        this.uuid = UUID.randomUUID().toString();
        this.code = AuraUtils.getRandomString(6);
        this.expiresIn = expireTime + new Date().getTime();
        this.discordId = discordId;

        active.add(this);
    }

    public static @Nullable TwitchLinkState getByCode(@NotNull String code) {
        TwitchLinkState state = active.stream()
                .filter(s -> Objects.equals(s.code, code))
                .findFirst().orElse(null);
        if (state != null) {
            long time = new Date().getTime();
            if (time < state.expiresIn)
                return state;
        }
        return null;
    }

    public static @Nullable String getByUUID(@NotNull String uuid) {
        TwitchLinkState state = active.stream()
                .filter(s -> Objects.equals(s.uuid, uuid))
                .findFirst().orElse(null);
        if (state != null) {
            active.remove(state);

            long time = new Date().getTime();
            if (time < state.expiresIn) {
                return state.getDiscordId();
            }
        }
        return null;
    }
}
