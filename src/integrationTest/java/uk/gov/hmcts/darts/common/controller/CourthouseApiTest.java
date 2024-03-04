package uk.gov.hmcts.darts.common.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.RegionRepository;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthouse;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.RegionStub;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.ADMIN;
import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
class CourthouseApiTest extends IntegrationBase {

    public static final String REQUEST_BODY_HAVERFORDWEST_JSON = "tests/CourthousesTest/courthousesPostEndpoint/requestBodyHaverfordwest.json";
    public static final String REQUEST_BODY_400_MISSING_COURTHOUSE_NAME_JSON =
        "tests/CourthousesTest/courthousesPostEndpoint/requestBody400_MissingCourthouseName.json";
    public static final String REQUEST_BODY_400_MISSING_COURTHOUSE_DISPLAY_NAME_JSON =
        "tests/CourthousesTest/courthousesPostEndpoint/requestBody400_MissingCourthouseDisplayName.json";
    private static final String REQUEST_BODY_TEST_JSON = "tests/CourthousesTest/courthousesPostEndpoint/requestBodyTest.json";

    private static final String ORIGINAL_USERNAME = "James Smith";
    private static final String ORIGINAL_EMAIL_ADDRESS = "james.smith@hmcts.net";
    private static final String ORIGINAL_DESCRIPTION = "A test user";
    private static final boolean ORIGINAL_SYSTEM_USER_FLAG = false;
    private static final OffsetDateTime ORIGINAL_LAST_LOGIN_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime ORIGINAL_LAST_MODIFIED_DATE_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime ORIGINAL_CREATED_DATE_TIME = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
    public static final String SWANSEA_CROWN_COURT = "SWANSEA CROWN COURT";
    public static final String LEEDS_COURT = "LEEDS";
    public static final String MANCHESTER_COURT = "MANCHESTER";
    public static final String WALES_REGION = "Wales";
    public static final String NORTH_WEST_REGION = "North West";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserStub adminUserStub;

    @Autowired
    private RegionStub regionStub;

    @MockBean
    private UserIdentity mockUserIdentity;

    @Autowired
    RegionRepository regionRepository;

    @Autowired
    CourthouseRepository courthouseRepository;

