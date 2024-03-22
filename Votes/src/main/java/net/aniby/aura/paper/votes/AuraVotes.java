package net.aniby.aura.paper.votes;

import co.aikar.commands.PaperCommandManager;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.aniby.aura.paper.votes.commands.CollectionCommand;
import net.aniby.aura.paper.votes.commands.VoteCommand;
import net.aniby.aura.paper.votes.logic.AuraCollection;
import net.aniby.aura.paper.votes.logic.Vote;
import net.aniby.aura.paper.votes.logic.VotesTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class AuraVotes extends JavaPlugin {
    @Getter
    private static AuraVotes instance;
    @Setter
    @Getter
    private volatile AuraCollection activeCollection = null;
    @Getter
    @Setter
    private volatile Vote activeVote = new Vote();
    private final VotesTimer votesTimer = new VotesTimer();


    public static String getPlainMessage(String path) {
        return instance.getConfig().getConfigurationSection("messages")
                        .getString(path);
    }

    public static Component getMessage(String path, TagResolver... tags) {
        return miniMessage.deserialize(
                instance.getConfig().getConfigurationSection("messages")
                        .getString(path),
                tags
        );
    }

    public static Component getMessage(String path, List<TagResolver> tags) {
        return getMessage(path, tags.toArray(new TagResolver[0]));
    }

    @Getter
    private static final MiniMessage miniMessage = MiniMessage.builder()
            .tags(StandardTags.defaults())
            .build();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        votesTimer.start(getConfig().getLong("check_timer_period"));

        PaperCommandManager manager = new PaperCommandManager(this);
        manager.registerCommand(new VoteCommand());
        manager.registerCommand(new CollectionCommand());
    }

    @Override
    public void onDisable() {
        votesTimer.cancel();
    }
}
