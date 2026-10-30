package uk.gov.hmcts.darts.commonplatform.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.commonplatform.http.api.CommonPlatformApi;

@RestController
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public class CommonPlatformController implements CommonPlatformApi {

}
