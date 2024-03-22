package net.aniby.aura.gamemaster;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

public record MasterMessage(Date date, String sender, String receiver, Component component) {
    static final ArrayList<MasterMessage> messages = new ArrayList<>();

    public static void addMessage(MasterMessage masterMessage) {
        messages.add(masterMessage);
    }

    public static ArrayList<MasterMessage> getMessages(@NotNull String receiver, int page) {
        ArrayList<MasterMessage> receivedMessages = new ArrayList<>();
        int messagesPerPage = AuraGameMaster.getInstance().getConfig().getInt("list.messages_per_page", 10);
        for (int i = messages.size() - 1; i >= 0; i--) {
            MasterMessage message = messages.get(i);
            if (Objects.equals(receiver, message.receiver)) {
                receivedMessages.add(message);
                if (receivedMessages.size() == messagesPerPage)
                    break;
            }
        }
        return receivedMessages;
    }

}
