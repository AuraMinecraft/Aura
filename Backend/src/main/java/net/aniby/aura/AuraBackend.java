package net.aniby.aura;

import lombok.Getter;
import net.aniby.aura.discord.CommandHandler;
import net.aniby.aura.discord.DiscordIRC;
import net.aniby.aura.discord.JoinForm;
import net.aniby.aura.discord.commands.*;
import net.aniby.aura.donate.AuraDonation;
import net.aniby.aura.http.AuraHTTPServer;
import net.aniby.aura.modules.AuraUser;
import net.aniby.aura.module.CAuraUser;
import net.aniby.aura.tool.AuraUtils;
import net.aniby.aura.tool.ConsoleColors;
import net.aniby.aura.twitch.TwitchBot;
import ninja.leaping.configurate.ConfigurationNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Logger;

public class AuraBackend {
    // Velocity
    @Getter
    private static final Logger logger = Logger.getLogger("Aura");
    // Aura
    @Getter
    private static TwitchBot twitch;
    @Getter
    private static AuraConfig config = null;
    @Getter
    private static DiscordIRC discord;
    @Getter
    private static CommandHandler handler;
    @Getter
    private static AuraDonation donation;

    public static Path workingDirectory() {
        return new File("").toPath();
    }

    public static String getExternalURL() {
        String url = config.getRoot().getNode("http_server", "external_url").getString();
        if (!url.endsWith("/"))
            url += "/";

        return url;
    }

    public static void main(String[] args) {
        try {
            // All from config
            File configFile = new File("config.yml");
            AuraUtils.saveDefaultFile(configFile, "config.yml", AuraAPI.class);
            config = new AuraConfig(configFile);
            logger.info(ConsoleColors.GREEN + "Config loaded!" + ConsoleColors.WHITE);

            ConfigurationNode node = config.getRoot().getNode("mysql");
            AuraAPI.init(
                    node.getNode("connection_url").getString(),
                    node.getNode("login").getString(),
                    node.getNode("password").getString()
            );
            logger.info(ConsoleColors.GREEN + "Database connected!" + ConsoleColors.WHITE);

            twitch = new TwitchBot(config.getRoot().getNode("twitch", "application"));
            logger.info(ConsoleColors.GREEN + "Twitch4J Module initialized!" + ConsoleColors.WHITE);
            discord = new DiscordIRC();
            logger.info(ConsoleColors.GREEN + "JDA Module initialized!" + ConsoleColors.WHITE);

            // Users init
            for (CAuraUser u : AuraAPI.getDatabase().getUsers().queryBuilder()
                    .where()
                    .isNotNull("refresh_token")
                    .query()) {
                AuraUser user = AuraUser.cast(u);
                user.init();
            }
            logger.info(ConsoleColors.GREEN + "Users initialized!" + ConsoleColors.WHITE);

            // Commands
            handler = new CommandHandler(discord.getJda());
            handler.registerCommands(
                    new AuraCommand(),
                    new LinkCommand(),
                    new ForceLinkCommand(),
                    new UnlinkCommand(),
                    new AuraLinkCommand(),
                    new ProfileCommand(),
                    new DonateCommand()
            );
            handler.confirm();
            handler.registerButton(JoinForm.FORM_CREATE, JoinForm::buttonFormCreate);
            handler.registerModal(JoinForm.FORM_SUBMIT, JoinForm::modalFormSubmit);
            handler.registerButton(JoinForm.FORM_ACCEPT, JoinForm::buttonFormAccept);
            handler.registerButton(JoinForm.FORM_DECLINE, JoinForm::buttonFormDecline);
            logger.info(ConsoleColors.GREEN + "Commands registered!" + ConsoleColors.WHITE);

            // YooMoney
            donation = new AuraDonation();
            logger.info(ConsoleColors.GREEN + "Donates linked!" + ConsoleColors.WHITE);

            // HTTP Server
            ConfigurationNode serverNode = config.getRoot().getNode("http_server");
            AuraHTTPServer.start(
                    serverNode.getNode("hostname").getString(),
                    serverNode.getNode("port").getInt(),
                    serverNode.getNode("threads").getInt()
            );
            logger.info(ConsoleColors.GREEN + "HTTP-Server opened!" + ConsoleColors.WHITE);

            // On shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(AuraBackend::onShutdown));
        } catch (IOException e) {
            logger.info("Config can't be loaded! Disabling...");
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void onShutdown() {
        try {
            logger.info("Shutdown...");
            twitch.getClient().close();
            logger.info("Twitch Connection closed");
            AuraAPI.getDatabase().disconnect();
            logger.info("Disconnected from database");
        } catch (Exception ignored) {}
    }
}