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
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
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

        createTransientDirectoryAndObjectStatus(unchangedMediaRequest, OffsetDateTime.of(DATE_27TH_OCTOBER, LOCAL_TIME, ZoneOffset.UTC));
        createTransientDirectoryAndObjectStatus(currentMediaRequest, OffsetDateTime.of(DATE_27TH_OCTOBER.minusDays(3), LOCAL_TIME, ZoneOffset.UTC));

        outboundAudioDeleterProcessor.markForDeletion();

        assertEntityStateChanged(List.of(currentMediaRequest));
        assertEntityStateNotChanged(unchangedMediaRequest);


    }

    /**
     * Last access deletion time is set to 2 days ago with clock being set to 27th friday 13:56. With the media request being last accessed 25th 11:45.
     * The deleter task should not be using hours to calculate last accessed time but days.
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
        dartsDatabase.save(
            currentMediaRequest);

        createTransientDirectoryAndObjectStatus(currentMediaRequest, OffsetDateTime.parse("2023-10-25T11:45:00Z"));

        outboundAudioDeleterProcessor.markForDeletion();


        assertEntityStateChanged(List.of(currentMediaRequest));
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

        createTransientDirectoryAndObjectStatus(currentMediaRequest, OffsetDateTime.parse("2023-10-20T13:45:00Z"));

        //setting clock to 2023-10-23 on a monday
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.of(2023, 10, 23, 22, 0, 0, 0, ZoneOffset.UTC));
        outboundAudioDeleterProcessor.markForDeletion();

        assertEntityStateNotChanged(currentMediaRequest);
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


        createTransientDirectoryAndObjectStatus(currentMediaRequest, OffsetDateTime.of(DATE_27TH_OCTOBER, LOCAL_TIME, ZoneOffset.UTC).minusDays(12));

        List<LocalDate> holidays = new ArrayList<>();
        holidays.add(LocalDate.of(2023, Month.OCTOBER, 26));
        holidays.add(LocalDate.of(2023, Month.OCTOBER, 25));
        holidays.add(LocalDate.of(2023, Month.OCTOBER, 24));

        when(bankHolidaysService.getBankHolidaysLocalDateList()).thenReturn(holidays);

        outboundAudioDeleterProcessor.markForDeletion();

        assertEntityStateNotChanged(currentMediaRequest);
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

        createTransientDirectoryAndObjectStatus(currentMediaRequest, OffsetDateTime.of(DATE_27TH_OCTOBER, LOCAL_TIME, ZoneOffset.UTC).minusDays(5));

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
            AudioRequestType.DOWNLOAD,
            COMPLETED
        );
        dartsDatabase.save(
            currentMediaRequest);

        createTransientDirectoryAndObjectStatus(
            currentMediaRequest,
            OffsetDateTime.of(LocalDate.of(2023, Month.OCTOBER, 22), LOCAL_TIME, ZoneOffset.UTC)
        );

        //setting clock to Tuesday, 24 October 2023
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.of(2023, 10, 24, 22, 0, 0, 0, ZoneOffset.UTC));


        outboundAudioDeleterProcessor.markForDeletion();
        assertEntityStateNotChanged(currentMediaRequest);


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

        createTransientDirectoryAndObjectStatus(
            currentMediaRequest,
            OffsetDateTime.of(LocalDate.of(2023, Month.OCTOBER, 21), LOCAL_TIME, ZoneOffset.UTC)
        );

        outboundAudioDeleterProcessor.markForDeletion();
        assertEntityStateNotChanged(currentMediaRequest);

    }


    @Test
    void whereLastAccessedIsNullUseCreatedAtAndInProgressStatus() {
        MediaRequestEntity matchingMediaRequest = createMediaRequestsWithHearingWithLastAccessedTimeIsNull();

        List<MediaRequestEntity> markedForDeletion = outboundAudioDeleterProcessor.markForDeletion();

        MediaRequestEntity expiredMediaRequest = dartsDatabase.getMediaRequestRepository().findById(markedForDeletion.get(
            0).getId()).get();

        assertNotEquals(PROCESSING, expiredMediaRequest.getStatus());
        assertTransientObjectDirectoryStateChanged(matchingMediaRequest);

    }


    private MediaRequestEntity createMediaRequestsWithHearingWithLastAccessedTimeIsNull() {
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
            AudioRequestType.DOWNLOAD, OPEN
        );

        MediaRequestEntity savedValue = dartsDatabase.save(
            mediaRequestThatShouldMatch);

        createTransientDirectoryWithTransformedMediaNullLastAccessedDate(savedValue, OffsetDateTime.parse("2023-06-24T13:45:00Z"));


        // Non Matching request
        MediaRequestEntity currentMediaRequest2 = AudioTestData.createCurrentMediaRequest(
            hearing,
            requestor,
            OffsetDateTime.parse("2023-06-26T13:00:00Z"),
            OffsetDateTime.parse("2023-06-26T13:45:00Z"),
            AudioRequestType.DOWNLOAD, PROCESSING
        );

        savedValue = dartsDatabase.save(
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

        createTransientDirectoryWithTransformedMediaNullLastAccessedDate(savedValue, OffsetDateTime.now());

        return mediaRequestThatShouldMatch;
    }


    private void assertEntityStateChanged(List<MediaRequestEntity> currentMediaRequest) {
        assert !currentMediaRequest.isEmpty() : "Testing for entities changed in relation to media requests changed but no media requests provided";

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

    private void createTransientDirectoryAndObjectStatus(MediaRequestEntity currentMediaRequest, OffsetDateTime lastAccessedDate) {
        var blobId = UUID.randomUUID();

        var objectDirectoryStatusEntity = dartsDatabase.getObjectRecordStatusEntity(STORED);
        dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                currentMediaRequest,
                objectDirectoryStatusEntity,
                blobId, lastAccessedDate
            ));
    }

    private void createTransientDirectoryWithTransformedMediaNullLastAccessedDate(MediaRequestEntity mediaRequestEntity, OffsetDateTime createdAt) {
        var objectDirectoryStatusEntity = dartsDatabase.getObjectRecordStatusEntity(STORED);
        TransformedMediaEntity transformedMediaEntity = dartsDatabase.getTransformedMediaStub().createTransformedMediaEntity(
            mediaRequestEntity,
            null,
            null,
            null
        );
        TransformedMediaEntity savedTM = dartsDatabase.save(transformedMediaEntity);
        savedTM.setCreatedDateTime(createdAt);

        dartsDatabase.getTransientObjectDirectoryRepository().saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
            savedTM,
            objectDirectoryStatusEntity,
            UUID.randomUUID()
        ));


    }
}
