package uk.gov.hmcts.darts.common.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.repository.UserRolesCourthousesRepository;
import uk.gov.hmcts.darts.courthouse.model.CourthousePost;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthousePost;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.EntityGraphPersistence;
import uk.gov.hmcts.darts.testutils.stubs.RegionStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouseWithName;
import static uk.gov.hmcts.darts.test.common.data.RegionTestData.minimalRegion;
import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.createGroupForRole;

@AutoConfigureMockMvc
class CourthouseApiTest extends IntegrationBase {

    private static final String ORIGINAL_USERNAME = "James Smith";
    private static final String ORIGINAL_EMAIL_ADDRESS = "james.smith@hmcts.net";
    private static final String ORIGINAL_DESCRIPTION = "A test user";
    private static final boolean ORIGINAL_SYSTEM_USER_FLAG = false;
    private static final OffsetDateTime ORIGINAL_LAST_LOGIN_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime ORIGINAL_LAST_MODIFIED_DATE_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime ORIGINAL_CREATED_DATE_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final String SWANSEA_CROWN_COURT = "SWANSEA CROWN COURT";
    private static final String LEEDS_COURT = "LEEDS";
    private static final String MANCHESTER_COURT = "MANCHESTER";
    private static final String WALES_REGION = "Wales";
    private static final String NORTH_WEST_REGION = "North West";
    private static final String COURTHOUSE_NAME = "INT-TEST_HAVERFORDWEST";
    private static final String ANOTHER_COURTHOUSE_NAME = "INT-TEST_SWANSEA";
    private static final String COURTHOUSE_DISPLAY_NAME = "Haverfordwest";
    private static final int TRANSCRIBER_GROUP_ID = -4;

    private static final String OID = "oid";

    @Mock
    private AuthorisationApi authorisationApi;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private RegionStub regionStub;

    @Autowired
    private SecurityGroupRepository securityGroupRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    RegionRepository regionRepository;

    @Autowired
    CourthouseRepository courthouseRepository;

    @Autowired
    UserRolesCourthousesRepository userRolesCourthousesRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ObjectMapper objectMapper;

    private TransactionTemplate transactionTemplate;

    @Autowired
    DartsDatabaseStub dartsDatabaseStub;

    @Autowired
    UserAccountStub userStub;

    @Autowired
    private EntityGraphPersistence entityGraphPersistence;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        authentication = Mockito.mock(Authentication.class);
        transactionTemplate = new TransactionTemplate(transactionManager);
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    @Test
    void adminCourthousesGet() throws Exception {
        UserIdentity mockUserIdentity = Mockito.mock(UserIdentity.class);
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        createEnabledUserAccountEntity(user);
        superAdminUserStub.setupUserAsAuthorised(authentication, user);

        Integer addedId = dartsDatabase.createCourthouseUnlessExists(COURTHOUSE_NAME)
            .getId();

        MockHttpServletRequestBuilder requestBuilder = get("/admin/courthouses/{courthouse_id}", addedId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.courthouse_name", is(COURTHOUSE_NAME)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.has_data", is(false)))
            .andDo(print())
            .andReturn();

