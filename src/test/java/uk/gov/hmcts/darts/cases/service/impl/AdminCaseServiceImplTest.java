package uk.gov.hmcts.darts.cases.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.cases.controller.CaseController;
import uk.gov.hmcts.darts.cases.model.AdminCaseAudioResponseItem;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.util.pagination.PaginatedList;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCaseServiceImplTest {

    @Mock
    private CaseService caseService;
    @Mock
    private MediaRepository mediaRepository;

    private AdminCaseServiceImpl adminCaseService;

    @BeforeEach
    void setUp() {
        adminCaseService = new AdminCaseServiceImpl(caseService, mediaRepository);
    }

    @Test
    void getAudiosByCaseId_ShouldReturnPaginatedListWithSingleItem() {
        // given
        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimal();
        courtCaseEntity.setId(222);

        MediaEntity mediaEntity = PersistableFactory.getMediaTestData().someMinimal();
        mediaEntity.setId(111L);

        MediaLinkedCaseEntity mediaLinkedCase = new MediaLinkedCaseEntity();
        mediaLinkedCase.setCourtCase(courtCaseEntity);

        mediaEntity.setMediaLinkedCaseList(List.of(mediaLinkedCase));

        PaginationDto<AdminCaseAudioResponseItem> paginationDto = new PaginationDto<>(
            CaseController.AdminCaseIdAudioGetPaginatedResponse::new,
            1,
            5,
            PaginationDto.toSortBy(List.of("audioId", "courtroom", "startTime", "endTime", "channel")),
            PaginationDto.toSortDirection(List.of("ASC", "ASC", "ASC", "ASC", "ASC"))
        );

        Page<AdminCaseAudioResponseItem> mediaPage = new PageImpl<>(
            mapToAdminCaseAudioResponseItems(List.of(mediaEntity)));
        when(caseService.getCourtCaseById(courtCaseEntity.getId())).thenReturn(courtCaseEntity);
        when(mediaRepository.findByCaseIdAndIsCurrentTruePageable(eq(courtCaseEntity.getId()), any(Pageable.class))).thenReturn(mediaPage);

        // when
        var results = adminCaseService.getAudiosByCaseId(courtCaseEntity.getId(), paginationDto);

        // then
        assertThat(results.getTotalItems()).isEqualTo(1);
        assertThat(results.getData()).hasSize(1);
        assertThat(results.getData().get(0).getId()).isEqualTo(mediaEntity.getId());
    }

    @Test
    void getAudiosByCaseId_ShouldReturnPaginatedListWithMultipleItems() {
        // given
        Integer caseId = 123;

        CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setId(caseId);

        HearingEntity hearing1 = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearing1.setId(1);
        hearing1.setCourtCase(courtCaseEntity);

        MediaEntity media1 = PersistableFactory.getMediaTestData().someMinimal();
        media1.setId(111L);
        media1.setChannel(1);
        media1.setCourtroom(hearing1.getCourtroom());
        media1.setIsCurrent(true);

        MediaEntity media2 = PersistableFactory.getMediaTestData().someMinimal();
        media2.setId(222L);
        media2.setChannel(2);
        media2.setCourtroom(hearing1.getCourtroom());
        media2.setIsCurrent(true);

        MediaEntity media3 = PersistableFactory.getMediaTestData().someMinimal();
        media3.setId(333L);
        media3.setChannel(3);
        media3.setCourtroom(hearing1.getCourtroom());
        media3.setIsCurrent(true);

        hearing1.addMedia(media1);
        hearing1.addMedia(media2);
        hearing1.addMedia(media3);

        Page<AdminCaseAudioResponseItem> mediaPage = new PageImpl<>(
            mapToAdminCaseAudioResponseItems(List.of(media1, media2, media3)));

        when(caseService.getCourtCaseById(caseId)).thenReturn(courtCaseEntity);
        when(mediaRepository.findByCaseIdAndIsCurrentTruePageable(eq(caseId), any(Pageable.class))).thenReturn(mediaPage);
        PaginationDto<AdminCaseAudioResponseItem> paginationDto = new PaginationDto<>(
            CaseController.AdminCaseIdAudioGetPaginatedResponse::new,
            1,
            3,
            PaginationDto.toSortBy(List.of("audioId", "courtroom", "startTime", "endTime", "channel")),
            PaginationDto.toSortDirection(List.of("ASC", "ASC", "ASC", "ASC", "ASC")
            ));

        // when
        PaginatedList<AdminCaseAudioResponseItem> result = adminCaseService.getAudiosByCaseId(caseId, paginationDto);

        // then
        assertThat(result.getTotalItems()).isEqualTo(3);
        assertThat(result.getData()).hasSize(3);
        assertThat(result.getData().getFirst().getId()).isEqualTo(media1.getId());
        assertThat(result.getData().get(1).getId()).isEqualTo(media2.getId());
        assertThat(result.getData().get(2).getId()).isEqualTo(media3.getId());

        verify(caseService).getCourtCaseById(caseId);
        verify(mediaRepository).findByCaseIdAndIsCurrentTruePageable(eq(caseId), any(Pageable.class));
    }

    private List<AdminCaseAudioResponseItem> mapToAdminCaseAudioResponseItems(List<MediaEntity> mediaEntities) {
        return emptyIfNull(mediaEntities).stream()
            .map(this::mapToAdminCaseAudioResponseItem)
            .toList();
    }

    private AdminCaseAudioResponseItem mapToAdminCaseAudioResponseItem(MediaEntity mediaEntity) {
        AdminCaseAudioResponseItem adminCaseAudioResponseItem = new AdminCaseAudioResponseItem();
        adminCaseAudioResponseItem.setId(mediaEntity.getId());
        adminCaseAudioResponseItem.channel(mediaEntity.getChannel());
        adminCaseAudioResponseItem.setStartAt(mediaEntity.getStart());
        adminCaseAudioResponseItem.setEndAt(mediaEntity.getEnd());
        adminCaseAudioResponseItem.setCourtroom(mediaEntity.getCourtroom().getName());
        return adminCaseAudioResponseItem;

    }
}