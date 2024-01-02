package uk.gov.hmcts.darts.task.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessor;
import uk.gov.hmcts.darts.audio.service.impl.LastAccessedDeletionDayCalculator;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.MARKED_FOR_DELETION;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;

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

    @Autowired
    LastAccessedDeletionDayCalculator lastAccessedDeletionDayCalculator;

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
        when(currentTimeHelper.currentLocalDate()).thenReturn(LocalDate.of(2023, 10, 27));
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

        dartsDatabase.save(
            AudioTestData.createCurrentMediaRequest(
                hearing,
                requestor,
                OffsetDateTime.parse("2023-06-26T13:00:00Z"),
                OffsetDateTime.parse("2023-06-26T13:45:00Z"),
                OffsetDateTime.of(DATE_27TH_OCTOBER, LOCAL_TIME, ZoneOffset.UTC), AudioRequestType.DOWNLOAD,
                COMPLETED
            ));

        //This media request should be deleted as its 3 days old
        MediaRequestEntity currentMediaRequest = AudioTestData.createCurrentMediaRequest(
            hearing,
            requestor,
            OffsetDateTime.parse("2023-06-26T13:00:00Z"),
            OffsetDateTime.parse("2023-06-26T13:45:00Z"),
            OffsetDateTime.of(DATE_27TH_OCTOBER.minusDays(3), LOCAL_TIME, ZoneOffset.UTC),
            AudioRequestType.DOWNLOAD,
            COMPLETED
        );
        dartsDatabase.save(
            currentMediaRequest);


        createTransientDirectoryAndObjectStatus(currentMediaRequest);
        assertEntityStateChanged(outboundAudioDeleterProcessor.markForDeletion());


    }

    /**
     * Last access deletion time is set to 2 days ago with clock being set to 27th friday 13:56. With the media request being last accessed 25th 11:45.
     * The deleter task should be using hours to calculate last accessed time but days.
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
            OffsetDateTime.parse("2023-10-25T11:45:00Z"), AudioRequestType.DOWNLOAD, COMPLETED
        );
        dartsDatabase.save(
            currentMediaRequest);

        createTransientDirectoryAndObjectStatus(currentMediaRequest);

        outboundAudioDeleterProcessor.markForDeletion();


        assertEntityStateNotChanged(currentMediaRequest);
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
            OffsetDateTime.parse("2023-10-20T13:45:00Z"), AudioRequestType.DOWNLOAD, COMPLETED
        );
        dartsDatabase.save(
            currentMediaRequest);

        createTransientDirectoryAndObjectStatus(currentMediaRequest);

        //setting clock to 2023-10-23 on a monday
        when(currentTimeHelper.currentLocalDate()).thenReturn(LocalDate.of(2023, 10, 23));
        outboundAudioDeleterProcessor.markForDeletion();

        assertEntityStateNotChanged(currentMediaRequest);
    }


    @Test
    void shouldNotDeleteIfLastAccessWas10DaysAgoWith3BankHoliday() {
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
            OffsetDateTime.of(DATE_27TH_OCTOBER, LOCAL_TIME, ZoneOffset.UTC).minusDays(12),
            AudioRequestType.DOWNLOAD,
            COMPLETED
        );
        dartsDatabase.save(
            currentMediaRequest);


        createTransientDirectoryAndObjectStatus(currentMediaRequest);
        List<LocalDate> holidays = new ArrayList<>();
        holidays.add(LocalDate.of(2023, Month.OCTOBER, 26));
        holidays.add(LocalDate.of(2023, Month.OCTOBER, 25));
        holidays.add(LocalDate.of(2023, Month.OCTOBER, 24));

        when(bankHolidaysService.getBankHolidaysLocalDateList()).thenReturn(holidays);

        outboundAudioDeleterProcessor.markForDeletion();

        assertEntityStateNotChanged(currentMediaRequest);
    }

    @Test
    void deleteWithChangedDefaultLastAccessedDays() {
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
            OffsetDateTime.of(DATE_27TH_OCTOBER, LOCAL_TIME, ZoneOffset.UTC).minusDays(5), AudioRequestType.DOWNLOAD,
            COMPLETED
        );
        dartsDatabase.save(
            currentMediaRequest);

        createTransientDirectoryAndObjectStatus(currentMediaRequest);

        assertEntityStateChanged(outboundAudioDeleterProcessor.markForDeletion());
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
            OffsetDateTime.of(LocalDate.of(2023, Month.OCTOBER, 22), LOCAL_TIME, ZoneOffset.UTC),
            AudioRequestType.DOWNLOAD,
            COMPLETED
        );
        dartsDatabase.save(
            currentMediaRequest);

        createTransientDirectoryAndObjectStatus(currentMediaRequest);

        //setting clock to Tuesday, 24 October 2023
        when(currentTimeHelper.currentLocalDate()).thenReturn(LocalDate.of(2023, 10, 24));

        assertEntityStateChanged(outboundAudioDeleterProcessor.markForDeletion());


        //last accessed saturday
        currentMediaRequest = AudioTestData.createCurrentMediaRequest(
            hearing,
            requestor,
            OffsetDateTime.parse("2023-06-26T13:00:00Z"),
            OffsetDateTime.parse("2023-06-26T13:45:00Z"),
            OffsetDateTime.of(LocalDate.of(2023, Month.OCTOBER, 21), LOCAL_TIME, ZoneOffset.UTC),
            AudioRequestType.DOWNLOAD,
            COMPLETED
        );
        dartsDatabase.save(
            currentMediaRequest);

        createTransientDirectoryAndObjectStatus(currentMediaRequest);

        assertEntityStateChanged(outboundAudioDeleterProcessor.markForDeletion());

    }


    @Test
    void whereLastAccessedIsNullUseCreatedAndInProgressStatus() {
        MediaRequestEntity matchingMediaRequest = createMediaRequestsWithHearingWithLastAccessedTimeIsNull();

        List<MediaRequestEntity> markedForDeletion = outboundAudioDeleterProcessor.markForDeletion();

        MediaRequestEntity expiredMediaRequest = dartsDatabase.getMediaRequestRepository().findById(markedForDeletion.get(
            0).getId()).get();

        assertNotEquals(PROCESSING, expiredMediaRequest.getStatus());
        assertTransientObjectDirectoryStateChanged(matchingMediaRequest);

    }


    private MediaRequestEntity createMediaRequestsWithHearingWithLastAccessedTimeIsNull() {
        List<MediaRequestEntity> createdMediaRequests = new ArrayList<>();
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaRequestEntity mediaRequestThatShouldMatch = AudioTestData.createCurrentMediaRequest(
            hearing,
            requestor,
            OffsetDateTime.parse("2023-06-26T13:00:00Z"),
            OffsetDateTime.parse("2023-06-26T13:45:00Z"),
            null, AudioRequestType.DOWNLOAD, OPEN
        );

        MediaRequestEntity savedValue = dartsDatabase.save(
            mediaRequestThatShouldMatch);

        savedValue.setCreatedDateTime(OffsetDateTime.parse("2023-06-24T13:45:00Z"));

        createdMediaRequests.add(dartsDatabase.save(savedValue));

        // Non Matching request
        MediaRequestEntity currentMediaRequest2 = AudioTestData.createCurrentMediaRequest(
            hearing,
            requestor,
            OffsetDateTime.parse("2023-06-26T13:00:00Z"),
            OffsetDateTime.parse("2023-06-26T13:45:00Z"),
            null, AudioRequestType.DOWNLOAD, PROCESSING
        );

        savedValue = dartsDatabase.save(
            currentMediaRequest2);

        savedValue.setCreatedDateTime(OffsetDateTime.parse("2023-06-26T13:45:00Z"));

        createdMediaRequests.add(dartsDatabase.save(savedValue));


        MediaRequestEntity currentMediaRequest3 = AudioTestData.createCurrentMediaRequest(
            hearing,
            requestor,
            OffsetDateTime.parse("2023-06-26T13:00:00Z"),
            OffsetDateTime.parse("2023-06-26T13:45:00Z"),
            null, AudioRequestType.DOWNLOAD, OPEN
        );
        savedValue = dartsDatabase.save(
            currentMediaRequest3);

        savedValue.setCreatedDateTime(OffsetDateTime.now());

        createdMediaRequests.add(dartsDatabase.save(savedValue));

        for (MediaRequestEntity mediaRequest : createdMediaRequests) {
            createTransientDirectoryAndObjectStatus(mediaRequest);
        }
        return mediaRequestThatShouldMatch;
    }


    private void assertEntityStateChanged(List<MediaRequestEntity> currentMediaRequest) {
        for (MediaRequestEntity mediaRequestEntity : currentMediaRequest) {
            assertEquals(MediaRequestStatus.EXPIRED, mediaRequestEntity.getStatus());
            assertTransientObjectDirectoryStateChanged(mediaRequestEntity);

        }
    }

    private void assertTransientObjectDirectoryStateChanged(MediaRequestEntity expiredMediaRequest) {
        var transientObjectDirectoryEntity
            = dartsDatabase.getTransientObjectDirectoryRepository().findByMediaRequestId(
            expiredMediaRequest.getId()).get();

        assertEquals(
            MARKED_FOR_DELETION.getId(),
            transientObjectDirectoryEntity.getStatus().getId()
        );


        assertEquals(
            "system_housekeeping",
            transientObjectDirectoryEntity.getLastModifiedBy().getUserName()
        );
    }

    private void assertEntityStateNotChanged(MediaRequestEntity currentMediaRequest) {
        MediaRequestEntity savedMediaRequest = dartsDatabase.getMediaRequestRepository().findById(currentMediaRequest.getId()).get();
        assertEquals(COMPLETED, savedMediaRequest.getStatus());

        var transientObjectDirectoryEntity
            = dartsDatabase.getTransientObjectDirectoryRepository().findByMediaRequestId(
            savedMediaRequest.getId()).get();

        assertEquals(
            STORED.getId(),
            transientObjectDirectoryEntity.getStatus().getId()
        );


    }

    private void createTransientDirectoryAndObjectStatus(MediaRequestEntity currentMediaRequest) {
        var blobId = UUID.randomUUID();

        var objectDirectoryStatusEntity = dartsDatabase.getObjectDirectoryStatusEntity(STORED);
        dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                currentMediaRequest,
                objectDirectoryStatusEntity,
                blobId
            ));
    }

}
