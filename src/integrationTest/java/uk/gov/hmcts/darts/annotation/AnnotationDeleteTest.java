package uk.gov.hmcts.darts.annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.EnumSource.Mode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@AutoConfigureMockMvc
class AnnotationDeleteTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/annotations");

    @Autowired
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void judgeWithGlobalAccessCanDeleteTheirOwnAnnotation() throws Exception {
        var judge = given.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIARY);
        var annotation = someAnnotationNotMarkedForDeletionCreatedBy(judge);

        mockMvc.perform(
                delete(ENDPOINT + "/" + annotation.getId()))
            .andExpect(status().isNoContent())
            .andReturn();

        var annotationEntity = dartsDatabase.findAnnotationById(annotation.getId());
        var lastModifiedByUserId = dartsDatabase.getLastModifiedByUserId(annotationEntity);
        assertThat(annotationEntity).hasFieldOrPropertyWithValue("deleted", true);
        assertThat(lastModifiedByUserId).isEqualTo(judge.getId());
    }

    @Test
    void judgeWithCourthouseAccessCanDeleteTheirOwnAnnotation() throws Exception {
        var annotation = transactionalUtil.executeInTransaction(() -> {
            var hearing = dartsPersistence.save(PersistableFactory.getHearingTestData().someMinimalHearing());
            var judge = given.anAuthenticatedUserAuthorizedForCourthouse(JUDICIARY, hearing.getCourtroom().getCourthouse());
            return someAnnotationForHearingNotMarkedForDeletionCreatedBy(judge, hearing);
        });
        mockMvc.perform(
                delete(ENDPOINT + "/" + annotation.getId()))
            .andExpect(status().isNoContent())
            .andReturn();
    }

    @Test
    void preventsJudgeNotAuthorizedForCourthouseDeletingAnnotationAssociatedWithThatCourthouse() throws Exception {
        var annotation = transactionalUtil.executeInTransaction(() -> {
            var annotationHearing = dartsPersistence.save(PersistableFactory.getHearingTestData().someMinimalHearing());

            var someOtherCourthouse = dartsPersistence.save(someMinimalCourthouse());
            var judge = given.anAuthenticatedUserAuthorizedForCourthouse(JUDICIARY, someOtherCourthouse);
            return someAnnotationForHearingNotMarkedForDeletionCreatedBy(judge, annotationHearing);
        });
        mockMvc.perform(
                delete(ENDPOINT + "/" + annotation.getId()))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void preventsJudgesFromDeletingAnotherJudgesAnnotations() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIARY);
        var someOtherJudge = minimalUserAccount();
        var annotation = someAnnotationNotMarkedForDeletionCreatedBy(someOtherJudge);

        mockMvc.perform(
                delete(ENDPOINT + "/" + annotation.getId()))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void allowsDeleteAnnotationBySuperAdmin() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var someJudge = minimalUserAccount();
        var annotation = someAnnotationNotMarkedForDeletionCreatedBy(someJudge);

        mockMvc.perform(
                delete(ENDPOINT + "/" + annotation.getId()))
            .andExpect(status().isNoContent())
            .andReturn();
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN", "JUDICIARY"}, mode = Mode.EXCLUDE)
    void disallowsDeleteAnnotationByRolesOtherThanSuperAdminAndJudge(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);
        var someJudge = minimalUserAccount();
        var annotation = someAnnotationNotMarkedForDeletionCreatedBy(someJudge);

        mockMvc.perform(
                delete(ENDPOINT + "/" + annotation.getId()))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void returns404IfAnnotationDoesntExist() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        mockMvc.perform(
                delete(ENDPOINT + "/" + "-1"))
            .andExpect(status().isNotFound())
            .andReturn();
    }

    private AnnotationEntity someAnnotationNotMarkedForDeletionCreatedBy(UserAccountEntity userAccount) {
        return someAnnotationForHearingNotMarkedForDeletionCreatedBy(userAccount, PersistableFactory.getHearingTestData().someMinimalHearing());
    }

    private AnnotationEntity someAnnotationForHearingNotMarkedForDeletionCreatedBy(UserAccountEntity userAccount, HearingEntity hearing) {
        var annotation = PersistableFactory.getAnnotationTestData().minimalAnnotationEntity();
        annotation.setDeleted(false);
        annotation.setCurrentOwner(userAccount);
        annotation.setCreatedBy(userAccount);
        annotation.setLastModifiedBy(userAccount);
        annotation.addHearing(dartsPersistence.refresh(hearing));
        dartsPersistence.save(annotation);
        return annotation;
    }
}