package uk.gov.hmcts.darts.testutils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

public class TokenStub {

    @SuppressWarnings("checkstyle:linelength")
    public void stubExternalToken() {
        stubFor(post(urlPathEqualTo("/token"))
                    .willReturn(aResponse()
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                                                          {"access_token":"%s","token_type":"Bearer","expires_in":"3600"}
                                                      """.formatted(getStubbedToken()))));
    }

    /** Return a legitimate darts api token */
    public String getStubbedToken() {
        return """ 
        eyJhbGciOiJSUzI1NiIsImtpZCI6Ilg1ZVhrNHh5b2pORnVtMWtsMll0djhkbE5QNC1jNTdkTzZRR1RWQndhTmsiLCJ0eXAiOiJKV1QifQ.eyJhdWQiOiIwNTNkNDRmOS1jZDI3LTQ2M2UtYTk5NS1iY2Y4MzUyMzNjZjgiLCJpc3MiOiJodHRwczovL2htY3RzZGFydHNiMmNzYm94LmIyY2xvZ2luLmNvbS84OWI4ZGUyZS1lYjFiLTQyZmItYTU5Yy04MTNlYTJiODJhNTYvdjIuMC8iLCJleHAiOjE3MjMwMjczOTAsIm5iZiI6MTcyMzAyMzc5MCwiaWRwIjoiTG9jYWxBY2NvdW50Iiwib2lkIjoiMmM5MGUwZjgtYWFjYS00MjIyLWJmNGMtNzc5MWU0ODBlZDFiIiwic3ViIjoiMmM5MGUwZjgtYWFjYS00MjIyLWJmNGMtNzc5MWU0ODBlZDFiIiwiZW1haWxzIjpbImRhcnRzYXV0b21hdGlvbnVzZXJASE1DVFMubmV0Il0sInRmcCI6IkIyQ18xX3JvcGNfZGFydHNfc2lnbmluIiwic2NwIjoiRnVuY3Rpb25hbC5UZXN0IiwiYXpwIjoiMDUzZDQ0ZjktY2QyNy00NjNlLWE5OTUtYmNmODM1MjMzY2Y4IiwidmVyIjoiMS4wIiwiaWF0IjoxNzIzMDIzNzkwfQ.QuCHuEdjAcx4-X2hKs5ffaZ986XUQF37Le0bkFjIbdojhYbPGSmNDtwvQmX57GEhJKZSSOae8jswSIINQzaATgkIlEcNxtPpjZZnDV-mAq4Yy16xiADUHBUyMGxxySbYkE4GMMOm0ztzM5WED6QMPek72ePkYc7E_zfcKAXe-C6wPm5pTQa6nOTdOTdrE0fhXaQ7TohfN04GMltC-ezo1FAY5mOitvnsWxKUZvVWjR9igORdZ7i5Q4vwmVYwkLBW8hd1mQNWz_1Tk0d040zNl5vTHEtHRvrF_R_5axtkkcUfmuHtBYlw12NfikUQE9_v_9byehcnNSJMPI1Kl6GWuw
        """;
    }

    @SuppressWarnings("checkstyle:linelength")
    public void stubExternalJwksKeys() {
        stubFor(get(urlPathEqualTo("/keys"))
                    .willReturn(aResponse()
                                    .withHeader("Content-Type", "application/json")
                                    .withBody("""
                                                      {"keys":[{"kid":"X5eXk4xyojNFum1kl2Ytv8dlNP4-c57dO6QGTVBwaNk","nbf":1493763266,"use":"sig","kty":"RSA","e":"AQAB","n":"tVKUtcx_n9rt5afY_2WFNvU6PlFMggCatsZ3l4RjKxH0jgdLq6CScb0P3ZGXYbPzXvmmLiWZizpb-h0qup5jznOvOr-Dhw9908584BSgC83YacjWNqEK3urxhyE2jWjwRm2N95WGgb5mzE5XmZIvkvyXnn7X8dvgFPF5QwIngGsDG8LyHuJWlaDhr_EPLMW4wHvH0zZCuRMARIJmmqiMy3VD4ftq4nS5s8vJL0pVSrkuNojtokp84AtkADCDU_BUhrc2sIgfnvZ03koCQRoZmWiHu86SuJZYkDFstVTVSR0hiXudFlfQ2rOhPlpObmku68lXw-7V-P7jwrQRFfQVXw"}]}
                                                      """.trim())));
    }

    public void verifyNumberOfTimesKeysObtained(int numberOfTimes) {
        verify(exactly(numberOfTimes), getRequestedFor(urlPathEqualTo("/keys")));
    }
}