package net.aniby.aura.gamemaster;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.gamemaster.common.command.EventCommand;

import java.util.ArrayList;
import java.util.HashMap;

@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventQueue {
    @Getter
    private static final ArrayList<EventQueue> queue = new ArrayList<>();

    String sender;
    String receiver;
    double aura;
}
