package uk.gov.hmcts.darts.audio.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MediaRepositoryIntTest extends IntegrationBase {

    public static final LocalDateTime DATE_NOW = DateConverterUtil.toLocalDateTime(OffsetDateTime.now());

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
        var caseA = caseStub.createAndSaveMinimalCourtCase();
        var caseB = caseStub.createAndSaveMinimalCourtCase();

        var hearA1 = hearingStub.createHearing(caseA.getCourthouse().getCourthouseName(), "testCourtroom", caseA.getCaseNumber(), DATE_NOW);
        var hearA2 = hearingStub.createHearing(caseA.getCourthouse().getCourthouseName(), "testCourtroom2", caseA.getCaseNumber(), DATE_NOW);
        var hearA3 = hearingStub.createHearing(caseA.getCourthouse().getCourthouseName(), "testCourtroom", caseA.getCaseNumber(), DATE_NOW);
        var hearB = hearingStub.createHearing(caseB.getCourthouse().getCourthouseName(), "testCourtroom", caseB.getCaseNumber(), DATE_NOW);
        caseA.setHearings(List.of(hearA1, hearA2, hearA3));
        caseB.setHearings(List.of(hearB));
        caseRepository.save(caseA);
        caseRepository.save(caseB);

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
