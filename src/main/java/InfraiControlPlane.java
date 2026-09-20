import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

final class InfraiControlPlane {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String baseUrl;
    private final String apiKey;

    InfraiControlPlane(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    String post(String path, String json) throws IOException, InterruptedException {
        return send("POST", path, Optional.of(json));
    }

    String delete(String path) throws IOException, InterruptedException {
        return send("DELETE", path, Optional.empty());
    }

    private String send(String method, String path, Optional<String> json) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.BodyPublisher body = json.map(HttpRequest.BodyPublishers::ofString)
                    .orElse(HttpRequest.BodyPublishers.noBody());
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .method(method, body)
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Envelope envelope = Envelope.decode(response.body());
            if (!envelope.ok()) {
                if (response.statusCode() == 429 && attempt < 3) {
                    Thread.sleep(retryDelay(response, attempt));
                    continue;
                }
                throw new InfraiResponseException(envelope.error(), response.statusCode());
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport response " + response.statusCode());
            }
            return envelope.data();
        }
        throw new IOException("Retry attempts exhausted");
    }

    private static long retryDelay(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("");
        try {
            return Long.parseLong(retryAfter) * 1000L;
        } catch (NumberFormatException ignored) {
            return 250L << attempt;
        }
    }

    record Envelope(boolean ok, String data, String error) {
        static Envelope decode(String json) throws IOException {
            String compact = json == null ? "" : json.trim();
            if (!compact.startsWith("{") || !compact.endsWith("}")) {
                throw new IOException("Response was not an envelope");
            }
            boolean ok = compact.matches("(?s).*\\\"ok\\\"\\s*:\\s*true.*");
            String data = member(compact, "data").orElse("null");
            String error = member(compact, "error").orElse("unknown Infrai error");
            return new Envelope(ok, data, error);
        }

        private static Optional<String> member(String json, String name) {
            String marker = "\"" + name + "\"";
            int key = json.indexOf(marker);
            if (key < 0) return Optional.empty();
            int colon = json.indexOf(':', key + marker.length());
            if (colon < 0) return Optional.empty();
            int end = json.indexOf(',', colon + 1);
            return Optional.of(json.substring(colon + 1, end < 0 ? json.length() - 1 : end).trim());
        }
    }

    static final class InfraiResponseException extends IOException {
        InfraiResponseException(String error, int status) {
            super("Infrai response " + status + ": " + error);
        }
    }
}
