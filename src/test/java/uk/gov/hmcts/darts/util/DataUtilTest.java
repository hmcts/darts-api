package uk.gov.hmcts.darts.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;
import uk.gov.hmcts.darts.task.runner.HasLongId;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@Slf4j
class DataUtilTest {

    @Test
    void toUpperCase() {
        assertThat(DataUtil.toUpperCase("test")).isEqualTo("TEST");
    }


    @Test
    void toUpperCaseNullValue() {
        assertThat(DataUtil.toUpperCase(null)).isNull();
    }


    @Test
    void trimList() {
        assertThat(DataUtil.trim(
            List.of(" test ", "  test2  ", "test3  ", "   test4", "   test 5 with more spaces  ")))
            .containsExactly("test", "test2", "test3", "test4", "test 5 with more spaces");
    }

    @Test
    void trimListNullList() {
        assertThat(DataUtil.trim((List<String>) null)).isNull();
    }

    @Test
    void trimListWithNullListValue() {
        List<String> list = new ArrayList<>(List.of(" test ", "  test2  ", "test3  ", "   test4", "   test 5 with more spaces  "));
        list.add(null);
        assertThat(DataUtil.trim(list))
            .containsExactly("test", "test2", "test3", "test4", "test 5 with more spaces", null);
    }

    @Test
    void trim() {
        assertThat(DataUtil.trim(" test ")).isEqualTo("test");
    }

    @Test
    void trimNull() {
        assertThat(DataUtil.trim((String) null)).isNull();
    }

    @Test
    void preProcessDartsEvent() {
        DartsEvent dartsEvent = new DartsEvent();
        dartsEvent.setCourthouse("courthouse");
        dartsEvent.setCourtroom("courtroom");
        DataUtil.preProcess(dartsEvent);
        assertThat(dartsEvent.getCourthouse()).isEqualTo("COURTHOUSE");
        assertThat(dartsEvent.getCourtroom()).isEqualTo("COURTROOM");
    }

    @Test
    void preProcessDartsEventWithNullValues() {
        DartsEvent dartsEvent = new DartsEvent();
        dartsEvent.setCourthouse(null);
        dartsEvent.setCourtroom(null);
        DataUtil.preProcess(dartsEvent);
        assertThat(dartsEvent.getCourthouse()).isEmpty();
        assertThat(dartsEvent.getCourtroom()).isEmpty();
    }

    @Test
    void preProcessAddCaseRequest() {
        AddCaseRequest addCaseRequest = new AddCaseRequest();
        addCaseRequest.setCourthouse("courthouse");
        addCaseRequest.setDefenders(List.of("  defender1   ", "   defender2"));
        addCaseRequest.setProsecutors(List.of("  prosecutor1   ", "   prosecutor2"));
        addCaseRequest.setDefendants(List.of("  defendant1   ", "   defendant2"));
        DataUtil.preProcess(addCaseRequest);
        assertThat(addCaseRequest.getCourthouse()).isEqualTo("COURTHOUSE");
        assertThat(addCaseRequest.getDefenders()).containsExactly("defender1", "defender2");
        assertThat(addCaseRequest.getProsecutors()).containsExactly("prosecutor1", "prosecutor2");
        assertThat(addCaseRequest.getDefendants()).containsExactly("defendant1", "defendant2");
    }

    @Test
    void preProcessAddCaseRequestWithNullValues() {
        AddCaseRequest addCaseRequest = new AddCaseRequest();
        addCaseRequest.setCourthouse(null);
        DataUtil.preProcess(addCaseRequest);
        assertThat(addCaseRequest.getCourthouse()).isEmpty();
    }

    @Test
    void preProcessAddAudioMetadataRequest() {
        AddAudioMetadataRequest addAudioMetadataRequest = new AddAudioMetadataRequest();
        addAudioMetadataRequest.setCourthouse("courthouse");
        addAudioMetadataRequest.setCourtroom("courtroom");
        DataUtil.preProcess(addAudioMetadataRequest);
        assertThat(addAudioMetadataRequest.getCourthouse()).isEqualTo("COURTHOUSE");
        assertThat(addAudioMetadataRequest.getCourtroom()).isEqualTo("COURTROOM");
    }

    @Test
    void preProcessAddAudioMetadataRequestWithNullValues() {
        AddAudioMetadataRequest addAudioMetadataRequest = new AddAudioMetadataRequest();
        addAudioMetadataRequest.setCourthouse(null);
        addAudioMetadataRequest.setCourtroom(null);
        DataUtil.preProcess(addAudioMetadataRequest);
        assertThat(addAudioMetadataRequest.getCourthouse()).isEmpty();
        assertThat(addAudioMetadataRequest.getCourtroom()).isEmpty();
    }


