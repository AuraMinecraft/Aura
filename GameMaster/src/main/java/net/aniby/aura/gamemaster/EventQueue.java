package net.aniby.aura.gamemaster;

import lombok.Getter;

import java.util.HashMap;

public class EventQueue {
    @Getter
    private static final HashMap<String, Double> queue = new HashMap<>();
}
