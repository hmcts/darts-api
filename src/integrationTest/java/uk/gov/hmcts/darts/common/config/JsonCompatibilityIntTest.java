package uk.gov.hmcts.darts.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AdminMediaCaseResponseItem;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.exception.DartsApiExceptionHandler;
import uk.gov.hmcts.darts.event.model.DarNotifyEvent;
import uk.gov.hmcts.darts.retention.model.CaseRetentionConfidenceReason;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JsonCompatibilityIntTest.JsonConfiguration.class, properties = "spring.flyway.enabled=false")
class JsonCompatibilityIntTest {

    private static final String NOTIFICATION = """
        {
          "notification_url": "https://example.invalid/notifications",
          "notification_type": "Hearing started",
          "timestamp": "2026-11-15T12:34:56.123456Z",
          "courthouse": "TEST COURT",
          "courtroom": "1",
          "case_numbers": ["CASE001", "CASE002"]
        }
        """;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    @Qualifier("caseDocumentObjectMapper")
    private ObjectMapper caseDocumentObjectMapper;
    @Autowired
    private RequestMappingHandlerAdapter handlerAdapter;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private RestTemplate restTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @SuppressWarnings("removal") // Verify that MVC continues to select the Jackson 2 compatibility converter.
    void given_applicationContext_when_selectingJsonConverter_then_usePrimaryJackson2Mapper() {
        var converter = handlerAdapter.getMessageConverters().stream()
            .filter(candidate -> candidate.canRead(DarNotifyEvent.class, MediaType.APPLICATION_JSON))
            .findFirst().orElseThrow();

        assertThat(converter).isInstanceOf(MappingJackson2HttpMessageConverter.class);
        assertThat(((MappingJackson2HttpMessageConverter) converter).getObjectMapper()).isSameAs(objectMapper);
    }

    @Test
    @SuppressWarnings("removal") // Verify the imperative HTTP client also selects the application's Jackson 2 mapper.
    void given_restTemplate_when_selectingJsonConverter_then_usePrimaryJackson2Mapper() {
        var converter = restTemplate.getMessageConverters().stream()
            .filter(candidate -> candidate.canRead(DarNotifyEvent.class, MediaType.APPLICATION_JSON))
            .findFirst().orElseThrow();

        assertThat(converter).isInstanceOf(MappingJackson2HttpMessageConverter.class);
        assertThat(((MappingJackson2HttpMessageConverter) converter).getObjectMapper()).isSameAs(objectMapper);
    }

    @Test
    void given_notificationPayload_when_roundTrippingThroughHttp_then_preserveEveryProperty() throws Exception {
        String response = mockMvc.perform(post("/json-compatibility")
                .contentType(MediaType.APPLICATION_JSON).content(NOTIFICATION))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(response)).isEqualTo(objectMapper.readTree(NOTIFICATION));
        DarNotifyEvent event = objectMapper.readValue(response, DarNotifyEvent.class);
        assertThat(event.getTimestamp().getNano()).isEqualTo(123_456_000);
        assertThat(event.getCaseNumbers()).containsExactly("CASE001", "CASE002");
    }

    @Test
    void given_problemDetails_when_returningHttpError_then_keepExtensionsAtTopLevel() throws Exception {
        String response = mockMvc.perform(get("/json-compatibility/problem"))
            .andExpect(status().isBadRequest())
            .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(response)).isEqualTo(objectMapper.readTree("""
            {"type":"COMMON_104", "title":"Invalid request", "status":400,
             "instance":"/json-compatibility/problem", "case_number":"must not be blank"}
            """));
    }

    @Test
    void given_nestedArmMetadata_when_roundTripping_then_preserveCasingAndNestedValues() throws Exception {
        String payload = """
            {
              "UseGuidsForFields": false,
              "itemId": "item-123",
              "manifest": {
                "event_date": "2026-11-15T12:34:56Z",
                "ret_conf_score": 0,
                "ret_conf_reason": "Retention confirmed"
              }
            }
            """;

        UpdateMetadataRequest request = objectMapper.readValue(payload, UpdateMetadataRequest.class);

        assertThat(request.getManifest().getRetConfScore()).isZero();
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(request))).isEqualTo(objectMapper.readTree(payload));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"case_number\":\"CASE001\",\"source\":\"Add Audio Metadata\"}",
        "{\"id\":null,\"case_number\":\"CASE001\",\"source\":\"Add Audio Metadata\"}",
        "{\"id\":123,\"case_number\":\"CASE001\",\"source\":\"Add Audio Metadata\"}"
    })
    void given_generatedModel_when_roundTripping_then_preserveNullablePresenceAndEnumWireValue(String payload) throws Exception {
        var expected = objectMapper.readTree(payload);
        var response = objectMapper.readValue(payload, AdminMediaCaseResponseItem.class);

        assertThat(response.getId().isPresent()).isEqualTo(expected.has("id"));
        assertThat(response.getSource()).isEqualTo(AdminMediaCaseResponseItem.SourceEnum.ADD_AUDIO_METADATA);
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(response))).isEqualTo(expected);
    }

    @Test
    void given_unknownNotificationProperty_when_readingHttpRequest_then_preserveStrictBinding() throws Exception {
        mockMvc.perform(post("/json-compatibility").contentType(MediaType.APPLICATION_JSON)
                .content("{\"unexpected_property\":\"must not disappear silently\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void given_retentionCaseData_when_roundTripping_then_preserveNestedCaseProperties() throws Exception {
        String payload = """
            {
              "ret_conf_applied_ts": "2026-11-15T12:34:56Z",
              "cases": [{
                "courthouse": "TEST COURT",
                "case_number": "CASE001",
                "ret_conf_updated_ts": "2026-11-15T12:34:56.123456Z",
                "ret_conf_reason": "Retention confirmed"
              }]
            }
            """;

        var reason = objectMapper.readValue(payload, CaseRetentionConfidenceReason.class);

        assertThat(reason.getRetentionCases()).hasSize(1);
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(reason))).isEqualTo(objectMapper.readTree(payload));
    }

    @Test
    void given_nullProperties_when_usingDifferentMappers_then_preserveExistingInclusionPolicies() throws Exception {
        DarNotifyEvent event = DarNotifyEvent.builder().notificationType("Hearing started").build();

        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(event)))
            .isEqualTo(objectMapper.readTree("{\"notification_type\":\"Hearing started\"}"));
        assertThat(caseDocumentObjectMapper.readTree(caseDocumentObjectMapper.writeValueAsString(event)))
            .isEqualTo(objectMapper.readTree("""
                {"notification_type":"Hearing started", "notification_url":null, "timestamp":null,
                 "courthouse":null, "courtroom":null, "case_numbers":null}
                """));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class})
    @Import({ObjectMapperConfig.class, EchoController.class, DartsApiExceptionHandler.class})
    static class JsonConfiguration {
        @Bean
        @SuppressWarnings("PMD.SignatureDeclareThrowsException")
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable()).build();
        }
    }

    @RestController
    static class EchoController {
        @PostMapping("/json-compatibility")
        DarNotifyEvent echo(@RequestBody DarNotifyEvent event) {
            return event;
        }

        @GetMapping("/json-compatibility/problem")
        void problem() {
            throw new DartsApiException(CommonApiError.BAD_REQUEST, Map.of("case_number", "must not be blank"));
        }
    }
}