    private HasIntegerId createHasIntegerId(int id) {
        return new HasIntegerId() {
            @Override
            public Integer getId() {
                return id;
            }
        };
    }

    private HasLongId createHasLongId(long id) {
        return new HasLongId() {
            @Override
            public Long getId() {
                return id;
            }
        };
    }

    @Test
    void compare_withOutHasLongOrHasIntegerId_shouldFail() {
        DartsApiException exception = assertThrows(DartsApiException.class, () -> DataUtil.compare(new Object(), new Object()));
        assertThat(exception.getError()).isEqualTo(CommonApiError.INTERNAL_SERVER_ERROR);
        assertThat(exception.getMessage()).isEqualTo("Internal server error. Cannot compare ids of type java.lang.Object and java.lang.Object");
    }

    @Test
    void compare_withHasIntegerIds_shouldCompareIds() {
        // Given
        HasIntegerId hasIntegerId1 = createHasIntegerId(1);
        HasIntegerId hasIntegerId2 = createHasIntegerId(2);

        // When
        assertThat(DataUtil.compare(hasIntegerId1, hasIntegerId2))
            .isEqualTo(-1);

        assertThat(DataUtil.compare(hasIntegerId2, hasIntegerId2))
            .isEqualTo(0);

        assertThat(DataUtil.compare(hasIntegerId2, hasIntegerId1))
            .isEqualTo(1);
    }

    @Test
    void compare_withHasLongIds_shouldCompareIds() {
        HasLongId hasLongId1 = createHasLongId(1L);
        HasLongId hasLongId2 = createHasLongId(2L);

        // When
        assertThat(DataUtil.compare(hasLongId1, hasLongId2))
            .isEqualTo(-1);


        assertThat(DataUtil.compare(hasLongId2, hasLongId2))
            .isEqualTo(0);

        assertThat(DataUtil.compare(hasLongId2, hasLongId1))
            .isEqualTo(1);
    }


    @Nested
    class OrderByCreatedByAndIdTest {

        @Test
        void orderByCreatedByAndId_shouldReturnEmptyList_whenDataIsNull() {
            assertThat(DataUtil.orderByCreatedByAndId(null, null)).isEmpty();
        }


        @Test
        void orderByCreatedByAndId_shouldReturnEmptyList_whenDataIsEmpty() {
            assertThat(DataUtil.orderByCreatedByAndId(new ArrayList<>(), null)).isEmpty();
        }

        @Test
        void orderByCreatedByAndId_shouldCompareByCreatedDateTime_whenTheyAreDifferent_andNotUseSecondaryComparator() {
            HearingEntity hearing1 = createHearingEntity(1, OffsetDateTime.parse("2023-01-01T12:00:00Z"));
            HearingEntity hearing2 = createHearingEntity(2, OffsetDateTime.parse("2023-01-02T10:00:00Z"));

            List<HearingEntity> data = List.of(hearing1, hearing2);
            List<HearingEntity> sortedData = DataUtil.orderByCreatedByAndId(data, DataUtil::compare);

            assertThat(sortedData).hasSize(2);
            assertThat(sortedData.get(0).getId()).isEqualTo(1);
            assertThat(sortedData.get(1).getId()).isEqualTo(2);
        }

        @Test
        void orderByCreatedByAndId_shouldSecondaryComparator_whenCreatedDateTimeIsTheSame() {
            HearingEntity hearing1 = createHearingEntity(1, OffsetDateTime.parse("2023-01-01T10:00:00Z"));
            HearingEntity hearing2 = createHearingEntity(2, OffsetDateTime.parse("2023-01-02T10:00:00Z"));

            List<HearingEntity> data = List.of(hearing2, hearing1);
            List<HearingEntity> sortedData = DataUtil.orderByCreatedByAndId(data, DataUtil::compare);

            assertThat(sortedData).hasSize(2);
            assertThat(sortedData.get(0).getId()).isEqualTo(1);
            assertThat(sortedData.get(1).getId()).isEqualTo(2);
        }

        @Test
        void orderByCreatedByAndId_shouldProvideIdComparisonAsFallback_whenCreatedDateTimeIsTheSame() {
            HearingEntity hearing1 = createHearingEntity(1, OffsetDateTime.parse("2023-01-01T10:00:00Z"));
            HearingEntity hearing2 = createHearingEntity(2, OffsetDateTime.parse("2023-01-02T10:00:00Z"));

            List<HearingEntity> data = List.of(hearing2, hearing1);
            List<HearingEntity> sortedData = DataUtil.orderByCreatedByAndId(data);

            assertThat(sortedData).hasSize(2);
            assertThat(sortedData.get(0).getId()).isEqualTo(1);
            assertThat(sortedData.get(1).getId()).isEqualTo(2);
        }

