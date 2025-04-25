package uk.gov.hmcts.darts.cases.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.controller.CaseController;
import uk.gov.hmcts.darts.cases.model.AdminCaseAudioResponseItem;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaTestData;

class AdminCaseServiceIntTest extends PostgresIntegrationBase {

    private CourtCaseEntity courtCaseEntity1;
    private MediaEntity currentMediaEntity1;
    private MediaEntity currentMediaEntity2;
    private MediaEntity currentMediaEntity3;
    private MediaEntity currentMediaEntity4;

    @Autowired
    private AdminCaseService adminCaseService;

    @BeforeEach
    void setupData() {
        var minimalHearing1 = PersistableFactory.getHearingTestData().someMinimalHearing();
        var hearingEntity1 = dartsDatabase.save(minimalHearing1);

        courtCaseEntity1 = hearingEntity1.getCourtCase();

        var media1 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearingEntity1.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));
        currentMediaEntity1 = dartsDatabase.save(media1);

        var media2 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearingEntity1.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                2
            ));
        currentMediaEntity2 = dartsDatabase.save(media2);

        var media3 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearingEntity1.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                3
            ));
        currentMediaEntity3 = dartsDatabase.save(media3);

        var media4 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearingEntity1.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                4
            ));
        currentMediaEntity4 = dartsDatabase.save(media4);

        var media5 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearingEntity1.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T15:00:00Z"),
                OffsetDateTime.parse("2023-09-26T17:45:00Z"),
                1
            ));
        media5.setIsCurrent(false);
        MediaEntity mediaEntityNotCurrent1 = dartsDatabase.save(media5);

        hearingEntity1.addMedia(currentMediaEntity1);
        hearingEntity1.addMedia(currentMediaEntity2);
        hearingEntity1.addMedia(currentMediaEntity3);
        hearingEntity1.addMedia(currentMediaEntity4);
        hearingEntity1.addMedia(mediaEntityNotCurrent1);
        dartsDatabase.save(hearingEntity1);

        var minimalHearing2 = PersistableFactory.getHearingTestData().someMinimalHearing();
        var hearingEntity2 = dartsDatabase.save(minimalHearing2);

        var media6 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearingEntity2.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T15:00:00Z"),
                OffsetDateTime.parse("2023-09-26T17:45:00Z"),
                1
            ));
        MediaEntity currentMediaEntityForHearing2 = dartsDatabase.save(media6);
        hearingEntity2.addMedia(currentMediaEntityForHearing2);
        dartsDatabase.save(hearingEntity2);
    }

    @Test
    void getAudiosByCaseId_ShouldReturnPaginatedListWithMultiplePages() {
        // given
        PaginationDto<AdminCaseAudioResponseItem> paginationDto = new PaginationDto<>(
            CaseController.AdminCaseIdAudioGetPaginatedResponse::new,
            1,
            3,
            PaginationDto.toSortBy(List.of("audioId", "courtroom", "startTime", "endTime", "channel")),
            PaginationDto.toSortDirection(List.of("ASC", "ASC", "ASC", "ASC", "ASC"))
        );

        // when
        var results = adminCaseService.getAudiosByCaseId(courtCaseEntity1.getId(), paginationDto);

        // then
        assertThat(results.getTotalItems()).isEqualTo(4);
        assertThat(results.getTotalPages()).isEqualTo(2);
        assertThat(results.getData()).hasSize(3);

        List<AdminCaseAudioResponseItem> data = results.getData();
        assertThat(data.getFirst().getId()).isEqualTo(currentMediaEntity1.getId());
        assertThat(data.getFirst().getChannel()).isEqualTo(1);
        assertThat(data.get(1).getId()).isEqualTo(currentMediaEntity2.getId());
        assertThat(data.get(1).getChannel()).isEqualTo(2);
        assertThat(data.get(2).getId()).isEqualTo(currentMediaEntity3.getId());
        assertThat(data.get(2).getChannel()).isEqualTo(3);

    }

    @Test
    void getAudiosByCaseId_ShouldReturnPaginatedListByChannelAndStartTimeDesc() {
        // given
        PaginationDto<AdminCaseAudioResponseItem> paginationDto = new PaginationDto<>(
            CaseController.AdminCaseIdAudioGetPaginatedResponse::new,
            1,
            3,
            PaginationDto.toSortBy(List.of("channel", "startTime")),
            PaginationDto.toSortDirection(List.of("DESC", "DESC"))
        );

        // when
        var results = adminCaseService.getAudiosByCaseId(courtCaseEntity1.getId(), paginationDto);

        // then
        assertThat(results.getTotalItems()).isEqualTo(4);
        assertThat(results.getTotalPages()).isEqualTo(2);
        assertThat(results.getData()).hasSize(3);

        List<AdminCaseAudioResponseItem> data = results.getData();
        assertThat(data.getFirst().getId()).isEqualTo(currentMediaEntity4.getId());
        assertThat(data.getFirst().getChannel()).isEqualTo(4);
        assertThat(data.get(1).getId()).isEqualTo(currentMediaEntity3.getId());
        assertThat(data.get(1).getChannel()).isEqualTo(3);
        assertThat(data.get(2).getId()).isEqualTo(currentMediaEntity2.getId());
        assertThat(data.get(2).getChannel()).isEqualTo(2);

    }

}
