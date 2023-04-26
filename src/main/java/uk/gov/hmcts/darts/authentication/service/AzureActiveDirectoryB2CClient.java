package uk.gov.hmcts.darts.authentication.service;

import feign.Headers;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "AzureActiveDirectoryB2CClient",
    url = "${spring.security.oauth2.client.provider.external-azure-ad-provider.token-uri}")
@Headers({"Content-Type: application/x-www-form-urlencoded"})
public interface AzureActiveDirectoryB2CClient {
    @PostMapping
    Response fetchAccessToken(Map<String, ?> queryMap);
}
