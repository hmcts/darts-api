package uk.gov.hmcts.darts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import uk.gov.hmcts.darts.enums.GrantType;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AccessTokenClient {

    private final String tokenUri;
    private final String scope;
    private final String username;
    private final String password;
    private final String clientId;
    private final String clientSecret;

    @Setter
    private boolean enableAccessTokenCache;
    private String cachedAccessToken;

    @SneakyThrows
    public String getAccessToken() {
        if (cachedAccessToken != null && enableAccessTokenCache) {
            return cachedAccessToken;
        }
        Map<String, String> params = Map.of("client_id", clientId,
                                            "client_secret", clientSecret,
                                            "scope", scope,
                                            "grant_type", GrantType.PASSWORD.getValue(),
                                            "username", username,
                                            "password", password
        );
        HttpRequest request = HttpRequest.newBuilder(URI.create(tokenUri))
            .POST(encode(params))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build();

        String response = HttpClient.newHttpClient()
            .send(request, BodyHandlers.ofString())
            .body();

        TokenResponse tokenResponse = new ObjectMapper()
            .readValue(response, TokenResponse.class);

        cachedAccessToken = tokenResponse.accessToken();
        return cachedAccessToken;
    }

    private BodyPublisher encode(Map<String, String> params) {
        String urlEncoded = params.entrySet()
            .stream()
            .map(entry -> new StringJoiner("=")
                .add(entry.getKey())
                .add(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .toString())
            .collect(Collectors.joining("&"));

        return HttpRequest.BodyPublishers.ofString(urlEncoded);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }

}
