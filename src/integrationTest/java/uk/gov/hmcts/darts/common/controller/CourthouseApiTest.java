package uk.gov.hmcts.darts.common.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.repository.UserRolesCourthousesRepository;
import uk.gov.hmcts.darts.courthouse.model.CourthousePost;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthousePost;
import uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.RegionStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

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
    private UserAccountStub userAccountStub;

    @Autowired
    private RegionStub regionStub;

    @Autowired
    private SecurityGroupRepository securityGroupRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @MockBean
    private UserIdentity mockUserIdentity;

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

    UserAccountEntity user;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        Set<SecurityGroupEntity> securityGroupsToBeDeleted = dartsDatabase.getSecurityGroupRepository()
            .findAll()
            .stream()
            .filter(securityGroupEntity -> securityGroupEntity.getGroupName().contains("INT-TEST"))
            .collect(Collectors.toSet());
        dartsDatabase.addToTrash(securityGroupsToBeDeleted);
    }

    @Test
    void adminCourthousesGet() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        createEnabledUserAccountEntity(user);

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

        verify(mockUserIdentity, times(1)).userHasGlobalAccess(Set.of(SUPER_ADMIN, SUPER_USER));
        verifyNoMoreInteractions(mockUserIdentity);

    }

    @Test
    void adminCourthousesGetWithCase() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
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

        verify(mockUserIdentity, times(1)).userHasGlobalAccess(Set.of(SUPER_ADMIN, SUPER_USER));
        verifyNoMoreInteractions(mockUserIdentity);

    }

    @Test
    void adminCourthousesGetWithHearing() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
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

        verify(mockUserIdentity, times(1)).userHasGlobalAccess(Set.of(SUPER_ADMIN, SUPER_USER));
        verifyNoMoreInteractions(mockUserIdentity);

    }

    @Test
    @Transactional
    void courthousesWithRegionAndSecurityGroupsGet() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        createEnabledUserAccountEntity(user);

        CourthouseEntity courtHouseEntity = dartsDatabase.createCourthouseUnlessExists(COURTHOUSE_NAME);

        RegionEntity region = new RegionEntity();
        region.setId(5);
        courtHouseEntity.setRegion(region);

        Set<SecurityGroupEntity> secGrps = new LinkedHashSet<>();
        SecurityGroupEntity s1 = new SecurityGroupEntity();
        s1.setId(3);
        secGrps.add(s1);
        SecurityGroupEntity s2 = new SecurityGroupEntity();
        s2.setId(4);
        secGrps.add(s2);

        courtHouseEntity.setSecurityGroups(secGrps);

        MockHttpServletRequestBuilder requestBuilder = get("/admin/courthouses/{courthouse_id}", courtHouseEntity.getId())
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.courthouse_name", is(COURTHOUSE_NAME)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.region_id", is(5)))
            .andExpect(jsonPath("$.security_group_ids", hasSize(2)))
            .andExpect(jsonPath("$.security_group_ids", contains(3, 4)))
            .andDo(print())
            .andReturn();


        assertEquals(200, response.getResponse().getStatus());
        assertTrue(response.getResponse().getContentAsString().contains("security_group_ids"));
        assertTrue(response.getResponse().getContentAsString().contains("region_id"));

        verify(mockUserIdentity, times(1)).userHasGlobalAccess(Set.of(SUPER_ADMIN, SUPER_USER));
        verifyNoMoreInteractions(mockUserIdentity);

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

        // given
        var user = userAccountStub.createIntegrationUser(getGuidFromToken());
        userAccountRepository.save(user);

        Mockito.when(mockUserIdentity.getUserAccount()).thenReturn(user);
        Mockito.when(mockUserIdentity.getListOfCourthouseIdsUserHasAccessTo()).thenReturn(List.of(swanseaCourthouse.getId(),
                                                                                                  leedsCourthouse.getId(),
                                                                                                  manchesterCourthouse.getId()));

        Mockito.when(mockUserIdentity.userHasGlobalAccess(any())).thenReturn(true);

        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courthouse_name", is(SWANSEA_CROWN_COURT)))
            .andExpect(jsonPath("$[1].courthouse_name", is(LEEDS_COURT)))
            .andExpect(jsonPath("$[2].courthouse_name", is(MANCHESTER_COURT)))
            .andExpect(jsonPath("$[3].courthouse_name").doesNotExist())
            .andDo(print()).andReturn();

        assertEquals(200, response.getResponse().getStatus());

    }

    @Test
    void courthousesGet_TwoCourthousesAssignedToUser() throws Exception {
        dartsDatabase.createCourthouseUnlessExists(LEEDS_COURT);
        final CourthouseEntity swanseaCourthouse = dartsDatabase.createCourthouseUnlessExists(SWANSEA_CROWN_COURT);
        final CourthouseEntity manchesterCourthouse = dartsDatabase.createCourthouseUnlessExists(MANCHESTER_COURT);

        // given
        var user = userAccountStub.createIntegrationUser(getGuidFromToken());
        userAccountRepository.save(user);

        Mockito.when(mockUserIdentity.getUserAccount()).thenReturn(user);
        Mockito.when(mockUserIdentity.getListOfCourthouseIdsUserHasAccessTo()).thenReturn(List.of(swanseaCourthouse.getId(),
                                                                                                  manchesterCourthouse.getId()));
        Mockito.when(mockUserIdentity.userHasGlobalAccess(any())).thenReturn(true);

        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courthouse_name", is(SWANSEA_CROWN_COURT)))
            .andExpect(jsonPath("$[1].courthouse_name", is(MANCHESTER_COURT)))
            .andExpect(jsonPath("$[2].courthouse_name").doesNotExist())
            .andDo(print()).andReturn();

        assertEquals(200, response.getResponse().getStatus());

    }

    @Test
    @Transactional
    void courthousesGetRequestedByJudge() throws Exception {
        final CourthouseEntity leedsCourthouse = dartsDatabase.createCourthouseUnlessExists(LEEDS_COURT);
        final CourthouseEntity swanseaCourthouse = dartsDatabase.createCourthouseUnlessExists(SWANSEA_CROWN_COURT);
        final CourthouseEntity manchesterCourthouse = dartsDatabase.createCourthouseUnlessExists(MANCHESTER_COURT);

        final RegionEntity northWestRegion = regionStub.createRegionsUnlessExists(NORTH_WEST_REGION);
        final RegionEntity walesRegion = regionStub.createRegionsUnlessExists(WALES_REGION);

        swanseaCourthouse.setRegion(walesRegion);
        manchesterCourthouse.setRegion(northWestRegion);

        // given
        var user = userAccountStub.createJudgeUser();

        var securityGroup = getSecurityGroupEntity(swanseaCourthouse, manchesterCourthouse);
        assignSecurityGroupToUser(user, securityGroup);

        userAccountRepository.save(user);

        Mockito.when(mockUserIdentity.getUserAccount()).thenReturn(user);
        Mockito.when(mockUserIdentity.getListOfCourthouseIdsUserHasAccessTo()).thenReturn(List.of(leedsCourthouse.getId(),
                                                                                                  swanseaCourthouse.getId(),
                                                                                                  manchesterCourthouse.getId()));
        Mockito.when(mockUserIdentity.userHasGlobalAccess(any())).thenReturn(true);

        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courthouse_name", is(LEEDS_COURT)))
            .andExpect(jsonPath("$[1].courthouse_name", is(SWANSEA_CROWN_COURT)))
            .andExpect(jsonPath("$[2].courthouse_name", is(MANCHESTER_COURT)))
            .andDo(print()).andReturn();

        assertEquals(200, response.getResponse().getStatus());

    }

    @Test
    void courthousesGetNonExistingId() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        createEnabledUserAccountEntity(user);

        MockHttpServletRequestBuilder requestBuilder = get("/admin/courthouses/{courthouse_id}", 900)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andDo(print()).andReturn();

        assertEquals(404, response.getResponse().getStatus());

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN, SUPER_USER));
        verifyNoMoreInteractions(mockUserIdentity);
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
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        createEnabledUserAccountEntity(user);

        CourthouseEntity courtHouseEntity = dartsDatabase.createCourthouseUnlessExists(COURTHOUSE_NAME);
        CourthouseEntity anotherCourthouseEntity = dartsDatabase.createCourthouseUnlessExists(ANOTHER_COURTHOUSE_NAME);

        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        Mockito.when(mockUserIdentity.getListOfCourthouseIdsUserHasAccessTo()).thenReturn(List.of(courtHouseEntity.getId(),
                                                                                                  anotherCourthouseEntity.getId()));

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
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        createEnabledUserAccountEntity(user);

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
            SecurityGroupEntity approverGroup = approverGroups.get(0);
            assertEquals("INT-TEST_HAVERFORDWEST_APPROVER", approverGroup.getGroupName());
            assertEquals("Haverfordwest Approver", approverGroup.getDisplayName());

            List<SecurityGroupEntity> requesterGroups = securityGroups.stream()
                .filter(securityGroup -> securityGroup.getSecurityRoleEntity().getRoleName().equals(REQUESTER.name()))
                .toList();
            assertEquals(1, requesterGroups.size());
            SecurityGroupEntity requesterGroup = requesterGroups.get(0);
            assertEquals("INT-TEST_HAVERFORDWEST_REQUESTER", requesterGroup.getGroupName());
            assertEquals("Haverfordwest Requestor", requesterGroup.getDisplayName());

            verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN));
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

        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("COURTHOUSE_104"))
            .andExpect(jsonPath("$.title").value("Only TRANSCRIBER roles may be assigned"));

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void courthousesPostShouldSucceedWhenTranscriberSecurityGroupIsProvided() throws Exception {
        // Given
        final SecurityGroupEntity standingTranscriberGroup = transactionTemplate.execute(status -> {
            Optional<SecurityGroupEntity> standingTranscriberGroupOptional = dartsDatabase.getSecurityGroupRepository()
                .findAll()
                .stream()
                .filter(securityGroupEntity -> securityGroupEntity.getSecurityRoleEntity().getRoleName().equals(SecurityRoleEnum.TRANSCRIBER.name()))
                .findFirst();
            assertTrue(standingTranscriberGroupOptional.isPresent(), "Precondition failed: Expected a standing transcriber group to be available");

            return standingTranscriberGroupOptional.get();
        });

        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
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

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN));
    }

    @Test
    void courthousesPostShouldFailWhenCourthouseWithSameNameAlreadyExists() throws Exception {
        // Given
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
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

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void courthousesPostShouldFailWhenCourthouseWithSameDisplayNameAlreadyExists() throws Exception {
        // Given
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        createEnabledUserAccountEntity(user);

        CourthouseEntity courtHouseEntity = dartsDatabase.createCourthouseUnlessExists(COURTHOUSE_NAME);
        courtHouseEntity.setDisplayName(COURTHOUSE_DISPLAY_NAME);
        dartsDatabase.save(courtHouseEntity);

        CourthousePost courthousePost = new CourthousePost();
        courthousePost.setCourthouseName(UUID.randomUUID().toString()); // just some unique string
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

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void courthousesPostShouldSucceedWhenRegionIsProvided() throws Exception {
        // Given
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
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

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN));
    }

    @Test
    void courthousesPostShouldFailIfProvidedRegionDoesNotExist() throws Exception {
        // Given
        final int nonExistingRegionId = 999_999;
        assertFalse(dartsDatabase.getRegionRepository().existsById(nonExistingRegionId), "Precondition failed: Expected this group to not exist");

        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("COURTHOUSE_105"))
            .andExpect(jsonPath("$.title").value("Region ID does not exist"));

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void courthousesPostShouldFailIfMissingCourthouseName() throws Exception {
        // Given
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
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
            .andExpect(jsonPath("$.title").value("Constraint Violation"))
            .andExpect(jsonPath("$.properties.courthouseName").value("must not be null"));
    }

    @Test
    void courthousesPostShouldFailIfMissingDisplayName() throws Exception {
        // Given
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
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
            .andExpect(jsonPath("$.title").value("Constraint Violation"))
            .andExpect(jsonPath("$.properties.displayName").value("must not be null"));
    }

    @Test
    void adminRegionsGet() throws Exception {
        UserAccountEntity user = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
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

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(SUPER_ADMIN, SUPER_USER));
        verifyNoMoreInteractions(mockUserIdentity);
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
        userAccountEntity.setUserName(ORIGINAL_USERNAME);
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

    private static SecurityGroupEntity getSecurityGroupEntity(CourthouseEntity swanseaCourthouse, CourthouseEntity manchesterCourthouse) {
        Set<CourthouseEntity> courthouseEntities = new HashSet<>();
        courthouseEntities.add(swanseaCourthouse);
        courthouseEntities.add(manchesterCourthouse);

        var securityGroup = SecurityGroupTestData.buildGroupForRole(TRANSLATION_QA);
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

    private String getGuidFromToken() {
        if (nonNull(SecurityContextHolder.getContext().getAuthentication())) {
            Object principalObject = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

            Object oid = null;
            if (principalObject instanceof Jwt jwt) {
                oid = jwt.getClaims().get(OID);
            }
            if (nonNull(oid) && oid instanceof String guid && StringUtils.isNotBlank(guid)) {
                return guid;
            }
        }
        return null;
    }

}