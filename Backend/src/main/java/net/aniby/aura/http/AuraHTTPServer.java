package net.aniby.aura.http;

import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import net.aniby.aura.AuraAPI;
import net.aniby.aura.AuraBackend;
import net.aniby.yoomoney.modules.notifications.NotificationHook;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class AuraHTTPServer {
    @Getter
    static HttpServer server;

    public static void start(String hostname, int port, int threads) throws IOException {
        server = HttpServer.create(new InetSocketAddress(hostname, port), 0);
        server.createContext("/link", new AuraHTTPHandler.Link());
        server.createContext("/yoomoney/payment_notifications", new NotificationHook(i ->
                AuraBackend.getDonation().onYooMoneyNotification(i)
        ));
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
        server.setExecutor(threadPoolExecutor);
        server.start();
    }
}