    @Test
    void adminCourthousesGet() throws Exception {
        UserAccountEntity user = adminUserStub.givenUserIsAuthorised(mockUserIdentity);
        createEnabledUserAccountEntity(user);

        Integer addedId = addCourthouseAndGetId(REQUEST_BODY_HAVERFORDWEST_JSON);

        MockHttpServletRequestBuilder requestBuilder = get("/admin/courthouses/{courthouse_id}", addedId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.courthouse_name", is("HAVERFORDWEST")))
            .andExpect(jsonPath("$.code", is(761)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())))
            .andDo(print())
            .andReturn();

        assertEquals(200, response.getResponse().getStatus());
        assertTrue(response.getResponse().getContentAsString().contains("security_group_ids"));
        assertFalse(response.getResponse().getContentAsString().contains("region_id"));

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);

    }

    @Test
    @Transactional
    void courthousesWithRegionAndSecurityGroupsGet() throws Exception {
        UserAccountEntity user = adminUserStub.givenUserIsAuthorised(mockUserIdentity);
        createEnabledUserAccountEntity(user);

        final Integer addedId = addCourthouseAndGetId(REQUEST_BODY_HAVERFORDWEST_JSON);
        CourthouseEntity courtHouseEntity = dartsDatabase.createCourthouseUnlessExists("HAVERFORDWEST");

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

        MockHttpServletRequestBuilder requestBuilder = get("/admin/courthouses/{courthouse_id}", addedId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.courthouse_name", is("HAVERFORDWEST")))
            .andExpect(jsonPath("$.code", is(761)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.region_id", is(5)))
            .andExpect(jsonPath("$.security_group_ids", hasSize(2)))
            .andExpect(jsonPath("$.security_group_ids", contains(3,4)))
            .andDo(print())
            .andReturn();


        assertEquals(200, response.getResponse().getStatus());
        assertTrue(response.getResponse().getContentAsString().contains("security_group_ids"));
        assertTrue(response.getResponse().getContentAsString().contains("region_id"));

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);

    }

    @Test
    @Transactional
    void courthousesGet() throws Exception {

        final CourthouseEntity swanseaCourthouse = dartsDatabase.createCourthouseUnlessExists(SWANSEA_CROWN_COURT);
        final RegionEntity northWestRegion = regionStub.createRegionsUnlessExists(NORTH_WEST_REGION);
        final RegionEntity walesRegion = regionStub.createRegionsUnlessExists(WALES_REGION);

        swanseaCourthouse.setRegion(walesRegion);

        dartsDatabase.createCourthouseUnlessExists(LEEDS_COURT);

        final CourthouseEntity manchesterCourthouse = dartsDatabase.createCourthouseUnlessExists(MANCHESTER_COURT);

        manchesterCourthouse.setRegion(northWestRegion);

        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courthouse_name", is(SWANSEA_CROWN_COURT)))
            .andExpect(jsonPath("$[0].region_id", is(walesRegion.getId())))
            .andExpect(jsonPath("$[1].courthouse_name", is(LEEDS_COURT)))
            .andExpect(jsonPath("[1].region_id").doesNotExist())
            .andExpect(jsonPath("$[2].courthouse_name", is(MANCHESTER_COURT)))
            .andExpect(jsonPath("$[2].region_id", is(northWestRegion.getId())))
            .andDo(print()).andReturn();

        assertEquals(200, response.getResponse().getStatus());

    }

    @Test
    void courthousesGetNonExistingId() throws Exception {
        UserAccountEntity user = adminUserStub.givenUserIsAuthorised(mockUserIdentity);
        createEnabledUserAccountEntity(user);

        MockHttpServletRequestBuilder requestBuilder = get("/admin/courthouses/{courthouse_id}", 900)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andDo(print()).andReturn();

        assertEquals(404, response.getResponse().getStatus());

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void courthousesGetNotAuthorised() throws Exception {

        Integer addedId = addCourthouseAndGetId(REQUEST_BODY_HAVERFORDWEST_JSON);

        MockHttpServletRequestBuilder requestBuilder = get("/admin/courthouses/{courthouse_id}", addedId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andDo(print()).andReturn();

        assertEquals(403, response.getResponse().getStatus());

    }

    @Test
    void courthousesGetAll() throws Exception {

        MvcResult haverfordwestResponse = makeRequestToAddCourthouseToDatabase(
            REQUEST_BODY_HAVERFORDWEST_JSON);
        MvcResult swanseaResponse = makeRequestToAddCourthouseToDatabase(REQUEST_BODY_TEST_JSON);


        ExtendedCourthouse haverfordwestCourthouse = objectMapper.readValue(
            haverfordwestResponse.getResponse().getContentAsString(),
            ExtendedCourthouse.class
        );
        ExtendedCourthouse swanseaCourthouse = objectMapper.readValue(
            swanseaResponse.getResponse().getContentAsString(),
            ExtendedCourthouse.class
        );

        // Truncate created and modified to milliseconds as the post (saveAndFlush) returns a more precise timestamp
        haverfordwestCourthouse.setCreatedDateTime(haverfordwestCourthouse.getCreatedDateTime().truncatedTo(ChronoUnit.MILLIS));
        haverfordwestCourthouse.setLastModifiedDateTime(haverfordwestCourthouse.getLastModifiedDateTime().truncatedTo(
            ChronoUnit.MILLIS));
        swanseaCourthouse.setCreatedDateTime(swanseaCourthouse.getCreatedDateTime().truncatedTo(ChronoUnit.MILLIS));
        swanseaCourthouse.setLastModifiedDateTime(swanseaCourthouse.getLastModifiedDateTime().truncatedTo(ChronoUnit.MILLIS));

        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andDo(print()).andReturn();

        List<ExtendedCourthouse> courthouseList = objectMapper.readValue(
            response.getResponse().getContentAsString(),
            new TypeReference<>() {
            }
        );
        for (ExtendedCourthouse extendedCourthouse : courthouseList) {
            extendedCourthouse.setCreatedDateTime(extendedCourthouse.getCreatedDateTime().truncatedTo(ChronoUnit.MILLIS));
            extendedCourthouse.setLastModifiedDateTime(extendedCourthouse.getLastModifiedDateTime().truncatedTo(
                ChronoUnit.MILLIS));
        }
        assertTrue(
            courthouseList.contains(haverfordwestCourthouse),
            haverfordwestResponse.getResponse().getContentAsString()
        );
        assertTrue(courthouseList.contains(swanseaCourthouse), swanseaResponse.getResponse().getContentAsString());
    }

    @Test
    void courthousesPost() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(REQUEST_BODY_HAVERFORDWEST_JSON));
        mockMvc.perform(requestBuilder).andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(notNullValue())))
            .andExpect(jsonPath("$.courthouse_name", is("HAVERFORDWEST")))
            .andExpect(jsonPath("$.code", is(761)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())));
    }

    @Test
    void courthousesPostTwoCourthousesWithSameCode() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(REQUEST_BODY_HAVERFORDWEST_JSON));

        mockMvc.perform(requestBuilder).andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(notNullValue())))
            .andExpect(jsonPath("$.courthouse_name", is("HAVERFORDWEST")))
            .andExpect(jsonPath("$.code", is(761)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())));

        mockMvc.perform(requestBuilder).andExpect(status().isConflict());

    }

    @Test
    void courthousesPostWithMissingCourthouseName() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(REQUEST_BODY_400_MISSING_COURTHOUSE_NAME_JSON));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andReturn();

        assertEquals(
            "{\"violations\":[{\"field\":\"courthouseName\",\"message\":\"must not be null\"}],\"type\":\"https://zalando.github.io/problem/constraint-violation\",\"status\":400,\"title\":\"Constraint Violation\"}",
            response.getResponse().getContentAsString()
        );
    }

    @Test
    void courthousesPostWithMissingCourthouseDisplayName() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(REQUEST_BODY_400_MISSING_COURTHOUSE_DISPLAY_NAME_JSON));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andReturn();

        assertEquals(
            "{\"violations\":[{\"field\":\"displayName\",\"message\":\"must not be null\"}],\"type\":\"https://zalando.github.io/problem/constraint-violation\",\"status\":400,\"title\":\"Constraint Violation\"}",
            response.getResponse().getContentAsString()
        );
    }

    @Test
    void courthousesPut() throws Exception {
        UserAccountEntity user = adminUserStub.givenUserIsAuthorised(mockUserIdentity);
        createEnabledUserAccountEntity(user);

        Integer addedEntityId = addCourthouseAndGetId(REQUEST_BODY_HAVERFORDWEST_JSON);

        String requestBody = getContentsFromFile("tests/CourthousesTest/courthousesPutEndpoint/requestBodyTest.json");
        MockHttpServletRequestBuilder requestBuilder = put("/courthouses/{courthouse_id}", addedEntityId)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        mockMvc.perform(requestBuilder).andExpect(status().isNoContent()).andReturn();

        requestBuilder = get("/admin/courthouses/{courthouse_id}", addedEntityId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.courthouse_name", is("test")))
            .andExpect(jsonPath("$.code", is(9001)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())));

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void courthousesPutWhenIdDoesNotExist() throws Exception {
        String requestBody = getContentsFromFile("tests/CourthousesTest/courthousesPutEndpoint/requestBodyTest.json");
        MockHttpServletRequestBuilder requestBuilder = put("/courthouses/{courthouse_id}", 123)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);

        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());

    }

    @Test
    void courthousesDelete() throws Exception {
        UserAccountEntity user = adminUserStub.givenUserIsAuthorised(mockUserIdentity);
        createEnabledUserAccountEntity(user);

        Integer addedEntityId = addCourthouseAndGetId(REQUEST_BODY_HAVERFORDWEST_JSON);

        MockHttpServletRequestBuilder requestBuilder = delete("/courthouses/{courthouse_id}", addedEntityId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isNoContent()).andReturn();

        requestBuilder = get("/admin/courthouses/{courthouse_id}", addedEntityId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void adminRegionsGet() throws Exception {
        UserAccountEntity user = adminUserStub.givenUserIsAuthorised(mockUserIdentity);
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

        verify(mockUserIdentity).userHasGlobalAccess(Set.of(ADMIN));
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void adminRegionsNotAuthorised() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/admin/regions")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andDo(print()).andReturn();

        assertEquals(403, response.getResponse().getStatus());
    }

    /**
     * Test utility method used to add courthouse to database.
     *
     * @param fileLocation location of file that contains courthouse to be added.
     * @return response for successful add
     */
    private MvcResult makeRequestToAddCourthouseToDatabase(String fileLocation) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(fileLocation));
        return mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andDo(print()).andReturn();
    }

    private Integer addCourthouseAndGetId(String fileLocation) throws Exception {
        MvcResult addedCourthouseResponse = makeRequestToAddCourthouseToDatabase(fileLocation);
        return JsonPath.read(addedCourthouseResponse.getResponse().getContentAsString(), "$.id");
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

}

