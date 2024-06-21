package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseProcessor;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;

public class GenerateCaseDocumentProcessorIntTest extends IntegrationBase {

    private static final OffsetDateTime DT_2025 = OffsetDateTime.of(2025, 1, 1, 1, 0, 0, 0, UTC);

    @SpyBean
    CaseRepository caseRepository;
    @SpyBean
    CaseDocumentRepository caseDocumentRepository;
    @SpyBean
    ExternalObjectDirectoryRepository eodRepository;
    @Autowired
    UserAccountRepository userAccountRepository;
    @Autowired
    AnnotationRepository annotationRepository;
    @Autowired
    GenerateCaseDocumentSingleCaseProcessor processor;
    @Autowired
    HearingRepository hearingRepository;

    @Test
    public void testGenerateCaseDocument() {
        // given
        givenBearerTokenExists("darts.global.user@hmcts.net");
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        CourtCaseEntity courtCase = dartsDatabase.getCourtCaseStub().createAndSaveCourtCaseWithHearings(createdCourtCase -> {
            createdCourtCase.setRetentionUpdated(true);
            createdCourtCase.setRetentionRetries(1);
            createdCourtCase.setClosed(true);
        });

        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        var hearing = courtCase.getHearings().get(0);
        hearing.addMedia(medias.get(0));

        hearingRepository.save(hearing);
        dartsDatabase.getCaseRetentionStub().createCaseRetentionObject(courtCase, DT_2025);

        dartsDatabase.getExternalObjectDirectoryStub().createAndSaveEod(medias.get(0), ARM_DROP_ZONE, ARM, eod -> {});

        var annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(testUser, "TestAnnotation", hearing);
        annotationRepository.save(annotation);

        // when
        processor.processGenerateCaseDocument(courtCase.getId());

        // then
        List<CaseDocumentEntity> caseDocumentEntities = caseDocumentRepository.findByCourtCase(courtCase);
        assertThat(caseDocumentEntities.size()).isEqualTo(1);
        CaseDocumentEntity caseDocument = caseDocumentEntities.get(0);
        assertThat(caseDocument.getCourtCase().getId()).isEqualTo(courtCase.getId());

        List<ExternalObjectDirectoryEntity> eodCaseDocument = eodRepository.findByCaseDocument(caseDocument);
        assertThat(eodCaseDocument.size()).isEqualTo(1);
    }

    private static void givenBearerTokenExists(String email) {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("emails", List.of(email))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
