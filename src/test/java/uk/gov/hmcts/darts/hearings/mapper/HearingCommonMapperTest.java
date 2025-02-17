package uk.gov.hmcts.darts.hearings.mapper;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.hearings.model.Audit;
import uk.gov.hmcts.darts.hearings.model.Courthouse;
import uk.gov.hmcts.darts.hearings.model.Courtroom;
import uk.gov.hmcts.darts.hearings.model.Location;
import uk.gov.hmcts.darts.hearings.model.NameAndIdResponse;
import uk.gov.hmcts.darts.hearings.model.User;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;
import uk.gov.hmcts.darts.task.runner.IsNamedEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HearingCommonMapperTest {
    MockedStatic<HearingCommonMapper> hearingCommonMapperMockedStatic;

    @BeforeEach
    void beforeEach() {
        hearingCommonMapperMockedStatic = Mockito.mockStatic(HearingCommonMapper.class, Mockito.CALLS_REAL_METHODS);
    }

    @AfterEach
    void afterEach() {
        hearingCommonMapperMockedStatic.close();
    }

    @Test
    void asList_hasNullInput_shouldReturnEmptyList() {
        assertThat(HearingCommonMapper.asList(null, object -> object))
            .isNotNull()
            .isEmpty();
    }

    @Test
    void asList_hasNullData_shouldReturnListExcludingNullValues() {
        List<String> list = new ArrayList<>();
        list.add("value");
        list.add(null);
        list.add("value2");

        List<String> result = HearingCommonMapper.asList(list, object -> {
            assertThat(object).isNotNull();
            return object;
        });
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2).containsExactly("value", "value2");
    }

    @Test
    void asList_hasData_shouldReturnListWithMappedValues() {
        List<String> list = new ArrayList<>();
        list.add("value");
        list.add("value2");
        list.add("value3");

        List<String> result = HearingCommonMapper.asList(list, object -> {
            assertThat(object).isNotNull();
            return object;
        });
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3).containsExactly("value", "value2", "value3");


    }

    @Test
    void mapToNameAndIdResponse_hasNullInput_shouldReturnNull() {
        assertThat(HearingCommonMapper.mapToNameAndIdResponse(null)).isNull();
    }

    @Test
    void mapToNameAndIdResponse_hasData_shouldReturnMappedValues() {

        @Getter
        @Setter
        class SupportClass implements IsNamedEntity, HasIntegerId {
            Integer id;
            String name;
        }
        SupportClass supportClass = new SupportClass();
        supportClass.setId(123);
        supportClass.setName("some name");
        NameAndIdResponse nameAndIdResponse = HearingCommonMapper.mapToNameAndIdResponse(supportClass);
        assertThat(nameAndIdResponse).isNotNull();
        assertThat(nameAndIdResponse.getId()).isEqualTo(123);
        assertThat(nameAndIdResponse.getName()).isEqualTo("some name");
    }

    @Test
    void mapToAudit_hasNullInput_shouldReturnNull() {
        assertThat(HearingCommonMapper.mapToAudit(null)).isNull();
    }

    @Test
    void mapToAudit_hasData_shouldReturnMappedValues() {
        UserAccountEntity createdByUserAccount = mock(UserAccountEntity.class);
        User createdByUser = mock(User.class);
        hearingCommonMapperMockedStatic.when(() -> HearingCommonMapper.mapToUser(createdByUserAccount)).thenReturn(createdByUser);

        UserAccountEntity lastModifiedByUserAccount = mock(UserAccountEntity.class);
        User updatedByUser = mock(User.class);
        hearingCommonMapperMockedStatic.when(() -> HearingCommonMapper.mapToUser(lastModifiedByUserAccount)).thenReturn(updatedByUser);

        CreatedModifiedBaseEntity createdModifiedBaseEntity = new CreatedModifiedBaseEntity();

        OffsetDateTime cratedTime = OffsetDateTime.now().minusDays(2);
        createdModifiedBaseEntity.setCreatedBy(createdByUserAccount);
        createdModifiedBaseEntity.setCreatedDateTime(cratedTime);

        OffsetDateTime updatedTime = OffsetDateTime.now();
        createdModifiedBaseEntity.setLastModifiedBy(lastModifiedByUserAccount);
        createdModifiedBaseEntity.setLastModifiedDateTime(updatedTime);


        Audit audit = HearingCommonMapper.mapToAudit(createdModifiedBaseEntity);
        assertThat(audit).isNotNull();
        assertThat(audit.getCreatedBy()).isEqualTo(createdByUser);
        assertThat(audit.getCreatedAt()).isEqualTo(cratedTime);

        assertThat(audit.getUpdatedBy()).isEqualTo(updatedByUser);
        assertThat(audit.getUpdatedAt()).isEqualTo(updatedTime);

    }

    @Test
    void mapToUser_hasNullInput_shouldReturnNull() {
        assertThat(HearingCommonMapper.mapToUser(null)).isNull();
    }

    @Test
    void mapToUser_hasData_shouldReturnMappedValues() {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(123);
        userAccount.setUserFullName("some full name");

        User user = HearingCommonMapper.mapToUser(userAccount);
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(123);
        assertThat(user.getName()).isEqualTo("some full name");
    }

    @Test
    void toTimeStringOffsetDateTime_hasNullInput_shouldReturnNull() {
        assertThat(HearingCommonMapper.toTimeString((OffsetDateTime) null)).isNull();
    }

    @Test
    void toTimeStringOffsetDateTime_hasData_shouldReturnMappedValues() {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(LocalDate.now(), LocalTime.of(12, 23, 12),
                                                          OffsetDateTime.now().getOffset());
        assertThat(HearingCommonMapper.toTimeString(offsetDateTime)).isEqualTo("12:23:12");
    }

    @Test
    void toTimeStringOffsetDateTime_hasDataWith0Values_shouldReturnMappedValues() {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(LocalDate.now(), LocalTime.of(0, 0, 0),
                                                          OffsetDateTime.now().getOffset());
        assertThat(HearingCommonMapper.toTimeString(offsetDateTime)).isEqualTo("00:00:00");
    }

    @Test
    void toTimeStringLocalTime_hasNullInput_shouldReturnNull() {
        assertThat(HearingCommonMapper.toTimeString((LocalTime) null)).isNull();
    }

    @Test
    void toTimeStringLocalTime_hasData_shouldReturnMappedValues() {
        LocalTime localTime = LocalTime.of(12, 23, 12);
        assertThat(HearingCommonMapper.toTimeString(localTime)).isEqualTo("12:23:12");
    }

    @Test
    void toTimeStringLocalTime_hasDataWith0Values_shouldReturnMappedValues() {
        LocalTime localTime = LocalTime.of(0, 0, 0);
        assertThat(HearingCommonMapper.toTimeString(localTime)).isEqualTo("00:00:00");
    }

    @Test
    void mapToLocation_hasNullInput_shouldReturnNull() {
        assertThat(HearingCommonMapper.mapToLocation(null)).isNull();
    }

    @Test
    void mapToLocation_hasData_shouldReturnMappedValues() {
        CourtroomEntity courtroomEntity = new CourtroomEntity();
        CourthouseEntity courthouseEntity = mock(CourthouseEntity.class);
        courtroomEntity.setCourthouse(courthouseEntity);

        Courtroom courtroom = mock(Courtroom.class);
        Courthouse courthouse = mock(Courthouse.class);

        hearingCommonMapperMockedStatic.when(() -> HearingCommonMapper.mapToCourtroom(courtroomEntity)).thenReturn(courtroom);
        hearingCommonMapperMockedStatic.when(() -> HearingCommonMapper.mapToCourthouse(courthouseEntity)).thenReturn(courthouse);

        Location location = HearingCommonMapper.mapToLocation(courtroomEntity);

        assertThat(location).isNotNull();
        assertThat(location.getCourtroom()).isEqualTo(courtroom);
        assertThat(location.getCourthouse()).isEqualTo(courthouse);
    }

    @Test
    void mapToCourtroom_hasNullInput_shouldReturnNull() {
        assertThat(HearingCommonMapper.mapToCourtroom(null)).isNull();
    }

    @Test
    void mapToCourtroom_hasData_shouldReturnMappedValues() {
        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setId(123);
        courtroomEntity.setName("name 1");

        Courtroom courtroom = HearingCommonMapper.mapToCourtroom(courtroomEntity);
        assertThat(courtroom).isNotNull();
        assertThat(courtroom.getId()).isEqualTo(123);
        assertThat(courtroom.getName()).isEqualTo("NAME 1");
    }

    @Test
    void mapToCourthouse_hasNullInput_shouldReturnNull() {
        assertThat(HearingCommonMapper.mapToCourthouse(null)).isNull();
    }

    @Test
    void mapToCourthouse_hasData_shouldReturnMappedValues() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setId(123);
        courthouseEntity.setCode(31);
        courthouseEntity.setCourthouseName("name 1");
        courthouseEntity.setDisplayName("display name 1");

        Courthouse courthouse = HearingCommonMapper.mapToCourthouse(courthouseEntity);

        assertThat(courthouse).isNotNull();
        assertThat(courthouse.getId()).isEqualTo(123);
        assertThat(courthouse.getCode()).isEqualTo(31);
        assertThat(courthouse.getName()).isEqualTo("NAME 1");
        assertThat(courthouse.getDisplayName()).isEqualTo("display name 1");

    }
}
