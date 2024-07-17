package uk.gov.hmcts.darts.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import uk.gov.hmcts.darts.test.common.LogUtil;
import uk.gov.hmcts.darts.test.common.MemoryLogAppender;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.util.List;
import javax.annotation.PostConstruct;

/**
 * Base class for integration tests running with H2 in Postgres compatibility mode.
 * This class also starts a containerised Redis instance
 */
@AutoConfigureWireMock(port = 0, files = "file:src/integrationTest/resources/wiremock")
@SpringBootTest
@ActiveProfiles({"intTest", "h2db", "in-memory-caching"})
public class IntegrationBase  {

    @Autowired
    EntityManager emNonStatic;
    @Autowired
    EntityManagerFactory entityManagerFactoryNonStatic;

    static EntityManager em;

    static EntityManagerFactory emf;
    @PostConstruct
    public void init() {
        em = emNonStatic;
        emf = entityManagerFactoryNonStatic;
    }


//    public static ApplicationContext applicationContext;
//    @Override
//    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        this.applicationContext = applicationContext;
//    }

//    private static final EntityManager em = IntegrationBase.applicationContext.getBean("entityManager",EntityManager.class);;

    private static final List<String> excludedTables = List.of(
        "audit_activity",
        "user_roles_courthouses",
        "hearing_reporting_restrictions",
        "security_group",
        "user_account",
        "external_location_type",
        "object_record_status",
        "automated_task",
        "event_handler",
        "flyway_schema_history",
        "node_register",
        "region",
        "security_permission",
        "security_role",
        "shedlock",
        "transcription_status",
        "transcription_type",
        "transcription_urgency"
    );

    @Transactional
    public static void truncateTables () {
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();
        final Query query = em
            .createNativeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'darts'");
        final List result = query.getResultList();
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        for (Object tableName : result) {
            if (!excludedTables.contains(tableName.toString())) {
                em.createNativeQuery("TRUNCATE TABLE darts." + tableName + " RESTART IDENTITY").executeUpdate();
            }
        }
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        em.getTransaction().commit();
    }

//    @Autowired
//    EntityManager em;
    @Autowired
    protected WireMockServer wireMockServer;

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

    @BeforeAll()
    static void beforeAllTruncateTables() {
        truncateTables();
    }

    @BeforeEach
    void clearDb() {

        dartsDatabase.clearDatabaseInThisOrder();
        wireMockServer.resetAll();
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