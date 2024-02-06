package uk.gov.hmcts.darts.task.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.bankholidays.BankHolidaysService;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.AudioTestData;
import uk.gov.hmcts.darts.testutils.stubs.TransientObjectDirectoryStub;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.EXPIRED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.MARKED_FOR_DELETION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

//Requires transactional as the object is being created manually rather than being autowired.
// We are doing this, so we can mock out different dates to test the service.
@Transactional
@SuppressWarnings("PMD.ExcessiveImports")
class OutboundAudioDeleterProcessorTest extends IntegrationBase {

    public static final LocalDate DATE_27TH_OCTOBER = LocalDate.of(2023, Month.OCTOBER, 27);
    public static final LocalTime LOCAL_TIME = LocalTime.of(13, 1);
    public static final LocalDate HEARING_DATE = LocalDate.of(2023, 6, 10);
    @Autowired
    protected TransientObjectDirectoryStub transientObjectDirectoryStub;


    @MockBean
    private BankHolidaysService bankHolidaysService;

    @MockBean
    private CurrentTimeHelper currentTimeHelper;


    private UserAccountEntity requestor;

    @Autowired
    private OutboundAudioDeleterProcessor outboundAudioDeleterProcessor;

    @Mock
    private SystemUserHelper systemUserHelper;