        assertEquals(200, response.getResponse().getStatus());
        assertTrue(response.getResponse().getContentAsString().contains("security_group_ids"));
        assertFalse(response.getResponse().getContentAsString().contains("region_id"));
    }

    @Test
    void adminCourthousesGetWithCase() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        Integer addedId = dartsDatabase.createCourthouseUnlessExists(COURTHOUSE_NAME)
            .getId();

        dartsDatabase.createCase(COURTHOUSE_NAME, "101");

        MockHttpServletRequestBuilder requestBuilder = get("/admin/courthouses/{courthouse_id}", addedId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.courthouse_name", is(COURTHOUSE_NAME)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.has_data", is(true)))
            .andDo(print())
            .andReturn();

        assertEquals(200, response.getResponse().getStatus());
        assertTrue(response.getResponse().getContentAsString().contains("security_group_ids"));
        assertFalse(response.getResponse().getContentAsString().contains("region_id"));
    }

    @Test
    void adminCourthousesGetWithHearing() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        Integer addedId = dartsDatabase.createCourthouseUnlessExists(COURTHOUSE_NAME)
            .getId();

        dartsDatabase.createHearing(COURTHOUSE_NAME,
                                    "roomname", "101", LocalDateTime.now());

        MockHttpServletRequestBuilder requestBuilder = get("/admin/courthouses/{courthouse_id}", addedId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.courthouse_name", is(COURTHOUSE_NAME)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.has_data", is(true)))
            .andDo(print())
            .andReturn();

        assertEquals(200, response.getResponse().getStatus());
        assertTrue(response.getResponse().getContentAsString().contains("security_group_ids"));
        assertFalse(response.getResponse().getContentAsString().contains("region_id"));
    }


    @Test
    void courthousesWithRegionAndSecurityGroupsGet() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        var courthouse = createCourthouseWithName(COURTHOUSE_NAME);
        var region = minimalRegion();
        var secGrp1 = createGroupForRole(SUPER_USER);
        var secGrp2 = createGroupForRole(SUPER_USER);
        dartsDatabase.save(region);
        dartsDatabase.save(secGrp1);
        dartsDatabase.save(secGrp2);

        courthouse.setRegion(region);
        courthouse.setSecurityGroups(Set.of(secGrp1, secGrp2));

        dartsDatabase.save(courthouse);

        MockHttpServletRequestBuilder requestBuilder = get("/admin/courthouses/{courthouse_id}", courthouse.getId())
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.courthouse_name", is(COURTHOUSE_NAME)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.region_id", is(region.getId())))
            .andExpect(jsonPath("$.security_group_ids", hasSize(2)))
            .andExpect(jsonPath("$.security_group_ids", containsInAnyOrder(secGrp1.getId(), secGrp2.getId())))
            .andDo(print())
            .andReturn();


        assertEquals(200, response.getResponse().getStatus());
        assertTrue(response.getResponse().getContentAsString().contains("security_group_ids"));
        assertTrue(response.getResponse().getContentAsString().contains("region_id"));
    }

    @Test
    void courthousesGet_ThreeCourthousesAssignedToUser() throws Exception {
        final CourthouseEntity swanseaCourthouse = dartsDatabase.createCourthouseUnlessExists(SWANSEA_CROWN_COURT);
        final CourthouseEntity leedsCourthouse = dartsDatabase.createCourthouseUnlessExists(LEEDS_COURT);
        final CourthouseEntity manchesterCourthouse = dartsDatabase.createCourthouseUnlessExists(MANCHESTER_COURT);

        final RegionEntity northWestRegion = regionStub.createRegionsUnlessExists(NORTH_WEST_REGION);
        final RegionEntity walesRegion = regionStub.createRegionsUnlessExists(WALES_REGION);

        swanseaCourthouse.setRegion(walesRegion);
        manchesterCourthouse.setRegion(northWestRegion);

        var user = userStub.createAuthorisedIntegrationTestUser(false, swanseaCourthouse, leedsCourthouse, manchesterCourthouse);

        user = userAccountRepository.save(user);

        superAdminUserStub.setupUserAsAuthorised(authentication, user);

        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courthouse_name", is(LEEDS_COURT)))
            .andExpect(jsonPath("$[1].courthouse_name", is(MANCHESTER_COURT)))
            .andExpect(jsonPath("$[2].courthouse_name", is(SWANSEA_CROWN_COURT)))
            .andExpect(jsonPath("$[3].courthouse_name").doesNotExist())
            .andDo(print()).andReturn();

        assertEquals(200, response.getResponse().getStatus());
    }

    @Test
    void courthousesGet_ThreeCourthousesAssignedToUserInactive() throws Exception {
        String courthouseName = "courthousetest";
        UserAccountEntity userAccountEntity = userStub.createAuthorisedIntegrationTestUser(false, courthouseName);
        userAccountEntity.setActive(false);
        userAccountRepository.save(userAccountEntity);

        superAdminUserStub.setupUserAsAuthorised(authentication, userAccountEntity);

        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andExpect(jsonPath("$.type").value(
            AuthorisationError.USER_DETAILS_INVALID.getType()));
    }

    @Test
    void courthousesGet_TwoCourthousesAssignedToUser() throws Exception {
        dartsDatabase.createCourthouseUnlessExists(LEEDS_COURT);
        final CourthouseEntity swanseaCourthouse = dartsDatabase.createCourthouseUnlessExists(SWANSEA_CROWN_COURT);
        final CourthouseEntity manchesterCourthouse = dartsDatabase.createCourthouseUnlessExists(MANCHESTER_COURT);

        // given
        var user = userStub.createAuthorisedIntegrationTestUser(false, swanseaCourthouse, manchesterCourthouse);
        userAccountRepository.save(user);

        superAdminUserStub.setupUserAsAuthorised(authentication, user);

        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courthouse_name", is(MANCHESTER_COURT)))
            .andExpect(jsonPath("$[1].courthouse_name", is(SWANSEA_CROWN_COURT)))
            .andExpect(jsonPath("$[2].courthouse_name").doesNotExist())
            .andDo(print()).andReturn();

        assertEquals(200, response.getResponse().getStatus());

    }

    @Test
    void courthousesGetRequestedByJudge() throws Exception {
        final CourthouseEntity leedsCourthouse = dartsDatabase.createCourthouseUnlessExists(LEEDS_COURT);
        final CourthouseEntity swanseaCourthouse = dartsDatabase.createCourthouseUnlessExists(SWANSEA_CROWN_COURT);
        final CourthouseEntity manchesterCourthouse = dartsDatabase.createCourthouseUnlessExists(MANCHESTER_COURT);

        final RegionEntity northWestRegion = regionStub.createRegionsUnlessExists(NORTH_WEST_REGION);
        final RegionEntity walesRegion = regionStub.createRegionsUnlessExists(WALES_REGION);

        swanseaCourthouse.setRegion(walesRegion);
        manchesterCourthouse.setRegion(northWestRegion);

        // given
        var user = userStub.createJudgeUser();
        superAdminUserStub.setupUserAsAuthorised(authentication, user);

        var securityGroup = getSecurityGroupEntity(Set.of(swanseaCourthouse, manchesterCourthouse, leedsCourthouse));
        assignSecurityGroupToUser(user, securityGroup);

        userAccountRepository.save(user);

        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courthouse_name", is(LEEDS_COURT)))
            .andExpect(jsonPath("$[1].courthouse_name", is(MANCHESTER_COURT)))
            .andExpect(jsonPath("$[2].courthouse_name", is(SWANSEA_CROWN_COURT)))
            .andDo(print()).andReturn();

        assertEquals(200, response.getResponse().getStatus());

    }

    @Test
    void courthousesGetNonExistingId() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        MockHttpServletRequestBuilder requestBuilder = get("/admin/courthouses/{courthouse_id}", 900)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andDo(print()).andReturn();

        assertEquals(404, response.getResponse().getStatus());
    }

    @Test
    void courthousesGetNotAuthorised() throws Exception {
        CourthouseEntity swanseaCourthouse = dartsDatabase.createCourthouseUnlessExists(SWANSEA_CROWN_COURT);
        Integer addedId = swanseaCourthouse.getId();

        MockHttpServletRequestBuilder requestBuilder = get("/admin/courthouses/{courthouse_id}", addedId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andDo(print()).andReturn();

        assertEquals(403, response.getResponse().getStatus());
    }

    @Test
    void courthousesGetRequestedByAdmin() throws Exception {
        // Given
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        CourthouseEntity courtHouseEntity = dartsDatabase.createCourthouseUnlessExists(COURTHOUSE_NAME);
        CourthouseEntity anotherCourthouseEntity = dartsDatabase.createCourthouseUnlessExists(ANOTHER_COURTHOUSE_NAME);

        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        superAdminUserStub.setupUserAsAuthorised(authentication, user);

        // When
        mockMvc.perform(requestBuilder)
            // Then
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.*", hasSize(2)))

            .andExpect(jsonPath("$[0].id", is(courtHouseEntity.getId())))
            .andExpect(jsonPath("$[0].courthouse_name", is(COURTHOUSE_NAME)))
            .andExpect(jsonPath("$[0].display_name", is(COURTHOUSE_NAME)))
            .andExpect(jsonPath("$[0].created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$[0].last_modified_date_time", is(notNullValue())))

            .andExpect(jsonPath("$[1].id", is(anotherCourthouseEntity.getId())))
            .andExpect(jsonPath("$[1].courthouse_name", is(ANOTHER_COURTHOUSE_NAME)))
            .andExpect(jsonPath("$[1].display_name", is(ANOTHER_COURTHOUSE_NAME)))
            .andExpect(jsonPath("$[1].created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$[1].last_modified_date_time", is(notNullValue())));
    }

    @Test
    void courthousesPostShouldCreateExpectedApproverAndRequesterGroupsAndCourthouse() throws Exception {
        // Given
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        superAdminUserStub.setupUserAsAuthorised(authentication, user);

        CourthousePost courthousePost = new CourthousePost();
        courthousePost.setCourthouseName(COURTHOUSE_NAME);
        courthousePost.setDisplayName(COURTHOUSE_DISPLAY_NAME);

        String jsonRequestBody = objectMapper.writeValueAsString(courthousePost);
        MockHttpServletRequestBuilder requestBuilder = post("/admin/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(jsonRequestBody);

        // When
        MvcResult mvcResult = mockMvc.perform(requestBuilder)
            // Then
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(notNullValue())))
            .andExpect(jsonPath("$.courthouse_name", is(COURTHOUSE_NAME)))
            .andExpect(jsonPath("$.display_name", is(COURTHOUSE_DISPLAY_NAME)))
            .andExpect(jsonPath("$.security_group_ids", is(notNullValue())))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())))
            .andReturn();

        var extendedCourthousePost = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ExtendedCourthousePost.class);

        transactionTemplate.executeWithoutResult(status -> {
            List<SecurityGroupEntity> securityGroups = extendedCourthousePost.getSecurityGroupIds().stream()
                .map(id -> dartsDatabase.getSecurityGroupRepository().findById(id))
                .flatMap(Optional::stream)
                .toList();

            assertEquals(2, securityGroups.size());
            List<SecurityGroupEntity> approverGroups = securityGroups.stream()
                .filter(securityGroup -> securityGroup.getSecurityRoleEntity().getRoleName().equals(APPROVER.name()))
                .toList();
            assertEquals(1, approverGroups.size());
            SecurityGroupEntity approverGroup = approverGroups.getFirst();
            assertEquals("INT-TEST_HAVERFORDWEST_APPROVER", approverGroup.getGroupName());
            assertEquals("Haverfordwest Approver", approverGroup.getDisplayName());

            List<SecurityGroupEntity> requesterGroups = securityGroups.stream()
                .filter(securityGroup -> securityGroup.getSecurityRoleEntity().getRoleName().equals(REQUESTER.name()))
                .toList();
            assertEquals(1, requesterGroups.size());
            SecurityGroupEntity requesterGroup = requesterGroups.getFirst();
            assertEquals("INT-TEST_HAVERFORDWEST_REQUESTER", requesterGroup.getGroupName());
            assertEquals("Haverfordwest Requester", requesterGroup.getDisplayName());
        });
    }

    @Test
    void courthousesPostShouldFailWhenNonTranscriberSecurityGroupIsProvided() throws Exception {
        // Given
        final SecurityGroupEntity standingApproverGroup = transactionTemplate.execute(status -> {
            Optional<SecurityGroupEntity> standingApproverGroupOptional = dartsDatabase.getSecurityGroupRepository()
                .findAll()
                .stream()
                .filter(securityGroupEntity -> securityGroupEntity.getSecurityRoleEntity().getRoleName().equals(APPROVER.name()))
                .findFirst();
            assertTrue(standingApproverGroupOptional.isPresent(), "Precondition failed: Expected a standing approver group to be available");

            return standingApproverGroupOptional.get();
        });

        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        CourthousePost courthousePost = new CourthousePost();
        courthousePost.setCourthouseName(COURTHOUSE_NAME);
        courthousePost.setDisplayName(COURTHOUSE_DISPLAY_NAME);
        courthousePost.setSecurityGroupIds(Collections.singletonList(standingApproverGroup.getId()));

        String jsonRequestBody = objectMapper.writeValueAsString(courthousePost);

        MockHttpServletRequestBuilder requestBuilder = post("/admin/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(jsonRequestBody);

        /// When
        mockMvc.perform(requestBuilder)
            // Then
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("COURTHOUSE_104"))
            .andExpect(jsonPath("$.title").value("Only TRANSCRIBER roles may be assigned"));
    }

    @Test
    void courthousesPostShouldSucceedWhenTranscriberSecurityGroupIsProvided() throws Exception {
        // Given
        final SecurityGroupEntity standingTranscriberGroup = transactionTemplate.execute(status -> {
            Optional<SecurityGroupEntity> standingTranscriberGroupOptional = dartsDatabase.getSecurityGroupRepository()
                .findAll()
                .stream()
                .filter(securityGroupEntity -> securityGroupEntity.getSecurityRoleEntity().getRoleName().equals(TRANSCRIBER.name()))
                .findFirst();
            assertTrue(standingTranscriberGroupOptional.isPresent(), "Precondition failed: Expected a standing transcriber group to be available");

            return standingTranscriberGroupOptional.get();
        });

        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        CourthousePost courthousePost = new CourthousePost();
        courthousePost.setCourthouseName(COURTHOUSE_NAME);
        courthousePost.setDisplayName(COURTHOUSE_DISPLAY_NAME);
        courthousePost.setSecurityGroupIds(Collections.singletonList(standingTranscriberGroup.getId()));

        String jsonRequestBody = objectMapper.writeValueAsString(courthousePost);

        MockHttpServletRequestBuilder requestBuilder = post("/admin/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(jsonRequestBody);

        // When
        mockMvc.perform(requestBuilder)
            // Then
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(notNullValue())))
            .andExpect(jsonPath("$.courthouse_name", is(COURTHOUSE_NAME)))
            .andExpect(jsonPath("$.display_name", is(COURTHOUSE_DISPLAY_NAME)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.security_group_ids", hasItems(TRANSCRIBER_GROUP_ID)))
            .andExpect(jsonPath("$.security_group_ids", hasSize(3)));
    }

    @Test
    void courthousesPostShouldFailWhenCourthouseWithSameNameAlreadyExists() throws Exception {
        // Given
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        CourthouseEntity courtHouseEntity = dartsDatabase.createCourthouseUnlessExists(COURTHOUSE_NAME);
        courtHouseEntity.setDisplayName(COURTHOUSE_DISPLAY_NAME);
        dartsDatabase.save(courtHouseEntity);

        CourthousePost courthousePost = new CourthousePost();
        courthousePost.setCourthouseName(COURTHOUSE_NAME);
        courthousePost.setDisplayName(UUID.randomUUID().toString()); // just some unique string

        String jsonRequestBody = objectMapper.writeValueAsString(courthousePost);

        MockHttpServletRequestBuilder requestBuilder = post("/admin/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(jsonRequestBody);

        // When
        mockMvc.perform(requestBuilder)
            // Then
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("COURTHOUSE_100"))
            .andExpect(jsonPath("$.title").value("Provided courthouse name already exists."));
    }

    @Test
    void courthousesPostShouldFailWhenCourthouseWithSameDisplayNameAlreadyExists() throws Exception {
        // Given
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        CourthouseEntity courtHouseEntity = dartsDatabase.createCourthouseUnlessExists(COURTHOUSE_NAME);
        courtHouseEntity.setDisplayName(COURTHOUSE_DISPLAY_NAME);
        dartsDatabase.save(courtHouseEntity);

        CourthousePost courthousePost = new CourthousePost();
        courthousePost.setCourthouseName(UUID.randomUUID().toString().toUpperCase(Locale.ENGLISH)); // just some unique string
        courthousePost.setDisplayName(COURTHOUSE_DISPLAY_NAME);

        String jsonRequestBody = objectMapper.writeValueAsString(courthousePost);

        MockHttpServletRequestBuilder requestBuilder = post("/admin/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(jsonRequestBody);

        // When
        mockMvc.perform(requestBuilder)
            // Then
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("COURTHOUSE_103"))
            .andExpect(jsonPath("$.title").value("Provided courthouse display name already exists."));
    }

    @Test
    void courthousesPostShouldSucceedWhenRegionIsProvided() throws Exception {
        // Given
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        CourthousePost courthousePost = new CourthousePost();
        courthousePost.setCourthouseName(COURTHOUSE_NAME);
        courthousePost.setDisplayName(COURTHOUSE_DISPLAY_NAME);

        RegionEntity southWalesRegion = regionStub.createRegionsUnlessExists("South Wales");
        regionStub.createRegionsUnlessExists("North Wales");
        Integer regionId = southWalesRegion.getId();
        courthousePost.setRegionId(regionId);

        String jsonRequestBody = objectMapper.writeValueAsString(courthousePost);

        MockHttpServletRequestBuilder requestBuilder = post("/admin/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(jsonRequestBody);

        // When
        mockMvc.perform(requestBuilder)
            // Then
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(notNullValue())))
            .andExpect(jsonPath("$.courthouse_name", is(COURTHOUSE_NAME)))
            .andExpect(jsonPath("$.display_name", is(COURTHOUSE_DISPLAY_NAME)))
            .andExpect(jsonPath("$.region_id", is(regionId)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())));
    }

    @Test
    void courthousesPostShouldFailIfProvidedRegionDoesNotExist() throws Exception {
        // Given
        final int nonExistingRegionId = 999_999;
        assertFalse(dartsDatabase.getRegionRepository().existsById(nonExistingRegionId), "Precondition failed: Expected this group to not exist");

        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        CourthousePost courthousePost = new CourthousePost();
        courthousePost.setCourthouseName(COURTHOUSE_NAME);
        courthousePost.setDisplayName(COURTHOUSE_DISPLAY_NAME);
        courthousePost.setRegionId(nonExistingRegionId);

        String jsonRequestBody = objectMapper.writeValueAsString(courthousePost);

        MockHttpServletRequestBuilder requestBuilder = post("/admin/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(jsonRequestBody);

        // When
        mockMvc.perform(requestBuilder)
            // Then
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("COURTHOUSE_105"))
            .andExpect(jsonPath("$.title").value("Region ID does not exist"));

    }

    @Test
    void courthousesPostShouldFailIfMissingCourthouseName() throws Exception {
        // Given
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        CourthousePost courthousePost = new CourthousePost();
        courthousePost.setCourthouseName(null);
        courthousePost.setDisplayName(COURTHOUSE_DISPLAY_NAME);

        String jsonRequestBody = objectMapper.writeValueAsString(courthousePost);

        MockHttpServletRequestBuilder requestBuilder = post("/admin/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(jsonRequestBody);

        // When
        mockMvc.perform(requestBuilder)
            // Then
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://zalando.github.io/problem/constraint-violation"))
            .andExpect(jsonPath("$.title").value("Constraint Violation"))
            .andExpect(jsonPath("$.violations.*.field").value("courthouseName"))
            .andExpect(jsonPath("$.violations.*.message").value("must not be null"));
    }

    @Test
    void courthousesPostShouldFailIfMissingDisplayName() throws Exception {
        // Given
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        CourthousePost courthousePost = new CourthousePost();
        courthousePost.setCourthouseName(COURTHOUSE_NAME);
        courthousePost.setDisplayName(null);

        String jsonRequestBody = objectMapper.writeValueAsString(courthousePost);

        MockHttpServletRequestBuilder requestBuilder = post("/admin/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(jsonRequestBody);

        // When
        mockMvc.perform(requestBuilder)
            // Then
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://zalando.github.io/problem/constraint-violation"))
            .andExpect(jsonPath("$.title").value("Constraint Violation"))
            .andExpect(jsonPath("$.violations.*.field").value("displayName"))
            .andExpect(jsonPath("$.violations.*.message").value("must not be null"));
    }

    @Test
    void adminCourthousesPost_shouldReturnUnprocessableEntity_whenCourthouseNameIsLowercase() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        CourthousePost courthousePost = new CourthousePost();
        courthousePost.setCourthouseName("lowercase courthouse");
        courthousePost.setDisplayName(COURTHOUSE_DISPLAY_NAME);

        String jsonRequestBody = objectMapper.writeValueAsString(courthousePost);

        mockMvc.perform(post("/admin/courthouses")
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(jsonRequestBody))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("COURTHOUSE_108"))
            .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void adminRegionsGet() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(authentication);
        createEnabledUserAccountEntity(user);

        RegionEntity region1 = regionStub.createRegionsUnlessExists("South Wales");
        RegionEntity region2 = regionStub.createRegionsUnlessExists("North Wales");

        MockHttpServletRequestBuilder requestBuilder = get("/admin/regions")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].id", is(region1.getId())))
            .andExpect(jsonPath("$.[0].name", is("South Wales")))
            .andExpect(jsonPath("$.[1].id", is(region2.getId())))
            .andExpect(jsonPath("$.[1].name", is("North Wales")))
            .andDo(print())
            .andReturn();

        assertEquals(200, response.getResponse().getStatus());

        regionRepository.deleteAll();

    }

    @Test
    void adminRegionsNotAuthorised() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/admin/regions")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andDo(print()).andReturn();

        assertEquals(403, response.getResponse().getStatus());
    }

    private UserAccountEntity createEnabledUserAccountEntity(UserAccountEntity user) {
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setUserFullName(ORIGINAL_USERNAME);
        userAccountEntity.setEmailAddress(ORIGINAL_EMAIL_ADDRESS);
        userAccountEntity.setUserDescription(ORIGINAL_DESCRIPTION);
        userAccountEntity.setActive(true);
        userAccountEntity.setLastLoginTime(ORIGINAL_LAST_LOGIN_TIME);
        userAccountEntity.setLastModifiedDateTime(ORIGINAL_LAST_MODIFIED_DATE_TIME);
        userAccountEntity.setCreatedDateTime(ORIGINAL_CREATED_DATE_TIME);
        userAccountEntity.setIsSystemUser(ORIGINAL_SYSTEM_USER_FLAG);
        userAccountEntity.setCreatedBy(user);
        userAccountEntity.setLastModifiedBy(user);

        return dartsDatabase.getUserAccountRepository()
            .save(userAccountEntity);
    }

    private static SecurityGroupEntity getSecurityGroupEntity(Set<CourthouseEntity> courthouseEntities) {
        var securityGroup = createGroupForRole(TRANSLATION_QA);
        securityGroup.setCourthouseEntities(courthouseEntities);
        securityGroup.setGlobalAccess(true);
        securityGroup.setUseInterpreter(false);
        return securityGroup;
    }

    private void assignSecurityGroupToUser(UserAccountEntity user, SecurityGroupEntity securityGroup) {
        securityGroup.getUsers().add(user);
        user.getSecurityGroupEntities().add(securityGroup);
        securityGroupRepository.save(securityGroup);
    }
}
