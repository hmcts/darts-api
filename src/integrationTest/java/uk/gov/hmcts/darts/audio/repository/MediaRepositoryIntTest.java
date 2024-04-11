package uk.gov.hmcts.darts.audio.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MediaRepositoryIntTest extends IntegrationBase {

    public static final LocalDate DATE_NOW = OffsetDateTime.now().toLocalDate();

    @Autowired
    CaseRepository caseRepository;
    @Autowired
    HearingStub hearingStub;
    @Autowired
    HearingRepository hearingRepository;
    @Autowired
    CourtCaseStub caseStub;

    @Autowired
    MediaRepository mediaRepository;

    @Test
    void testFindMediasByCaseId() {

        // given
        var caseA = caseStub.createAndSaveCourtCaseWithHearings();
        var caseB = caseStub.createAndSaveCourtCaseWithHearings();

        var hearA1 = caseA.getHearings().get(0);
        var hearA2 = caseA.getHearings().get(1);
        var hearA3 = caseA.getHearings().get(2);
        var hearB = caseB.getHearings().get(0);

        var medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        hearA1.addMedia(medias.get(0));
        hearA1.addMedia(medias.get(1));
        hearA2.addMedia(medias.get(2));
        hearB.addMedia(medias.get(0));
        hearingRepository.save(hearA2);
        hearingRepository.save(hearA3);
        hearingRepository.save(hearA1);
        hearingRepository.save(hearB);

        // when
        var caseAMedias = mediaRepository.findAllByCaseId(caseA.getId());
        var caseBMedias = mediaRepository.findAllByCaseId(caseB.getId());

        // then
        var caseAMediasId = caseAMedias.stream().map(MediaEntity::getId);
        assertThat(caseAMediasId).containsOnly(medias.get(0).getId(), medias.get(1).getId(), medias.get(2).getId());
        var caseBMediasId = caseBMedias.stream().map(MediaEntity::getId);
        assertThat(caseBMediasId).containsOnly(medias.get(0).getId());
    }
}
