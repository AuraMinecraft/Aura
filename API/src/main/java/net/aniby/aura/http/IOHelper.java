package net.aniby.aura.http;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class IOHelper {
    public static final JSONParser parser = new JSONParser();

    public enum RequestType {
        GET, POST, PUT, DELETE
    }

    static HttpClient httpClient = HttpClient.newHttpClient();
    public static HttpResponse<String> generate(String url, Map<String, String> headers, RequestType requestType, Map<String, String> body) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(new URI(url));
        if (headers != null) {
            for (String key : headers.keySet())
                requestBuilder = requestBuilder.header(key, headers.get(key));
        }
        HttpRequest.BodyPublisher publisher = null;
        if (requestType != RequestType.GET)
            publisher = HttpRequest.BodyPublishers.ofString(getFormDataAsString(body));
        switch (requestType) {
            case GET -> requestBuilder = requestBuilder.GET();
            case POST -> requestBuilder = requestBuilder.POST(publisher);
            case PUT -> requestBuilder = requestBuilder.PUT(publisher);
            case DELETE -> requestBuilder.DELETE();
        }
        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> get(String url, Map<String, String> headers) throws URISyntaxException, IOException, InterruptedException {
        return generate(url, headers, RequestType.GET, null);
    }
    public static HttpResponse<String> post(String url, Map<String, String> body) throws URISyntaxException, IOException, InterruptedException {
        return generate(url, Map.of("Content-Type", "application/x-www-form-urlencoded"), RequestType.POST, body);
    }
    public static HttpResponse<String> post(String url, Map<String, String> headers, Map<String, String> body) throws URISyntaxException, IOException, InterruptedException {
        return generate(url, headers, RequestType.POST, body);
    }

    public static JSONObject parse(HttpResponse<String> response) throws ParseException {
        return (JSONObject) parser.parse(response.body());
    }

    private static String getFormDataAsString(Map<String, String> formData) {
        StringBuilder formBodyBuilder = new StringBuilder();
        for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
            if (!formBodyBuilder.isEmpty()) {
                formBodyBuilder.append("&");
            }
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8));
            formBodyBuilder.append("=");
            formBodyBuilder.append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8));
        }
        return formBodyBuilder.toString();
    }
}
