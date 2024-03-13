package net.aniby.aura.http;

import com.sun.net.httpserver.HttpExchange;
import lombok.Getter;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AuraHTTPUtils {
    @Getter
    public static class Path {
        private final String domain;
        private final String[] way;
        private final String method;
        private JSONObject body;

        public Path(HttpExchange exchange) {
            String[] path = exchange.getRequestURI().toString()
                    .replace("https://", "")
                    .replace("http://", "")
                    .split("/");

            this.domain = path[0];
            this.way = Arrays.copyOfRange(path, 1, path.length);
            this.method = exchange.getRequestMethod();

            this.body = new JSONObject();
            if (!Objects.equals(this.method, "GET")) {
                try {
                    InputStreamReader isr =  new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    String value = br.readLine();

                    this.body = (JSONObject) IOHelper.parser.parse(value);
                } catch (IOException | ParseException ignored) {}
            }
        }

        public Map<String, String> getQuery() {
            String[] args = this.way[this.way.length - 1].split("\\?");
            return AuraHTTPUtils.queryToMap(
                    args[args.length - 1]
            );
        }
    }

    public enum Answer {
        OK(200, "OK"),
        FORBIDDEN(403, "Forbidden"),
        NOT_FOUND(404, "Not Found"),
        INVALID_SECRET_KEY(401, "Not authorized "),
        BAD_REQUEST(400, "Bad Request");


        final int code;
        final String message;

        Answer(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getMessage() {
            return new JSONObject(Map.of(
                    (this.code >= 400 ? "error" : "message"), this.message
            )).toJSONString();
        }

        public void handle(HttpExchange exchange) throws IOException {
            String json = this.getMessage();

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(this.code, json.length());
            OutputStream os = exchange.getResponseBody();
            os.write(json.getBytes());
            os.close();
        }
    }

    public static void redirect(HttpExchange exchange, String url) throws IOException {
        exchange.getResponseHeaders().set("Location", url);
        exchange.sendResponseHeaders(302, url.length());
        OutputStream os = exchange.getResponseBody();
        os.write(url.getBytes());
        os.close();
    }

    public static Map<String, String> queryToMap(String query) {
        if (query == null) {
            return null;
        }

        if (query.startsWith("#") || query.startsWith("?"))
            query = query.substring(1);

        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }
}
