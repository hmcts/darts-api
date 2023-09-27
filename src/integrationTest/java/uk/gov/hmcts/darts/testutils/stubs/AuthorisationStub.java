package uk.gov.hmcts.darts.testutils.stubs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionTypeRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum.SPECIFIED_TIMES;

@Component
@RequiredArgsConstructor
@Getter
public class AuthorisationStub {

    private final DartsDatabaseStub dartsDatabaseStub;
    private final TranscriptionTypeRepository transcriptionTypeRepository;
    private final TranscriptionStatusRepository transcriptionStatusRepository;

    private UserAccountEntity systemUser;
    private UserAccountEntity testUser;
    private SecurityGroupEntity bristolStaff;
    private SecurityGroupEntity bristolAppr;
    private CourtroomEntity courtroomEntity;
    private CourthouseEntity courthouseEntity;
    private CourtCaseEntity courtCaseEntity;
    private HearingEntity hearingEntity;
    private MediaRequestEntity mediaRequestEntity;
    private MediaRequestEntity mediaRequestEntitySystemUser;
    private MediaEntity mediaEntity;
    private TranscriptionEntity transcriptionEntity;

    public void givenTestSchema() {
        courtroomEntity = dartsDatabaseStub.givenTheDatabaseContainsCourthouseWithRoom(
            "Bristol",
            "Court 1"
        );

        courthouseEntity = dartsDatabaseStub.getCourthouseRepository()
            .findById(courtroomEntity.getCourthouse().getId()).orElseThrow();

        systemUser = dartsDatabaseStub.getUserAccountStub().getSystemUserAccountEntity();
        testUser = dartsDatabaseStub.getUserAccountStub().getIntegrationTestUserAccountEntity();

        SecurityGroupRepository securityGroupRepository = dartsDatabaseStub.getSecurityGroupRepository();
        bristolStaff = securityGroupRepository.findById(20).orElseThrow();
        bristolStaff.setCourthouseEntities(Set.of(courthouseEntity));
        bristolAppr = securityGroupRepository.findById(35).orElseThrow();
        bristolAppr.setCourthouseEntities(Set.of(courthouseEntity));
        testUser.getSecurityGroupEntities().addAll(List.of(bristolStaff, bristolAppr));
        dartsDatabaseStub.getUserAccountRepository().save(testUser);

        courtCaseEntity = new CourtCaseEntity();
        var yesterday = OffsetDateTime.now(UTC).minusDays(1).withHour(9).withMinute(0).withSecond(0);
        courtCaseEntity.setCaseNumber(String.format("T%s", yesterday.format(BASIC_ISO_DATE)));
        courtCaseEntity.setCourthouse(courthouseEntity);
        dartsDatabaseStub.save(courtCaseEntity);

        hearingEntity = new HearingEntity();
        hearingEntity.setCourtCase(courtCaseEntity);
        hearingEntity.setCourtroom(courtroomEntity);
        hearingEntity.setHearingDate(LocalDate.now());
        dartsDatabaseStub.save(hearingEntity);

        mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearing(hearingEntity);
        mediaRequestEntity.setRequestor(testUser);
        mediaRequestEntity.setStatus(OPEN);
        mediaRequestEntity.setRequestType(DOWNLOAD);
        mediaRequestEntity.setStartTime(yesterday);
        mediaRequestEntity.setEndTime(yesterday.plusHours(1));
        dartsDatabaseStub.save(mediaRequestEntity);

        mediaRequestEntitySystemUser = new MediaRequestEntity();
        mediaRequestEntitySystemUser.setHearing(hearingEntity);
        mediaRequestEntitySystemUser.setRequestor(systemUser);
        mediaRequestEntitySystemUser.setStatus(OPEN);
        mediaRequestEntitySystemUser.setRequestType(DOWNLOAD);
        mediaRequestEntitySystemUser.setStartTime(yesterday);
        mediaRequestEntitySystemUser.setEndTime(yesterday.plusHours(1));
        dartsDatabaseStub.save(mediaRequestEntitySystemUser);

        mediaEntity = new MediaEntity();
        mediaEntity.setCourtroom(courtroomEntity);
        dartsDatabaseStub.save(mediaEntity);

        hearingEntity.addMedia(mediaEntity);
        dartsDatabaseStub.save(hearingEntity);

        transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setCourtCase(courtCaseEntity);
        transcriptionEntity.setCourtroom(courtroomEntity);
        transcriptionEntity.setHearing(hearingEntity);
        transcriptionEntity.setTranscriptionType(transcriptionTypeRepository.getReferenceById(SPECIFIED_TIMES.getId()));
        transcriptionEntity.setTranscriptionStatus(transcriptionStatusRepository.getReferenceById(REQUESTED.getId()));
        transcriptionEntity.setCreatedBy(testUser);
        transcriptionEntity.setLastModifiedBy(testUser);
        dartsDatabaseStub.save(transcriptionEntity);
    }

}
