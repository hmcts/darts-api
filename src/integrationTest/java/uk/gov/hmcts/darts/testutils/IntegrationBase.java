package uk.gov.hmcts.darts.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import uk.gov.hmcts.darts.authentication.component.DartsJwt;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.task.api.AutomatedTasksApi;
import uk.gov.hmcts.darts.task.runner.AutomatedOnDemandTask;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;
import uk.gov.hmcts.darts.test.common.AwaitabilityUtil;
import uk.gov.hmcts.darts.test.common.FileStore;
import uk.gov.hmcts.darts.test.common.LogUtil;
import uk.gov.hmcts.darts.test.common.MemoryLogAppender;
import uk.gov.hmcts.darts.test.common.data.UserAccountTestData;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseRetrieval;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;
import uk.gov.hmcts.darts.testutils.stubs.wiremock.TokenStub;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Base class for integration tests running with H2 in Postgres compatibility mode.<br>
 * This class also starts a containerised Redis instance.<br>
 * <br>
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
 * <br>
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
@SpringBootTest()
@Slf4j
@ActiveProfiles({"intTest", "h2db", "in-memory-caching"})
@Import(IntegrationTestConfiguration.class)
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
    @Autowired
    private List<AutomatedOnDemandTask> automatedOnDemandTask;
    @Autowired
    private AutomatedTasksApi automatedTasksApi;

    protected TokenStub tokenStub = new TokenStub();

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
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    void clearTestData() throws Exception {
        logAppender.reset();
        FileStore.getFileStore().remove();
        checkCleanup();
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    protected void checkCleanup() throws Exception {

    }

    protected void givenBearerTokenExists(String email) {
        Optional<UserAccountEntity> userAccount = dartsDatabase.getUserAccountRepository().findFirstByEmailAddressIgnoreCase(email);
        if (userAccount.isEmpty()) {
            UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
            dartsDatabase.getUserAccountRepository().save(userAccountEntity);
            userAccount = Optional.of(userAccountEntity);
        }
        DartsJwt jwt = new DartsJwt(
            Jwt.withTokenValue("test")
                .header("alg", "RS256")
                .claim("emails", List.of(email))
                .build(),
            userAccount.get().getId());
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    protected void waitForOnDemandTaskToReady() {
        AwaitabilityUtil.waitForMaxWithOneSecondPoll(() -> {
            for (AutomatedOnDemandTask onDemandTask : automatedOnDemandTask) {
                var taskName = onDemandTask.getTaskName();
                Optional<AutomatedTaskEntity> automatedTaskEntity = automatedTasksApi.getTaskByName(taskName);
                if (automatedTaskEntity.isPresent() && !automatedTasksApi.isLocked(automatedTaskEntity.get())) {
                    return true;
                }
            }

            return false;
        }, Duration.ofSeconds(30));
    }

    protected void waitForOnDemandToComplete() {
        AwaitabilityUtil.waitForMaxWithOneSecondPoll(() -> {
            for (AutomatedOnDemandTask onDemandTask : automatedOnDemandTask) {
                if (onDemandTask.getAutomatedTaskStatus().equals(AutomatedTaskStatus.COMPLETED)) {
                    return true;
                }
            }

            return false;
        }, Duration.ofSeconds(30));
    }

    protected void anAuthenticatedUserFor(String userEmail) {
        GivenBuilder.anAuthenticatedUserFor(userEmail, dartsDatabase.getUserAccountRepository());
    }
}