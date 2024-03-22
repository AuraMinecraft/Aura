package net.aniby.aura.paper.votes.logic;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.paper.votes.AuraVotes;
import net.aniby.aura.tool.FrequencyMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Vote {
    long expiresAt = 0;
    final HashMap<String, String> answers = new HashMap<>();
    @Setter
    ArrayList<String> possibleAnswers = new ArrayList<>();

    public boolean isStarted() {
        return this.expiresAt > 0;
    }

    public boolean isExpired() {
        return new Date().getTime() >= this.expiresAt && isStarted();
    }

    public void start(String admin, String message, long expiresAt) {
        this.expiresAt = expiresAt;

        Component header = AuraVotes.getMessage(
                "vote_start_header", Placeholder.unparsed("admin", admin)
        );
        Component main = AuraVotes.getMiniMessage().deserialize(message);
        Component buttons = Component.empty();
        for (int i = 0; i < possibleAnswers.size(); i++) {
            String answer = possibleAnswers.get(i);
            if (i != 0)
                buttons = buttons.append(
                        AuraVotes.getMessage("vote_start_button_delimiter")
                );
            buttons = buttons.append(
                    AuraVotes.getMiniMessage().deserialize(
                            AuraVotes.getPlainMessage("vote_start_button_format")
                                    .replace("%answer%", answer)
                    )
            );
        }

        Component toSend = header
                .appendNewline()
                .append(main)
                .appendNewline()
                .append(buttons);
        Bukkit.broadcast(toSend);
    }

    public ArrayList<String> getMostFrequentAnswers() {
        return new FrequencyMap(this.answers.values()).getMostCommon();
    }

    public void end(String forceAdmin) {
        FrequencyMap frequency = new FrequencyMap(this.answers.values());

        ArrayList<String> result = frequency.getMostCommon();
        String voteResult = result.size() == 1
                ? result.get(0)
                : AuraVotes.getPlainMessage("vote_result_not_certain");

        Component header = AuraVotes.getMessage("vote_end_header", Placeholder.unparsed("vote_result", voteResult));
        Component answers = Component.empty();

        HashMap<String, Integer> frequencyMap = frequency.getFrequencies();
        for (int i = 0; i < possibleAnswers.size(); i++) {
            String answer = possibleAnswers.get(i);
            if (i != 0)
                answers = answers.append(AuraVotes.getMessage("vote_end_answer_delimiter"));

            int answerFrequency = frequencyMap.getOrDefault(answer, 0);
            answers = answers.append(AuraVotes.getMessage("vote_end_answer_format",
                    Placeholder.unparsed("answer", answer),
                    Placeholder.unparsed("answer_frequency", String.valueOf(answerFrequency))
            ));
        }

        Component component = header
                .appendNewline()
                .append(answers);
        Bukkit.broadcast(component);

        if (forceAdmin != null) {
            Bukkit.broadcast(AuraVotes.getMessage("vote_end_force", Placeholder.unparsed("admin", forceAdmin)));
        }

        this.answers.clear();
        this.possibleAnswers.clear();
        this.expiresAt = 0;
    }
}
