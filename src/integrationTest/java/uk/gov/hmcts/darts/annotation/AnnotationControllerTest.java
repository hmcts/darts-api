package uk.gov.hmcts.darts.annotation;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createSomeMinimalHearing;
import static uk.gov.hmcts.darts.testutils.data.SecurityGroupTestData.buildGroupForRole;
import static uk.gov.hmcts.darts.testutils.data.SecurityGroupTestData.buildGroupForRoleAndCourthouse;
import static uk.gov.hmcts.darts.testutils.data.UserAccountTestData.minimalUserAccount;

@AutoConfigureMockMvc
@Transactional
class AnnotationControllerTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/annotations");

    private static final String ANNOTATION_DOCUMENT_ENDPOINT = "/annotations/{annotation_id}/documents/{annotation_document_id}";

    private final BasicJsonTester json = new BasicJsonTester(getClass());

    @Mock
    private DataManagementApi dataManagementApi;

    @Mock
    private InputStream inputStreamResource;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void beforeEach() {
        createAuthenticatedJudgeWithGlobalAccessAndEmail("judge@global.com");
    }

    @Test
    void returnsAnnotationId() throws Exception {

        var mvcResult = mockMvc.perform(
                multipart(ENDPOINT)
                    .file(someAnnotationPostDocument())
                    .file(someAnnotationPostBodyFor(createSomeMinimalHearing())))
            .andExpect(status().isOk())
            .andReturn();

        var response = mvcResult.getResponse().getContentAsString();
        assertThat(json.from(response)).hasJsonPathNumberValue("annotation_id");
    }

    @Test
    void allowsJudgeWithGlobalAccessToUploadAnnotations() throws Exception {

        mockMvc.perform(
                multipart(ENDPOINT)
                    .file(someAnnotationPostDocument())
                    .file(someAnnotationPostBodyFor(createSomeMinimalHearing())))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void allowsJudgeAuthorisedForCourthouseAccessToUploadAnnotations() throws Exception {
        var hearing = dartsDatabase.save(createSomeMinimalHearing());
        createAuthenticatedJudgeAuthorizedForCourthouse("judge@global.com", hearing.getCourtroom().getCourthouse());

        mockMvc.perform(
                multipart(ENDPOINT)
                    .file(someAnnotationPostDocument())
                    .file(someAnnotationPostBodyFor(hearing)))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void shouldThrowHttp404ForValidJudgeAndInvalidAnnotationDocumentEntity() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ANNOTATION_DOCUMENT_ENDPOINT, 1, 1);

        mockMvc.perform(
                        requestBuilder)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("ANNOTATION_104"))
                .andExpect(jsonPath("$.title").value("invalid annotation id or annotation document id"))
                .andExpect(jsonPath("$.status").value("404"))
                .andReturn();
    }

    @Test
    void shouldDownloadAnnotationDocument() throws Exception {


        var binaryData = mock(BinaryData.class);
        lenient().when(dataManagementApi.getBlobDataFromInboundContainer(any(UUID.class))).thenReturn(binaryData);
        lenient().when(binaryData.toStream()).thenReturn(inputStreamResource);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        String testAnnotation = "TestAnnotation";
        AnnotationEntity annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(testUser, testAnnotation);

        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = OffsetDateTime.now();
        final String checksum = "123";
        AnnotationDocumentEntity annotationDocument = dartsDatabase.getAnnotationStub()
                .createAndSaveAnnotationDocumentEntityWith(annotation, fileName, fileType, fileSize,
                                                           testUser, uploadedDateTime, checksum
                );

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
                annotationDocument,
                dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
                dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
                UUID.fromString("665e00c8-5b82-4392-8766-e0c982f603d3")
        );
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        MockHttpServletRequestBuilder requestBuilder = get(ANNOTATION_DOCUMENT_ENDPOINT, 1, 1);

        mockMvc.perform(
                        requestBuilder)
                .andExpect(status().isOk())
                .andExpect(header().string(
                        CONTENT_DISPOSITION,
                        "attachment; filename=\"judges-notes.txt\""
                ))
                .andExpect(header().string(
                        CONTENT_TYPE,
                        "application/zip"
                ))
                .andExpect(header().string(
                        "external_location",
                        "665e00c8-5b82-4392-8766-e0c982f603d3"
                ))
                .andExpect(header().string(
                        "annotation_document_id",
                        "1"
                ));

    }

    @Test
    void returns400IfAnnotationDocumentMissing() throws Exception {

        mockMvc.perform(
                multipart(ENDPOINT)
                    .file(someAnnotationPostBodyFor(createSomeMinimalHearing())))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    void returns400IfPostBodyMissing() throws Exception {

        mockMvc.perform(
                multipart(ENDPOINT)
                    .file(someAnnotationPostDocument()))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    void returns400WhenHearingIdIsNull() throws Exception {

        mockMvc.perform(
                multipart(ENDPOINT)
                    .file(someAnnotationPostBodyNullHearingId())
                    .file(someAnnotationPostDocument()))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    private MockMultipartFile someAnnotationPostBodyFor(HearingEntity hearingEntity) throws JsonProcessingException {
        dartsDatabase.save(hearingEntity);
        var annotation = new Annotation(hearingEntity.getId());
        annotation.setComment("some comment");

        return new MockMultipartFile(
            "annotation",
            null,
            "application/json",
            objectMapper.writeValueAsString(annotation).getBytes()
        );
    }

    private MockMultipartFile someAnnotationPostBodyNullHearingId() throws JsonProcessingException {
        var annotation = new Annotation(null);
        return new MockMultipartFile(
            "annotation",
            null,
            "application/json",
            objectMapper.writeValueAsString(annotation).getBytes()
        );
    }

    private void createAuthenticatedJudgeWithGlobalAccessAndEmail(String email) {
        authenticateUserWithEmail(email);

        var securityGroup = buildGroupForRole(JUDGE);
        securityGroup.setGlobalAccess(true);

        var judge = minimalUserAccount();
        judge.setEmailAddress(email);

        dartsDatabase.addUserToGroup(judge, securityGroup);
        dartsDatabase.addToTrash(securityGroup);
        dartsDatabase.addToUserAccountTrash(email);
    }

    private void createAuthenticatedJudgeAuthorizedForCourthouse(String email, CourthouseEntity courthouse) {
        authenticateUserWithEmail(email);

        var securityGroup = buildGroupForRoleAndCourthouse(JUDGE, courthouse);
        dartsDatabase.addToTrash(securityGroup);

        var judge = minimalUserAccount();
        judge.setEmailAddress(email);
        dartsDatabase.addToUserAccountTrash(email);

        dartsDatabase.addUserToGroup(judge, securityGroup);
    }

    private static void authenticateUserWithEmail(String email) {
        var jwt = Jwt.withTokenValue("some-token")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(email))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private static MockMultipartFile someAnnotationPostDocument() {
        return new MockMultipartFile(
            "file",
            "some-filename.txt",
            "some-content-type",
            "some-content".getBytes()
        );
    }
}
