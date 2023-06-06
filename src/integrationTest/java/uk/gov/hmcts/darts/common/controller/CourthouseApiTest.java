package uk.gov.hmcts.darts.common.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthouse;
import uk.gov.hmcts.darts.courthouse.service.CourthouseService;

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
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert","PMD.AvoidDuplicateLiterals"})
class CourthouseApiTest {

    public static final String REQUEST_BODY_HAVERFORDWEST_JSON = "Tests/CourthousesTest/courthousesPostEndpoint/requestBodyHaverfordwest.json";
    private static final String REQUEST_BODY_SWANSEA_JSON = "Tests/CourthousesTest/courthousesPostEndpoint/requestBodySwansea.json";
    public static final String REQUEST_BODY_400_MISSING_COURTHOUSE_NAME_JSON = "Tests/CourthousesTest/courthousesPostEndpoint/requestBody400_MissingCourthouseName.json";
    @Autowired
    private CourthouseService courthouseService;

    @Autowired
    private CourthouseRepository courthouseRepository;

    @Autowired
    private transient MockMvc mockMvc;

    /**
     * Test adds courthouse and then retrieves to check for equality.
     */
    @Test
    void courthousesGet() throws Exception {
        makeRequestToAddCourthouseToDatabase(
            REQUEST_BODY_HAVERFORDWEST_JSON);
        MockHttpServletRequestBuilder requestBuilder = get("/courthouses/{courthouse_id}", 1)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.courthouse_name", is("HAVERFORDWEST")))
            .andExpect(jsonPath("$.code", is(761)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())));
    }

    /**
     * Test adds multiple courthouses and then retrieves them all to check for equality.
     */
    @Test
    void courthousesGetAll() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        MvcResult haverfordwestResponse = makeRequestToAddCourthouseToDatabase(
            REQUEST_BODY_HAVERFORDWEST_JSON);
        MvcResult swanseaResponse = makeRequestToAddCourthouseToDatabase(REQUEST_BODY_SWANSEA_JSON);


        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andDo(print()).andReturn();

        List<ExtendedCourthouse> courthouseList = objectMapper.readValue(response.getResponse().getContentAsString(), new TypeReference<>() {});
        ExtendedCourthouse haverfordwestCourthouse = objectMapper.readValue(haverfordwestResponse.getResponse().getContentAsString(), ExtendedCourthouse.class);
        ExtendedCourthouse swanseaCourthouse = objectMapper.readValue(swanseaResponse.getResponse().getContentAsString(), ExtendedCourthouse.class);

        assertTrue(courthouseList.contains(swanseaCourthouse));
        assertTrue(courthouseList.contains(haverfordwestCourthouse));
    }

    /**
     * Test adds courthouse and checks if returned object matches.
     */
    @Test
    void courthousesPost() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(REQUEST_BODY_HAVERFORDWEST_JSON));
        mockMvc.perform(requestBuilder).andExpect(status().isCreated())
            .andExpect(jsonPath("$.courthouse_name", is("HAVERFORDWEST")))
            .andExpect(jsonPath("$.code", is(761)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())));
    }

    @Test
    void courthousesPostMissingCourthouseName() throws Exception {
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


    /**
     * Test adds courthouse then amends it and then retrieves it to check amendment was made.
     */
    @Test
    void courthousesPut() throws Exception {
        makeRequestToAddCourthouseToDatabase(
            REQUEST_BODY_HAVERFORDWEST_JSON);

        String requestBody = getContentsFromFile("Tests/CourthousesTest/courthousesPutEndpoint/requestBodySwansea.json");
        MockHttpServletRequestBuilder requestBuilder = put("/courthouses/{courthouse_id}", 1)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        mockMvc.perform(requestBuilder).andExpect(status().isNoContent()).andReturn();

        requestBuilder = get("/courthouses/{courthouse_id}", 1)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.courthouse_name", is("SWANSEA")))
            .andExpect(jsonPath("$.code", is(457)))
            .andExpect(jsonPath("$.created_date_time", is(notNullValue())))
            .andExpect(jsonPath("$.last_modified_date_time", is(notNullValue())));
    }

    /*
        Test adds a courthouse, deletes it and then tries to retrieve it but should get no content response.
     */
    @Test
    void courthousesDelete() throws Exception {
        makeRequestToAddCourthouseToDatabase(
            REQUEST_BODY_HAVERFORDWEST_JSON);

        MockHttpServletRequestBuilder requestBuilder = delete("/courthouses/{courthouse_id}", 1)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isNoContent()).andReturn();

        requestBuilder = get("/courthouses/{courthouse_id}", 1)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().isNoContent());
    }
}
