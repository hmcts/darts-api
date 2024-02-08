package uk.gov.hmcts.darts.annotation;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createSomeMinimalHearing;
import static uk.gov.hmcts.darts.testutils.data.SecurityGroupTestData.buildGroupForRole;
import static uk.gov.hmcts.darts.testutils.data.SecurityGroupTestData.buildGroupForRoleAndCourthouse;
import static uk.gov.hmcts.darts.testutils.data.UserAccountTestData.minimalUserAccount;

@AutoConfigureMockMvc
class AnnotationControllerTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/annotations");

    private final BasicJsonTester json = new BasicJsonTester(getClass());

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsAnnotationId() throws Exception {
        createAuthenticatedJudgeWithGlobalAccessAndEmail("judge@global.com");

        MvcResult mvcResult = mockMvc.perform(
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
        createAuthenticatedJudgeWithGlobalAccessAndEmail("judge@global.com");

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


    private static MockMultipartFile someAnnotationPostDocument() {
        return new MockMultipartFile(
            "file",
            "some-filename.txt",
            "some-content-type",
            "some-content".getBytes()
        );
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

    private void createAuthenticatedJudgeWithGlobalAccessAndEmail(String email) {
        Jwt jwt = Jwt.withTokenValue("some-token")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(email))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        var securityGroup = buildGroupForRole(JUDGE);
        securityGroup.setGlobalAccess(true);

        var judge = minimalUserAccount();
        judge.setEmailAddress(email);

        dartsDatabase.addUserToGroup(judge, securityGroup);
        dartsDatabase.addToTrash(securityGroup);
        dartsDatabase.addToUserAccountTrash(email);
    }

    private void createAuthenticatedJudgeAuthorizedForCourthouse(String email, CourthouseEntity courthouse) {
        Jwt jwt = Jwt.withTokenValue("some-token")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(email))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        var securityGroup = buildGroupForRoleAndCourthouse(JUDGE, courthouse);
        dartsDatabase.addToTrash(securityGroup);

        var judge = minimalUserAccount();
        judge.setEmailAddress(email);
        dartsDatabase.addToUserAccountTrash(email);

        dartsDatabase.addUserToGroup(judge, securityGroup);
    }
}