    @Mock
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUp() {
        requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        //setting clock to 2023-10-27
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC));
        when(systemUserHelper.findSystemUserGuid(anyString())).thenReturn("value");
        UserAccountEntity systemUser = new UserAccountEntity();
        systemUser.setId(0);
        when(userAccountRepository.findSystemUser(anyString())).thenReturn(systemUser);
    }

    @Test
    void whereLastAccessed2DaysAgoAndStatusIsCompleted() {
        HearingEntity hearing = dartsDatabase.createHearing(
              "NEWCASTLE",
              "Int Test Courtroom 2",
              "2",
              HEARING_DATE
        );

        MediaRequestEntity unchangedMediaRequest = AudioTestData.createCurrentMediaRequest(
              hearing,
              requestor,
              OffsetDateTime.parse("2023-06-26T13:00:00Z"),
              OffsetDateTime.parse("2023-06-26T13:45:00Z"),
              AudioRequestType.PLAYBACK,
              COMPLETED
        );
        dartsDatabase.save(
              unchangedMediaRequest);

        //This media request should be deleted as its 3 days old
        MediaRequestEntity currentMediaRequest = AudioTestData.createCurrentMediaRequest(
              hearing,
              requestor,
              OffsetDateTime.parse("2023-06-26T13:00:00Z"),
              OffsetDateTime.parse("2023-06-26T13:45:00Z"),
              AudioRequestType.DOWNLOAD,
              COMPLETED
        );
        dartsDatabase.save(
              currentMediaRequest);

        TransientObjectDirectoryEntity notMarkedForDeletion = createTransientDirectoryAndObjectStatus(
              unchangedMediaRequest,
              OffsetDateTime.of(
                    DATE_27TH_OCTOBER,
                    LOCAL_TIME,
                    ZoneOffset.UTC
              )
        );
        TransientObjectDirectoryEntity markedForDeletion = createTransientDirectoryAndObjectStatus(
              currentMediaRequest,
              OffsetDateTime.of(
                    DATE_27TH_OCTOBER.minusDays(3),
                    LOCAL_TIME,
                    ZoneOffset.UTC
              )
        );

        assertEquals(1, outboundAudioDeleterProcessor.markForDeletion().size());

        assertTransientObjectDirectoryStateChanged(markedForDeletion);
        assertTransientObjectDirectoryStateNotChanged(notMarkedForDeletion);


    }

    /**
     * Last access deletion time is set to 2 days ago with clock being set to 27th friday 13:56. With the media request being last accessed 25th 11:45. The
     * deleter task should not be using hours to calculate last accessed time but days.
     */
    @Test
    void shouldNotTakeIntoAccountTimeWhenCalculatingLastAccessed() {
        HearingEntity hearing = dartsDatabase.createHearing(
              "NEWCASTLE",
              "Int Test Courtroom 2",
              "2",
              HEARING_DATE
        );

        // Last accessed on a wednesday
        MediaRequestEntity currentMediaRequest = AudioTestData.createCurrentMediaRequest(
              hearing,
              requestor,
              OffsetDateTime.parse("2023-06-26T13:00:00Z"),
              OffsetDateTime.parse("2023-06-26T13:45:00Z"),
              AudioRequestType.DOWNLOAD, COMPLETED
        );
        MediaRequestEntity savedMediaRequest = dartsDatabase.save(
              currentMediaRequest);

        TransientObjectDirectoryEntity markedForDeletion = createTransientDirectoryAndObjectStatus(
              currentMediaRequest,
              OffsetDateTime.parse("2023-10-25T11:45:00Z")
        );

        assertEquals(1, outboundAudioDeleterProcessor.markForDeletion().size());
        assertTransientObjectDirectoryStateChanged(markedForDeletion);
        assertEquals(
              EXPIRED,
              dartsDatabase.getMediaRequestRepository().findById(savedMediaRequest.getId()).get().getStatus()
        );
    }


    @Test
    void whereLastAccessedDoesNotIncludesNonBusinessDays() {
        HearingEntity hearing = dartsDatabase.createHearing(
              "NEWCASTLE",
              "Int Test Courtroom 2",
              "2",
              HEARING_DATE
        );

        // Last accessed on a 2023-10-20 friday
        MediaRequestEntity currentMediaRequest = AudioTestData.createCurrentMediaRequest(
              hearing,
              requestor,
              OffsetDateTime.parse("2023-06-26T13:00:00Z"),
              OffsetDateTime.parse("2023-06-26T13:45:00Z"),
              AudioRequestType.DOWNLOAD, COMPLETED
        );
        dartsDatabase.save(
              currentMediaRequest);

        TransientObjectDirectoryEntity notMarkedForDeletion = createTransientDirectoryAndObjectStatus(
              currentMediaRequest,
              OffsetDateTime.parse("2023-10-20T13:45:00Z")
        );

        //setting clock to 2023-10-23 on a monday
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.of(2023, 10, 23, 22, 0, 0, 0, ZoneOffset.UTC));

        assertEquals(0, outboundAudioDeleterProcessor.markForDeletion().size());
        assertTransientObjectDirectoryStateNotChanged(notMarkedForDeletion);
    }


    @Test
    void shouldNotDeleteIfLastAccessWas10DaysAgoWith3BankHoliday() {
        outboundAudioDeleterProcessor.setDeletionDays(10);
        HearingEntity hearing = dartsDatabase.createHearing(
              "NEWCASTLE",
              "Int Test Courtroom 2",
              "2",
              HEARING_DATE
        );

        MediaRequestEntity currentMediaRequest = AudioTestData.createCurrentMediaRequest(
              hearing,
              requestor,
              OffsetDateTime.parse("2023-06-26T13:00:00Z"),
              OffsetDateTime.parse("2023-06-26T13:45:00Z"),
              AudioRequestType.DOWNLOAD,
              COMPLETED
        );
        dartsDatabase.save(
              currentMediaRequest);

        List<LocalDate> holidays = new ArrayList<>();
        holidays.add(LocalDate.of(2023, Month.OCTOBER, 26));
        holidays.add(LocalDate.of(2023, Month.OCTOBER, 25));
        holidays.add(LocalDate.of(2023, Month.OCTOBER, 24));

        TransientObjectDirectoryEntity notMarkedForDeletion = createTransientDirectoryAndObjectStatus(
              currentMediaRequest,
              OffsetDateTime.of(
                    DATE_27TH_OCTOBER,
                    LOCAL_TIME,
                    ZoneOffset.UTC
              ).minusDays(12)
        );

        when(bankHolidaysService.getBankHolidaysLocalDateList()).thenReturn(holidays);

        assertEquals(0, outboundAudioDeleterProcessor.markForDeletion().size());
        assertTransientObjectDirectoryStateNotChanged(notMarkedForDeletion);
    }

    @Test
    void deleteWithTwoTransformedMediaDefaultLastAccessedDays() {
        HearingEntity hearing = dartsDatabase.createHearing(
              "NEWCASTLE",
              "Int Test Courtroom 2",
              "2",
              HEARING_DATE
        );

        //last accessed monday
        MediaRequestEntity currentMediaRequest = AudioTestData.createCurrentMediaRequest(
              hearing,
              requestor,
              OffsetDateTime.parse("2023-06-26T13:00:00Z"),
              OffsetDateTime.parse("2023-06-26T13:45:00Z"),
              AudioRequestType.DOWNLOAD,
              COMPLETED
        );
        dartsDatabase.save(
              currentMediaRequest);

        TransientObjectDirectoryEntity markedForDeletion = createTransientDirectoryAndObjectStatus(
              currentMediaRequest,
              OffsetDateTime.of(
                    DATE_27TH_OCTOBER,
                    LOCAL_TIME,
                    ZoneOffset.UTC
              ).minusDays(5)
        );
        TransientObjectDirectoryEntity notMarkedForDeletion = createTransientDirectoryAndObjectStatus(
              currentMediaRequest,
              OffsetDateTime.of(
                    DATE_27TH_OCTOBER,
                    LOCAL_TIME,
                    ZoneOffset.UTC
              )
        );

        assertEquals(1, outboundAudioDeleterProcessor.markForDeletion().size());
        assertTransientObjectDirectoryStateChanged(markedForDeletion);
        assertTransientObjectDirectoryStateNotChanged(notMarkedForDeletion);

    }

    @Test
    void ifMediaRequestLastAccessedIsOnAWeekendThenTreatItAsItWasAccessedOnAFriday() {
        HearingEntity hearing = dartsDatabase.createHearing(
              "NEWCASTLE",
              "Int Test Courtroom 2",
              "2",
              HEARING_DATE
        );

        //last accessed sunday
        MediaRequestEntity currentMediaRequest = AudioTestData.createCurrentMediaRequest(
              hearing,
              requestor,
              OffsetDateTime.parse("2023-06-26T13:00:00Z"),
              OffsetDateTime.parse("2023-06-26T13:45:00Z"),
              AudioRequestType.DOWNLOAD,
              COMPLETED
        );
        dartsDatabase.save(
              currentMediaRequest);

        TransientObjectDirectoryEntity notMarkedForDeletion = createTransientDirectoryAndObjectStatus(
              currentMediaRequest,
              OffsetDateTime.of(LocalDate.of(2023, Month.OCTOBER, 22), LOCAL_TIME, ZoneOffset.UTC)
        );

        //setting clock to Tuesday, 24 October 2023
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.of(2023, 10, 24, 22, 0, 0, 0, ZoneOffset.UTC));

        outboundAudioDeleterProcessor.markForDeletion();
        assertTransientObjectDirectoryStateNotChanged(notMarkedForDeletion);

        //last accessed saturday
        currentMediaRequest = AudioTestData.createCurrentMediaRequest(
              hearing,
              requestor,
              OffsetDateTime.parse("2023-06-26T13:00:00Z"),
              OffsetDateTime.parse("2023-06-26T13:45:00Z"),

              AudioRequestType.DOWNLOAD,
              COMPLETED
        );
        dartsDatabase.save(
              currentMediaRequest);

        notMarkedForDeletion = createTransientDirectoryAndObjectStatus(
              currentMediaRequest,
              OffsetDateTime.of(LocalDate.of(2023, Month.OCTOBER, 21), LOCAL_TIME, ZoneOffset.UTC)
        );

        outboundAudioDeleterProcessor.markForDeletion();
        assertTransientObjectDirectoryStateNotChanged(notMarkedForDeletion);

    }


    @Test
    void whereLastAccessedIsNullUseCreatedAtAndInProgressStatus() {
        TransientObjectDirectoryEntity markedForDeletion = createMediaRequestsAndTransientObjectDirectoryWithHearingWithLastAccessedTimeIsNull();

        assertEquals(1, outboundAudioDeleterProcessor.markForDeletion().size());
        assertTransientObjectDirectoryStateChanged(markedForDeletion);
        assertEquals(EXPIRED, markedForDeletion.getTransformedMedia().getMediaRequest().getStatus());

    }


    private TransientObjectDirectoryEntity createMediaRequestsAndTransientObjectDirectoryWithHearingWithLastAccessedTimeIsNull() {
        HearingEntity hearing = dartsDatabase.createHearing(
              "NEWCASTLE",
              "Int Test Courtroom 2",
              "2",
              HEARING_DATE
        );

        // Non Matching request
        MediaRequestEntity currentMediaRequest2 = AudioTestData.createCurrentMediaRequest(
              hearing,
              requestor,
              OffsetDateTime.parse("2023-06-26T13:00:00Z"),
              OffsetDateTime.parse("2023-06-26T13:45:00Z"),
              AudioRequestType.DOWNLOAD, PROCESSING
        );

        MediaRequestEntity savedValue = dartsDatabase.save(
              currentMediaRequest2);

        createTransientDirectoryWithTransformedMediaNullLastAccessedDate(savedValue, OffsetDateTime.parse("2023-06-26T13:45:00Z"));

        MediaRequestEntity currentMediaRequest3 = AudioTestData.createCurrentMediaRequest(
              hearing,
              requestor,
              OffsetDateTime.parse("2023-06-26T13:00:00Z"),
              OffsetDateTime.parse("2023-06-26T13:45:00Z"), AudioRequestType.DOWNLOAD, OPEN
        );
        savedValue = dartsDatabase.save(
              currentMediaRequest3);

        createTransientDirectoryWithTransformedMediaNullLastAccessedDate(savedValue, currentTimeHelper.currentOffsetDateTime());

        // should match this
        MediaRequestEntity mediaRequestThatShouldMatch = AudioTestData.createCurrentMediaRequest(
              hearing,
              requestor,
              OffsetDateTime.parse("2023-06-26T13:00:00Z"),
              OffsetDateTime.parse("2023-06-26T13:45:00Z"),
              AudioRequestType.DOWNLOAD, OPEN
        );

        savedValue = dartsDatabase.save(
              mediaRequestThatShouldMatch);

        return createTransientDirectoryWithTransformedMediaNullLastAccessedDate(
              savedValue,
              OffsetDateTime.parse(
                    "2023-06-24T13:45:00Z")
        );
    }


    private void assertTransientObjectDirectoryStateChanged(TransientObjectDirectoryEntity transientObjectDirectoryEntity) {

        assertEquals(
              MARKED_FOR_DELETION.getId(),
              transientObjectDirectoryEntity.getStatus().getId()
        );

        assertEquals(
              "system_housekeeping",
              transientObjectDirectoryEntity.getLastModifiedBy().getUserName()
        );

        assertNotNull(transientObjectDirectoryEntity.getTransformedMedia().getExpiryTime());
        assertEquals(1, transientObjectDirectoryEntity.getTransformedMedia().getLastModifiedBy().getId());
    }

    private void assertTransientObjectDirectoryStateNotChanged(TransientObjectDirectoryEntity transientObjectDirectoryEntity) {
        assertNotEquals(
              MARKED_FOR_DELETION.getId(),
              transientObjectDirectoryEntity.getStatus().getId()
        );

        assertNotEquals(
              "system_housekeeping",
              transientObjectDirectoryEntity.getLastModifiedBy().getUserName()
        );

        assertEquals(
              STORED.getId(),
              transientObjectDirectoryEntity.getStatus().getId()
        );

        assertNull(transientObjectDirectoryEntity.getTransformedMedia().getExpiryTime());
    }


    private TransientObjectDirectoryEntity createTransientDirectoryAndObjectStatus(MediaRequestEntity currentMediaRequest, OffsetDateTime lastAccessedDate) {
        var blobId = UUID.randomUUID();

        var objectDirectoryStatusEntity = dartsDatabase.getObjectRecordStatusEntity(STORED);
        return dartsDatabase.getTransientObjectDirectoryRepository()
              .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                    currentMediaRequest,
                    objectDirectoryStatusEntity,
                    blobId, lastAccessedDate
              ));
    }

    private TransientObjectDirectoryEntity createTransientDirectoryWithTransformedMediaNullLastAccessedDate(MediaRequestEntity mediaRequestEntity,
                                                                                                            OffsetDateTime createdAt) {
        var objectDirectoryStatusEntity = dartsDatabase.getObjectRecordStatusEntity(STORED);
        TransformedMediaEntity transformedMediaEntity = dartsDatabase.getTransformedMediaStub().createTransformedMediaEntity(
              mediaRequestEntity,
              null,
              null,
              null
        );
        TransformedMediaEntity savedTM = dartsDatabase.save(transformedMediaEntity);
        savedTM.setCreatedDateTime(createdAt);

        return dartsDatabase.getTransientObjectDirectoryRepository().saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
              savedTM,
              objectDirectoryStatusEntity,
              UUID.randomUUID()
        ));


    }
}
