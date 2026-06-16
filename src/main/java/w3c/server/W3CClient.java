package w3c.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class W3CClient {
    private final String baseUrl;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public W3CClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    public JsonNode execute(String method, String path, Object body) throws Exception {
        String url = baseUrl + path;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json");

        if ("GET".equalsIgnoreCase(method)) {
            builder.GET();
        } else if ("DELETE".equalsIgnoreCase(method)) {
            builder.DELETE();
        } else if ("POST".equalsIgnoreCase(method)) {
            String jsonBody = body != null ? mapper.writeValueAsString(body) : "{}";
            builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        } else {
            throw new IllegalArgumentException("Unsupported method: " + method);
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP Error " + response.statusCode() + ": " + response.body());
        }

        return mapper.readTree(response.body());
    }
}
