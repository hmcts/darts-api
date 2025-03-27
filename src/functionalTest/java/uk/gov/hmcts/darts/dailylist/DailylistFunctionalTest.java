package uk.gov.hmcts.darts.dailylist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.darts.FunctionalTest;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.dailylist.model.PatchDailyListRequest;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailylistFunctionalTest extends FunctionalTest {

    public static final String POST_DAILYLIST_URL = "/dailylists";


    ObjectMapper objectMapper;

    @BeforeEach
    void createObjectMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }


    @AfterEach
    void cleanData() {
        buildRequestWithExternalAuth()
            .baseUri(getUri("/functional-tests/clean"))
            .redirects().follow(false)
            .delete();


    }

    @ParameterizedTest
    @EnumSource(names = {"CPP", "XHB"})
    void postDailyList(SourceType sourceType) throws IOException {
        String courthouseName = "FUNC-SWANSEA-HOUSE-" + randomAlphanumeric(7);
        String courtroomName = "FUNC-SWANSEA-ROOM-" + randomAlphanumeric(7);

        createCourtroomAndCourthouse(courthouseName, courtroomName);

        String xmlDocument = getContentsFromFile("DailyList-Document.xml");

        String uniqueId = "FUNC-UNIQUE-ID-" + randomAlphanumeric(7);
        String messageId = "FUNC-UNIQUE-ID-" + randomAlphanumeric(7);

        PostDailyListRequest request = new PostDailyListRequest();
        request.setSourceSystem(sourceType.toString());
        request.setCourthouse(courthouseName);
        request.setHearingDate(LocalDate.of(2020, 10, 10));
        request.setUniqueId(uniqueId);
        request.setPublishedTs(OffsetDateTime.of(2020, 10, 9, 23, 30, 0, 0, ZoneOffset.UTC));
        request.setMessageId(messageId);
        request.setXmlDocument(xmlDocument);

        String requestBody = objectMapper.writeValueAsString(request);

        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .baseUri(getUri(POST_DAILYLIST_URL))
            .redirects().follow(false)
            .post().then().extract().response();

        assertEquals(200, response.getStatusCode());

        Integer dalId = response.jsonPath().get("dal_id");

        String jsonDocument = getJsonDocumentWithValues(request.getPublishedTs(), request.getHearingDate(), uniqueId);

        PatchDailyListRequest patchRequest = new PatchDailyListRequest();
        patchRequest.setDalId(dalId);
        patchRequest.setJsonString(jsonDocument);

        //then patch it with JSON
        response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .body(objectMapper.writeValueAsString(patchRequest))
            .when()
            .baseUri(getUri(POST_DAILYLIST_URL))
            .redirects().follow(false)
            .patch().then().extract().response();

        assertEquals(200, response.getStatusCode());
    }


    private String getJsonDocumentWithValues(OffsetDateTime publishedDate, LocalDate hearingDate, String uniqueId) throws IOException {
        String jsonDocument = getContentsFromFile("test/dailylist/DailylistFunctionalTest/DailyListRequestTemplate.json");

        jsonDocument = jsonDocument.replace("<<PUBLISHED_DATETIME>>", publishedDate.toString());
        jsonDocument = jsonDocument.replace("<<HEARING_DATE>>", hearingDate.toString());
        jsonDocument = jsonDocument.replace("<<CASENUMBER>>", UUID.randomUUID().toString());
        jsonDocument = jsonDocument.replace("<<JUDGENAME>>", UUID.randomUUID().toString());
        jsonDocument = jsonDocument.replace("<<UNIQUEID>>", uniqueId);
        return jsonDocument;
    }

    @Test
    void postNoDocument() throws IOException {

        PostDailyListRequest request = new PostDailyListRequest();
        request.setSourceSystem("XHB");
        request.setCourthouse("Swansea");
        request.setHearingDate(LocalDate.of(2020, 10, 10));
        request.setUniqueId("1111111");
        request.setPublishedTs(OffsetDateTime.of(2020, 10, 9, 23, 30, 0, 0, ZoneOffset.UTC));
        request.setMessageId("some-message-id");

        Response response = buildRequestWithExternalGlobalAccessAuth()
            .contentType(ContentType.JSON)
            .body(objectMapper.writeValueAsString(request))
            .when()
            .baseUri(getUri(POST_DAILYLIST_URL))
            .redirects().follow(false)
            .post().then().extract().response();

        assertEquals(422, response.getStatusCode());
        assertTrue(response.getBody().jsonPath().getString("title").contains("xml_document"));
        assertTrue(response.getBody().jsonPath().getString("title").contains("json_document"));
    }
}
