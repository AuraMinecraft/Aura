package net.aniby.aura.twitch;

public record AccessData(String accessToken, String refreshToken, long expiresIn) {}