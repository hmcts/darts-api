package uk.gov.hmcts.darts.annotation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;

@AutoConfigureMockMvc
class AnnotationPostTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/annotations");

    private final BasicJsonTester json = new BasicJsonTester(getClass());

    @Autowired
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsAnnotationId() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIARY);
        HearingEntity hearingEntity = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearingEntity = dartsPersistence.save(hearingEntity);

        var mvcResult = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(someAnnotationPostDocument())
                    .file(someAnnotationPostBodyFor(hearingEntity)))
            .andExpect(status().isOk())
            .andReturn();

        var response = mvcResult.getResponse().getContentAsString();
        assertThat(json.from(response)).hasJsonPathNumberValue("annotation_id");

        Integer annotationId = JsonPath.parse(response).read("$.annotation_id");
        assertNotNull(annotationId);

        Optional<AnnotationEntity> annotation = dartsDatabase.getAnnotationRepository().findById(annotationId);
        assertTrue(annotation.isPresent());

        List<AnnotationEntity> annotationByHearing = dartsDatabase.getAnnotationRepository().findByHearingId(hearingEntity.getId());
        assertFalse(annotationByHearing.isEmpty());

        assertThat(dartsDataRetrieval.findExternalObjectDirectoryFor(annotationId).size()).isEqualTo(2);
    }

    @Test
    void allowsJudgeWithGlobalAccessToUploadAnnotations() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIARY);
        HearingEntity hearingEntity = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearingEntity = dartsPersistence.save(hearingEntity);
        mockMvc.perform(
                multipart(ENDPOINT)
                    .file(someAnnotationPostDocument())
                    .file(someAnnotationPostBodyFor(hearingEntity)))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void allowsJudgeAuthorisedForCourthouseAccessToUploadAnnotations() throws Exception {
        var hearing = dartsPersistence.save(PersistableFactory.getHearingTestData().someMinimalHearing());
        given.anAuthenticatedUserAuthorizedForCourthouse(JUDICIARY, hearing.getCourtroom().getCourthouse());

        mockMvc.perform(
                multipart(ENDPOINT)
                    .file(someAnnotationPostDocument())
                    .file(someAnnotationPostBodyFor(hearing)))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void returns400IfAnnotationDocumentMissing() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIARY);

        mockMvc.perform(
                multipart(ENDPOINT)
                    .file(someAnnotationPostBodyFor(PersistableFactory.getHearingTestData().someMinimalHearing())))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    void returns400IfPostBodyMissing() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIARY);

        mockMvc.perform(
                multipart(ENDPOINT)
                    .file(someAnnotationPostDocument()))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    void returns400WhenHearingIdIsNull() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIARY);

        mockMvc.perform(
                multipart(ENDPOINT)
                    .file(someAnnotationPostBodyNullHearingId())
                    .file(someAnnotationPostDocument()))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    private MockMultipartFile someAnnotationPostBodyFor(HearingEntity hearingEntity) throws JsonProcessingException {
        var annotation = new Annotation(hearingEntity.getId(),null);
        annotation.setComment("some comment");

        return new MockMultipartFile(
            "annotation",
            null,
            "application/json",
            objectMapper.writeValueAsString(annotation).getBytes()
        );
    }

    private MockMultipartFile someAnnotationPostBodyNullHearingId() throws JsonProcessingException {
        var annotation = new Annotation(null,null);
        return new MockMultipartFile(
            "annotation",
            null,
            "application/json",
            objectMapper.writeValueAsString(annotation).getBytes()
        );
    }

    private static MockMultipartFile someAnnotationPostDocument() {
        return new MockMultipartFile(
            "file",
            "some-filename.doc",
            "application/msword",
            "some-content".getBytes()
        );
    }

}