package uk.gov.hmcts.darts.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
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
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.LogUtil;
import uk.gov.hmcts.darts.test.common.MemoryLogAppender;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;

/**
 * Base class for integration tests running with H2 in Postgres compatibility mode.
 * This class also starts a containerised Redis instance
 */
@AutoConfigureWireMock(port = 0, files = "file:src/integrationTest/resources/wiremock")
@SpringBootTest
@Slf4j
@ActiveProfiles({"intTest", "h2db", "in-memory-caching"})
public class IntegrationBase  {

    public static final List<String> PREDEFINED_SECURITY_GROUPS = List.of(
        "Mid Tier Group",
        "Dar Pc Group",
        "Cpp Group",
        "Xhibit Group",
        "Test RCJ Appeals",
        "Test Language Shop",
        "Test Transcriber",
        "Test Judge",
        "Test Judge Global",
        "Test Requestor",
        "Test Approver",
        "SUPER_ADMIN",
        "SUPER_USER",
        "DARTS",
        "MEDIA_IN_PERPETUITY"
    );
    @Autowired
    EntityManagerFactory entityManagerFactoryNonStatic;
    @Autowired
    UserAccountRepository userAccountRepository;
    @Autowired
    SecurityGroupRepository securityGroupRepository;
    @Autowired
    RetentionPolicyTypeRepository retentionPolicyTypeRepository;

    private static final List<String> excludedTables = List.of(
        "audit_activity",
        "user_roles_courthouses",
        "hearing_reporting_restrictions",
        "user_account",
        "external_location_type",
        "object_record_status",
        "automated_task",
        "event_handler",
        "flyway_schema_history",
        "node_register",
        "region",
        "retention_policy_type",
        "security_permission",
        "security_role",
        "shedlock",
        "security_group",
        "security_group_user_account_ae",
        "transcription_status",
        "transcription_type",
        "transcription_urgency"
    );

    private List excludedSequences = List.of(
        "revinfo_seq"
    );

    private Map<String, String> sequencesStartFrom = Map.of(
        "usr_seq", "2",
        "grp_seq", "5"
    );

    public void truncateTables () {
        EntityManager em = entityManagerFactoryNonStatic.createEntityManager();
//
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

    public void resetSequences () {
        EntityManager em = entityManagerFactoryNonStatic.createEntityManager();
//
        em.getTransaction().begin();
        final Query query = em
            .createNativeQuery("SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'darts'");
        final List result = query.getResultList();



        for (Object seqName : result) {
            if (!excludedSequences.contains(seqName.toString())) {
                em.createNativeQuery("ALTER SEQUENCE darts." + seqName + " RESTART").executeUpdate();
            }
            if (sequencesStartFrom.containsKey(seqName.toString())) {
                em.createNativeQuery("ALTER SEQUENCE darts." + seqName + " RESTART WITH " + sequencesStartFrom.get(seqName.toString())).executeUpdate();
            }
        }
        em.getTransaction().commit();
    }

    public void resetUserAccountTable () {
//        EntityManager em = entityManagerFactoryNonStatic.createEntityManager();
//        em.getTransaction().begin();
//        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
//        em.createNativeQuery("SELECT usr_id, grp_id from darts.security_group_user_account_ae").getResultList();
//
//        em.createNativeQuery("DELETE FROM darts.user_account WHERE user_name NOT IN ('darts_global_test_user', 'dartstestuser', 'Cpp', 'Xhibit', 'system', 'system_housekeeping')").executeUpdate();
//        em.createNativeQuery("DELETE FROM darts.security_group WHERE group_name NOT IN ('Mid Tier Group', 'Dar Pc Group', 'Cpp Group', 'Xhibit Group', 'Test RCJ Appeals', 'Test Language Shop', 'Test Transcriber', " +
//                                 "'Test Judge', 'Test Requestor', 'Test Approver', 'SUPER_ADMIN', 'SUPER_USER', 'DARTS', 'MEDIA_IN_PERPETUITY')").executeUpdate();
//        em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
//        em.createNativeQuery("SELECT usr_id, grp_id from darts.security_group_user_account_ae").getResultList();
//
//        em.getTransaction().commit();



        retentionPolicyTypeRepository.deleteAll(
            retentionPolicyTypeRepository.findByPolicyNameNotIn(List.of(
                "DARTS Permanent Retention v3",
                "DARTS Standard Retention v3",
                "DARTS Not Guilty Policy",
                "DARTS Non Custodial Policy",
                "DARTS Custodial Policy",
                "DARTS Life Policy",
                "DARTS Default Policy",
                "DARTS Permanent Policy",
                "DARTS Manual Policy"
            ))
        );
//TODO leave some logs in trace?
        log.info("retention policy types: {}", retentionPolicyTypeRepository.findAll());

//        em.createNativeQuery("SELECT usr_id, grp_id from darts.security_group_user_account_ae").getResultList();

        log.info("users before delete: {}", userAccountRepository.findAll());
        userAccountRepository.deleteAll(
            userAccountRepository.findByUserNameNotIn(List.of("darts_global_test_user", "dartstestuser", "Cpp", "Xhibit", "system", "system_housekeeping"))
        );
        log.info("users after delete: {}", userAccountRepository.findAll());

//        userAccountRepository.deleteByUserNameNotIn(List.of("darts_global_test_user", "dartstestuser", "Cpp", "Xhibit", "system", "system_housekeeping"))

//        em.createNativeQuery("SELECT usr_id, grp_id from darts.security_group_user_account_ae").getResultList();
        securityGroupRepository.deleteAll(
            securityGroupRepository.findByGroupNameNotIn(PREDEFINED_SECURITY_GROUPS)
        );
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
//        truncateTables();
    }

    @BeforeEach
    void clearDb() {
        resetSequences();
//        truncateTables();
        dartsDatabase.clearDatabaseInThisOrder();
        resetUserAccountTable();
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
