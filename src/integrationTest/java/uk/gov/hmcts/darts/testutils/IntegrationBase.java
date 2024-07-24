package uk.gov.hmcts.darts.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
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
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.LogUtil;
import uk.gov.hmcts.darts.test.common.MemoryLogAppender;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.util.List;

/**
 * Base class for integration tests running with H2 in Postgres compatibility mode.<br>
 * This class also starts a containerised Redis instance.<br>
 *<br>
 * To optimise tests total execution time, the below setup has been introduced:
 * <ul>
 *  <li>
 *     predefined test data created by Liquibase (user accounts, security groups, event handlers, etc...) is not guaranteed
 *     to be deleted and recreated in between test classes execution
 *  </li>
 *  <li>
 *     test data created by tests is deleted before each test. Tables with predefined test data have only rows with id >= SEQUENCE_START_VALUE deleted,
 *     i.e. the data created by tests, not Liquibase
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
 * </ul>
 */
@AutoConfigureWireMock(port = 0, files = "file:src/integrationTest/resources/wiremock")
@SpringBootTest
@Slf4j
@ActiveProfiles({"intTest", "h2db", "in-memory-caching"})
public class IntegrationBase {

    private static final int SEQUENCE_START_VALUE = 500;

    private static final List<String> SEQUENCES_NO_RESET = List.of(
        "revinfo_seq"
    );

    private static final List<String> SEQUENCES_RESET_FROM = List.of(
        "usr_seq",
        "grp_seq",
        "aut_seq",
        "rpt_seq",
        "evh_seq"
    );

    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    protected UserAccountRepository userAccountRepository;
    @Autowired
    protected SecurityGroupRepository securityGroupRepository;
    @Autowired
    protected RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    @Autowired
    protected EventHandlerRepository eventHandlerRepository;
    @Autowired
    protected AutomatedTaskRepository automatedTaskRepository;

    @Autowired
    protected OpenInViewUtil openInViewUtil;
    @Autowired
    protected DartsDatabaseStub dartsDatabase;
    @Autowired
    protected ObjectMapper objectMapper;

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
        resetSequences();
        dartsDatabase.clearDatabaseInThisOrder();
        resetTablesWithPredefinedTestData();
    }

    @AfterEach
    void clearTestData() {
        logAppender.reset();
    }

    public void resetSequences() {
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getTransaction().begin();
            final Query query = em.createNativeQuery("SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'darts'");
            final List sequences = query.getResultList();
            for (Object seqName : sequences) {
                if (SEQUENCES_RESET_FROM.contains(seqName.toString())) {
                    em.createNativeQuery("ALTER SEQUENCE darts." + seqName + " RESTART WITH " + SEQUENCE_START_VALUE).executeUpdate();
                } else if (!SEQUENCES_NO_RESET.contains(seqName.toString())) {
                    em.createNativeQuery("ALTER SEQUENCE darts." + seqName + " RESTART").executeUpdate();
                }
            }
            em.getTransaction().commit();
        }
    }

    @Transactional
    public void resetTablesWithPredefinedTestData() {

        retentionPolicyTypeRepository.deleteAll(
            retentionPolicyTypeRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE)
        );

        eventHandlerRepository.deleteAll(
            eventHandlerRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE)
        );

        automatedTaskRepository.deleteAll(
            automatedTaskRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE)
        );

        userAccountRepository.deleteAll(
            userAccountRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE)
        );

        securityGroupRepository.deleteAll(
            securityGroupRepository.findByIdGreaterThanEqual(SEQUENCE_START_VALUE)
        );
    }

    protected void givenBearerTokenExists(String email) {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("emails", List.of(email))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