        @Test
        void orderHearingsByCreatedByAndId_shouldProvideHearingDateAsFallback_whenCreatedDateTimeIsTheSame() {
            HearingEntity hearing1 = createHearingEntity(1, OffsetDateTime.parse("2023-01-01T10:00:00Z"), LocalDate.parse("2023-02-01"));
            HearingEntity hearing2 = createHearingEntity(2, OffsetDateTime.parse("2023-01-01T10:00:00Z"), LocalDate.parse("2023-01-01"));

            List<HearingEntity> data = List.of(hearing2, hearing1);
            List<HearingEntity> sortedData = DataUtil.orderHearingsByCreatedByAndId(data);

            assertThat(sortedData).hasSize(2);
            assertThat(sortedData.get(0).getId()).isEqualTo(2);
            assertThat(sortedData.get(1).getId()).isEqualTo(1);
        }

        @Test
        void orderHearingsByCreatedByAndId_shouldIdAsFallback_whenCreatedDateTimeIsTheSameAndHearingDateIsTheSame() {
            HearingEntity hearing1 = createHearingEntity(1, OffsetDateTime.parse("2023-01-01T10:00:00Z"), LocalDate.parse("2023-01-01"));
            HearingEntity hearing2 = createHearingEntity(2, OffsetDateTime.parse("2023-01-01T10:00:00Z"), LocalDate.parse("2023-01-01"));

            List<HearingEntity> data = List.of(hearing2, hearing1);
            List<HearingEntity> sortedData = DataUtil.orderHearingsByCreatedByAndId(data);

            assertThat(sortedData).hasSize(2);
            assertThat(sortedData.get(0).getId()).isEqualTo(1);
            assertThat(sortedData.get(1).getId()).isEqualTo(2);
        }

        private HearingEntity createHearingEntity(int id, OffsetDateTime createdDateTime) {
            return createHearingEntity(id, createdDateTime, createdDateTime.toLocalDate());
        }

        private HearingEntity createHearingEntity(int id, OffsetDateTime createdDateTime, LocalDate hearingDate) {
            HearingEntity hearingEntity = new HearingEntity();
            hearingEntity.setId(id);
            hearingEntity.setCreatedDateTime(createdDateTime);
            hearingEntity.setHearingDate(hearingDate);
            return hearingEntity;
        }
    }

    @Test
    void toBooleanNoDefaultValue_shouldDefaultToFalse() {
        assertThat(DataUtil.toBoolean(null)).isFalse();
        assertThat(DataUtil.toBoolean(true)).isTrue();
        assertThat(DataUtil.toBoolean(false)).isFalse();
    }

    @Test
    void toBooleanWithDefaultValue_shouldDefaultToValueSpecified() {
        assertThat(DataUtil.toBoolean(null, false)).isFalse();
        assertThat(DataUtil.toBoolean(null, true)).isTrue();
        assertThat(DataUtil.toBoolean(true)).isTrue();
        assertThat(DataUtil.toBoolean(false)).isFalse();
    }

    @Test
    void isWithinBounds_shouldReturnTrue_WhenValueIsWithinBounds() {
        assertThat(DataUtil.isWithinBounds(5L, 1L, 10L)).isTrue();
    }

    @Test
    void isWithinBounds_shouldReturnFalse_WhenValueIsBelowMin() {
        assertThat(DataUtil.isWithinBounds(0L, 1L, 10L)).isFalse();
    }

    @Test
    void isWithinBounds_shouldReturnFalse_WhenValueIsAboveMax() {
        assertThat(DataUtil.isWithinBounds(11L, 1L, 10L)).isFalse();
    }

    @Test
    void isWithinBounds_shouldReturnTrue_WhenValueIsEqualToMin() {
        assertThat(DataUtil.isWithinBounds(1L, 1L, 10L)).isTrue();
    }

    @Test
    void isWithinBounds_shouldReturnTrue_WhenValueIsEqualToMax() {
        assertThat(DataUtil.isWithinBounds(10L, 1L, 10L)).isTrue();
    }

    @Test
    void isWithinBounds_shouldReturnFalseWhenValueIsNull() {
        assertThat(DataUtil.isWithinBounds(null, 1L, 10L)).isFalse();
    }

}
