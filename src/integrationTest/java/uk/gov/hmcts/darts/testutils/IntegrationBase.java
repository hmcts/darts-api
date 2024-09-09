package uk.gov.hmcts.darts.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import uk.gov.hmcts.darts.common.util.RequestFileStore;
import uk.gov.hmcts.darts.test.common.LogUtil;
import uk.gov.hmcts.darts.test.common.MemoryLogAppender;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseRetrieval;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.util.List;

/**
 * Base class for integration tests running with H2 in Postgres compatibility mode.<br>
 * This class also starts a containerised Redis instance.<br>
 *<br>
 * To optimise tests total execution time, the below setup has been introduced:
 * <ul>
 *  <li>
 *     predefined test data created by Flyway (user accounts, security groups, event handlers, etc...) is not guaranteed
 *     to be deleted and recreated in between test classes execution
 *  </li>
 *  <li>
 *     test data created by tests is deleted before each test. Tables with predefined test data have only rows with id >= SEQUENCE_START_VALUE deleted,
 *     i.e. the data created by tests, not Flyway
 *  </li>
 *  <li>
 *     sequences are reset to either SEQUENCE_START_VALUE or their initial value (depending on the type of data, if predefined or not) before each test
 *  </li>
 * </ul>
 *<br>
 * Based on the above, please follow the following recommendations when writing integration tests:
 * <ul>
 *  <li>
 *     do not permanently modify predefined test data (e.g. changing a security group global flag, disabling an automated task, etc...)
 *  </li>
 *  <li>
 *     setup test data in a @BeforeEach rather than a @BeforeAll
 *  </li>
 *  <li>
 *      when creating test data with manually assigned ids use ids >= SEQUENCE_START_VALUE so that data is automatically deleted
 *  </li>
 *  <li>
 *      avoid directly hardcoding primary key ids in assertions, since those might change. Instead retrieve the id from the created test data entities.
 *      e.g. rather than assertThat(persistedUserGroup.createdBy.getId(), equalsTo(0)), use
 *      assertThat(persistedUserGroup.createdBy.getId(), equalsTo(integrationTestUser.getId()))
 *  </li>
 * </ul>
 */
@AutoConfigureWireMock(port = 0, files = "file:src/integrationTest/resources/wiremock")
@SpringBootTest
@Slf4j
@ActiveProfiles({"intTest", "h2db", "in-memory-caching"})
public class IntegrationBase {

    @Autowired
    protected OpenInViewUtil openInViewUtil;
    @Autowired
    protected DartsDatabaseStub dartsDatabase;
    @Autowired
    protected DartsPersistence dartsPersistence;
    @Autowired
    protected DartsDatabaseRetrieval dartsDataRetrieval;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected TransactionalUtil transactionalUtil;

    @Value("${wiremock.server.port}")
    protected String wiremockPort;

    protected MemoryLogAppender logAppender = LogUtil.getMemoryLogger();

    private static final GenericContainer<?> REDIS = new GenericContainer<>(
        "redis:7.2.4-alpine"
    ).withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379).toString());
    }

    static {
        // container will be automatically stopped
        REDIS.start();
    }

    @BeforeEach
    void clearDb() {
        dartsDatabase.resetSequences();
        dartsDatabase.clearDatabaseInThisOrder();
        dartsDatabase.resetTablesWithPredefinedTestData();
    }

    @AfterEach
    void clearTestData() {
        logAppender.reset();
        RequestFileStore.getFileStore().remove();
    }

    protected void givenBearerTokenExists(String email) {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("emails", List.of(email))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}