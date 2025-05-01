package uk.gov.hmcts.darts.cases.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.util.List;

class AdminCaseServiceImplTest {

    @Mock
    private CaseService caseService;
    @Mock
    private MediaRepository mediaRepository;

    private final int mediaId = 123;
    private MediaEntity mediaEntity;

    private AdminCaseServiceImpl adminCaseService;

    @BeforeEach
    void setUp() {
        adminCaseService = new AdminCaseServiceImpl(caseService, mediaRepository);

        mediaEntity = PersistableFactory.getMediaTestData().someMinimal();
        mediaEntity.setId(mediaId);
    }

    @Test
    void getAudiosByCaseId_WithPagination() {
        CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setId(222);

        MediaLinkedCaseEntity mediaLinkedCase = new MediaLinkedCaseEntity();
        mediaLinkedCase.setCourtCase(courtCaseEntity);

        mediaEntity.setMediaLinkedCaseList(List.of(mediaLinkedCase));
        //doNothing().when(caseService).getCourtCaseById(courtCaseEntity.getId());
        //when(mediaRepository.findByCaseIdAndIsCurrentTrue(222)).thenReturn(List.of(mediaEntity));

    }
}