package net.aniby.aura.paper.votes.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import net.aniby.aura.paper.votes.AuraVotes;
import net.aniby.aura.paper.votes.logic.Vote;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

@CommandAlias("vote")
public class VoteCommand extends BaseCommand {
    @Subcommand("set")
    @CommandPermission("aura.votes.vote.create")
    public void setAnswers(CommandSender sender, String[] answers) {
        Vote vote = AuraVotes.getInstance().getActiveVote();
        if (vote == null) {
            sender.sendMessage(
                    AuraVotes.getMessage("vote_not_exists")
            );
            return;
        }
        if (vote.isStarted()) {
            sender.sendMessage(
                    AuraVotes.getMessage("vote_exists")
            );
            return;
        }
        vote.setPossibleAnswers(Arrays.stream(answers).collect(Collectors.toCollection(ArrayList::new)));
        sender.sendMessage(
                AuraVotes.getMessage("vote_answers_set")
        );
    }

    @Subcommand("start")
    @CommandPermission("aura.votes.vote.create")
    public void start(CommandSender sender, long minutes, String message) {
        String admin = sender.getName();

        Vote vote = AuraVotes.getInstance().getActiveVote();
        if (vote == null) {
            sender.sendMessage(
                    AuraVotes.getMessage("vote_not_exists")
            );
            return;
        }
        if (vote.isStarted()) {
            sender.sendMessage(
                    AuraVotes.getMessage("vote_exists")
            );
            return;
        }
        if (vote.getPossibleAnswers().isEmpty()) {
            sender.sendMessage(
                    AuraVotes.getMessage("vote_no_possible_answers")
            );
            return;
        }

        long expiresAt = new Date().getTime() + minutes * 60 * 1000L;
        vote.start(admin, message, expiresAt);
        sender.sendMessage(
                AuraVotes.getMessage("vote_start")
        );
    }
    
    @Subcommand("end")
    @CommandPermission("aura.votes.vote.end")
    public void end(CommandSender sender) {
        Vote vote = AuraVotes.getInstance().getActiveVote();
        if (vote == null || !vote.isStarted()) {
            sender.sendMessage(
                    AuraVotes.getMessage("vote_not_exists")
            );
            return;
        }
        
        vote.end(sender.getName());
    }

    @Subcommand("answer")
    public void answer(Player player, String answer) {
        Vote vote = AuraVotes.getInstance().getActiveVote();
        if (vote == null || !vote.isStarted()) {
            player.sendMessage(
                    AuraVotes.getMessage("vote_not_exists")
            );
            return;
        }

        if (!vote.getPossibleAnswers().contains(answer)) {
            player.sendMessage(
                    AuraVotes.getMessage("vote_invalid_answer")
            );
            return;
        }

        String playerName = player.getName();
        if (vote.getAnswers().containsKey(playerName)) {
            player.sendMessage(
                    AuraVotes.getMessage("vote_already_voted")
            );
            return;
        }

        vote.getAnswers().put(playerName, answer);
        player.sendMessage(
                AuraVotes.getMessage("vote_answer", Placeholder.unparsed("answer", answer))
        );
    }
}
