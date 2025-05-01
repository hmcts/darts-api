package uk.gov.hmcts.darts.cases.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.cases.model.AdminCaseAudioResponseItem;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminCaseAudioResponseItemMapperTest {

    @Test
    void mapToAdminCaseAudioResponseItems_shouldMapListCorrectly() {
        // Given
        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimalHearing();

        MediaEntity mediaEntity1 = PersistableFactory.getMediaTestData().createMediaWith(
            hearing.getCourtroom(),
            OffsetDateTime.parse("2023-09-26T13:00:00Z"),
            OffsetDateTime.parse("2023-09-26T13:45:00Z"),
            1
        );
        mediaEntity1.setId(1);

        MediaEntity mediaEntity2 = PersistableFactory.getMediaTestData().createMediaWith(
            hearing.getCourtroom(),
            OffsetDateTime.parse("2023-09-26T13:00:00Z"),
            OffsetDateTime.parse("2023-09-26T13:45:00Z"),
            2
        );
        mediaEntity2.setId(2);

        // When
        List<AdminCaseAudioResponseItem> result = AdminCaseAudioResponseItemMapper.mapToAdminCaseAudioResponseItems(List.of(mediaEntity1, mediaEntity2));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getChannel()).isEqualTo("Channel1");
        assertThat(result.get(0).getCourtroom()).isEqualTo("Courtroom1");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getChannel()).isEqualTo("Channel2");
        assertThat(result.get(1).getCourtroom()).isEqualTo("Courtroom2");
    }

    @Test
    void mapToAdminCaseAudioResponseItems_shouldHandleNullList() {
        List<AdminCaseAudioResponseItem> result = AdminCaseAudioResponseItemMapper.mapToAdminCaseAudioResponseItems(null);

        assertThat(result).isEmpty();
    }

}