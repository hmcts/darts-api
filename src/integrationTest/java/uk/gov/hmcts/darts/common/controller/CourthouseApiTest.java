package uk.gov.hmcts.darts.common.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthouse;
import uk.gov.hmcts.darts.courthouse.service.CourthouseService;

import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class CourthouseApiTest {

    public static final String REQUEST_BODY_HAVERFORDWEST_JSON = "tests/CourthousesTest/courthousesPostEndpoint/requestBodyHaverfordwest.json";
    public static final String REQUEST_BODY_400_MISSING_COURTHOUSE_NAME_JSON = "tests/CourthousesTest/courthousesPostEndpoint/requestBody400_MissingCourthouseName.json";
    private static final String REQUEST_BODY_TEST_JSON = "tests/CourthousesTest/courthousesPostEndpoint/requestBodyTest.json";
    @Autowired
    private CourthouseService courthouseService;

    @Autowired
    private CourthouseRepository courthouseRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private transient MockMvc mockMvc;

    @Test
    void courthousesGet() throws Exception {
        Integer addedId = addCourthouseAndGetId(REQUEST_BODY_HAVERFORDWEST_JSON);

        MockHttpServletRequestBuilder requestBuilder = get("/courthouses/{courthouse_id}", addedId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.courthouse_name", is("HAVERFORDWEST")))
            .andExpect(jsonPath("$.code", is(761)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())));
    }

    @Test
    void courthousesGetNonExistingId() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/courthouses/{courthouse_id}", 900)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());
    }

    @Test
    void courthousesGetAll() throws Exception {

        MvcResult haverfordwestResponse = makeRequestToAddCourthouseToDatabase(
            REQUEST_BODY_HAVERFORDWEST_JSON);
        MvcResult swanseaResponse = makeRequestToAddCourthouseToDatabase(REQUEST_BODY_TEST_JSON);


        ExtendedCourthouse haverfordwestCourthouse = objectMapper.readValue(haverfordwestResponse.getResponse().getContentAsString(), ExtendedCourthouse.class);
        ExtendedCourthouse swanseaCourthouse = objectMapper.readValue(swanseaResponse.getResponse().getContentAsString(), ExtendedCourthouse.class);

        // Truncate created and modified to milliseconds as the post (saveAndFlush) returns a more precise timestamp
        haverfordwestCourthouse.setCreatedDateTime(haverfordwestCourthouse.getCreatedDateTime().truncatedTo(ChronoUnit.MILLIS));
        haverfordwestCourthouse.setLastModifiedDateTime(haverfordwestCourthouse.getLastModifiedDateTime().truncatedTo(ChronoUnit.MILLIS));
        swanseaCourthouse.setCreatedDateTime(swanseaCourthouse.getCreatedDateTime().truncatedTo(ChronoUnit.MILLIS));
        swanseaCourthouse.setLastModifiedDateTime(swanseaCourthouse.getLastModifiedDateTime().truncatedTo(ChronoUnit.MILLIS));

        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andDo(print()).andReturn();

        List<ExtendedCourthouse> courthouseList = objectMapper.readValue(response.getResponse().getContentAsString(), new TypeReference<>() {});
        for (ExtendedCourthouse extendedCourthouse : courthouseList) {
            extendedCourthouse.setCreatedDateTime(extendedCourthouse.getCreatedDateTime().truncatedTo(ChronoUnit.MILLIS));
            extendedCourthouse.setLastModifiedDateTime(extendedCourthouse.getLastModifiedDateTime().truncatedTo(ChronoUnit.MILLIS));
        }
        assertTrue(courthouseList.contains(haverfordwestCourthouse),haverfordwestResponse.getResponse().getContentAsString());
        assertTrue(courthouseList.contains(swanseaCourthouse),swanseaResponse.getResponse().getContentAsString());
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
    void courthousesPosWithtMissingCourthouseName() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(REQUEST_BODY_400_MISSING_COURTHOUSE_NAME_JSON));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andReturn();
        assertEquals("{\"code\":\"400 BAD_REQUEST\",\"message\":\"courthouseName must not be null\"}", response.getResponse().getContentAsString());
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

    @Test
    void courthousesPut() throws Exception {
        Integer addedEntityId = addCourthouseAndGetId(REQUEST_BODY_HAVERFORDWEST_JSON);

        String requestBody = getContentsFromFile("tests/CourthousesTest/courthousesPutEndpoint/requestBodyTest.json");
        MockHttpServletRequestBuilder requestBuilder = put("/courthouses/{courthouse_id}", addedEntityId)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        mockMvc.perform(requestBuilder).andExpect(status().isNoContent()).andReturn();

        requestBuilder = get("/courthouses/{courthouse_id}", addedEntityId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.courthouse_name", is("test")))
            .andExpect(jsonPath("$.code", is(9001)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())));
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
        Integer addedEntityId = addCourthouseAndGetId(REQUEST_BODY_HAVERFORDWEST_JSON);

        MockHttpServletRequestBuilder requestBuilder = delete("/courthouses/{courthouse_id}", addedEntityId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isNoContent()).andReturn();

        requestBuilder = get("/courthouses/{courthouse_id}", addedEntityId)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());
    }
}

