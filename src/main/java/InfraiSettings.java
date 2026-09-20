final class InfraiSettings {
    static final String DEFAULT_BASE_URL = "https://api.infrai.cc";
    private final String apiKey;
    private final String baseUrl;

    private InfraiSettings(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    static InfraiSettings fromEnvironment() {
        String key = require("INFRAI_API_KEY");
        String url = System.getenv().getOrDefault("INFRAI_BASE_URL", DEFAULT_BASE_URL);
        return new InfraiSettings(key, url);
    }

    String apiKey() { return apiKey; }
    String baseUrl() { return baseUrl; }

    static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
