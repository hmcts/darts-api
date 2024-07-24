package uk.gov.hmcts.darts.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.Map;

/**
 * Base class for integration tests running with H2 in Postgres compatibility mode.
 * This class also starts a containerised Redis instance
 */
@AutoConfigureWireMock(port = 0, files = "file:src/integrationTest/resources/wiremock")
@SpringBootTest
@Slf4j
@ActiveProfiles({"intTest", "h2db", "in-memory-caching"})
public class IntegrationBase  {

    private final List<String> SEQUENCES_NO_RESET = List.of(
        "revinfo_seq"
    );

    private final Map<String, String> SEQUENCES_RESET_FROM = Map.of(
        "usr_seq", "500",
        "grp_seq", "500",
        "aut_seq", "500",
        "rpt_seq", "500",
        "evh_seq", "500"
    );

    @Autowired
    EntityManagerFactory entityManagerFactory;
    @Autowired
    UserAccountRepository userAccountRepository;
    @Autowired
    SecurityGroupRepository securityGroupRepository;
    @Autowired
    RetentionPolicyTypeRepository retentionPolicyTypeRepository;
    @Autowired
    EventHandlerRepository eventHandlerRepository;
    @Autowired
    AutomatedTaskRepository automatedTaskRepository;


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

    public void resetSequences () {
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        final Query query = em
            .createNativeQuery("SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'darts'");
        final List result = query.getResultList();

        for (Object seqName : result) {
            //TODO swap and add if else
            if (!SEQUENCES_NO_RESET.contains(seqName.toString())) {
                em.createNativeQuery("ALTER SEQUENCE darts." + seqName + " RESTART").executeUpdate();
            }
            if (SEQUENCES_RESET_FROM.containsKey(seqName.toString())) {
                em.createNativeQuery("ALTER SEQUENCE darts." + seqName + " RESTART WITH " + SEQUENCES_RESET_FROM.get(seqName.toString())).executeUpdate();
            }
        }
        em.getTransaction().commit();
        em.close();
    }

    @Transactional
    public void resetTableWithPredefinedTestData() {

        retentionPolicyTypeRepository.deleteAll(
            retentionPolicyTypeRepository.findByIdGreaterThanEqual(500)
        );

        eventHandlerRepository.deleteAll(
            eventHandlerRepository.findByIdGreaterThanEqual(500)
        );

        automatedTaskRepository.deleteAll(
            automatedTaskRepository.findByIdGreaterThanEqual(500)
        );

        userAccountRepository.deleteAll(
            userAccountRepository.findByIdGreaterThanEqual(500)
        );

        securityGroupRepository.deleteAll(
            securityGroupRepository.findByIdGreaterThanEqual(500)
        );
    }

    @BeforeEach
    void clearDb() {
        log.info("wiremock running on port: {}", wiremockPort);
        resetSequences();
        dartsDatabase.clearDatabaseInThisOrder();
        resetTableWithPredefinedTestData();
    }

    @AfterEach
    void clearTestData() {
        logAppender.reset();
    }

    protected void givenBearerTokenExists(String email) {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("emails", List.of(email))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
