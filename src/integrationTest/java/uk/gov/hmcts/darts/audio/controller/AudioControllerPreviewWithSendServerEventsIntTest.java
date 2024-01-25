package uk.gov.hmcts.darts.audio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;
import uk.gov.hmcts.darts.audio.service.AudioTransformationServiceGivenBuilder;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"intTest", "h2db", "in-memory-caching"})
@DirtiesContext
class AudioControllerPreviewWithSendServerEventsIntTest {

    public static final String RANGE_1024 = "bytes=0-1024";
    public static final String RANGE = "range";
    public static final String URL = "/audio/preview/";
    public static final int TIMEOUT = 100_000;

    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    private AudioTransformationServiceGivenBuilder given;
    @MockBean
    private Authorisation authorisation;
    private MediaEntity mediaEntity;
    @Autowired
    private WebTestClient webClient;

    @BeforeEach
    void setupData() {
        given.getDartsDatabase().clearDatabaseInThisOrder();
        given.setupTest();
        mediaEntity = given.getMediaEntity1();
        given.externalObjectDirForMedia(mediaEntity);
        doNothing().when(authorisation).authoriseByMediaId(
            mediaEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA, RCJ_APPEALS)
        );
        webClient = webClient.mutate()
            .responseTimeout(Duration.ofMillis(TIMEOUT))
            .build();

    }

    @Test
    void previewShouldReturnSuccess() {
        FluxExchangeResult<String> result = webClient.get().uri(uriBuilder -> uriBuilder.path(
                URL + mediaEntity.getId()).build())
            .header(RANGE, RANGE_1024)
            .accept(TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk().returnResult(String.class);

        StepVerifier.create(result.getResponseBody())
            .recordWith(ArrayList::new)
            .thenConsumeWhile(x -> true)
            .consumeRecordedWith(elements -> assertThat(elements.isEmpty()).isFalse())
            .verifyComplete();
    }

    @Test
    void previewShouldReturnError() {
        webClient.get().uri(uriBuilder -> uriBuilder.path(
                URL + -1).build())
            .header(RANGE, RANGE_1024)
            .accept(TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().is5xxServerError().returnResult(String.class);
    }

}
