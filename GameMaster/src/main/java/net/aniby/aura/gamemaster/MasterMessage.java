package net.aniby.aura.gamemaster;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;

@Getter
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MasterMessage {
    @Getter
    static final ArrayList<MasterMessage> messages = new ArrayList<>();

    final String sender;
    final String receiver;
    final Component message;
}
