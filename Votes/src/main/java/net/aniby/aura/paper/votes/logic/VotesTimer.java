package net.aniby.aura.paper.votes.logic;

import net.aniby.aura.paper.votes.AuraVotes;
import org.bukkit.Bukkit;

import java.util.Timer;
import java.util.TimerTask;

public class VotesTimer extends Timer {
    public VotesTimer() {
        super("VoteTimer");
    }

    private final TimerTask task = new TimerTask() {
        @Override
        public void run() {
            AuraVotes instance = AuraVotes.getInstance();
            AuraCollection collection = instance.getActiveCollection();
            if (collection != null && collection.isExpired())
                Bukkit.getScheduler().runTask(AuraVotes.getInstance(), () -> collection.end(null));
            Vote vote = instance.getActiveVote();
            if (vote != null && vote.isExpired())
                Bukkit.getScheduler().runTask(AuraVotes.getInstance(), () -> vote.end(null));
        }
    };

    public void start(long period) {
        this.scheduleAtFixedRate(task, 30, period);
    }
}
