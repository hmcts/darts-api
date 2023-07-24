package uk.gov.hmcts.darts.common.util;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingMediaEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccount;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
deprecated, in favour of IntegrationBase / DartsDatabaseStub
 */
@UtilityClass
@Deprecated()
@SuppressWarnings({"PMD.TooManyMethods", "HideUtilityClassConstructor"})
public class CommonTestDataUtil {

    public static EventEntity createEvent(String eventName, String eventText, HearingEntity hearingEntity) {
        EventEntity event = new EventEntity();
        event.setHearingEntities(List.of(hearingEntity));
        event.setCourtroom(hearingEntity.getCourtroom());
        event.setEventName(eventName);
        event.setEventText(eventText);
        event.setId(1);
        event.setTimestamp(createOffsetDateTime("2023-07-01T10:00:00"));

        return event;

    }

    public static OffsetDateTime createOffsetDateTime(String timestamp) {

        ZoneId zoneId = ZoneId.of("UTC");   // Or another geographic: Europe/Paris

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime start = LocalDateTime.parse(timestamp, formatter);

        ZoneOffset offset = zoneId.getRules().getOffset(start);

        return OffsetDateTime.of(start, offset);
    }

    public static CourthouseEntity createCourthouse(String name) {
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName(name);
        return courthouse;
    }

    public static CourtroomEntity createCourtroom(CourthouseEntity courthouse, String name) {
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(name);
        return courtroom;
    }

    public static CourtroomEntity createCourtroom(String name) {
        return createCourtroom(createCourthouse("NEWCASTLE"), name);
    }

    public static CourtCaseEntity createCase(String caseNumber) {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCourthouse(createCourthouse("some-courthouse"));
        courtCase.setCaseNumber(caseNumber);
        courtCase.setDefenceList(createDefenceList(courtCase));
        courtCase.setDefendantList(createDefendantList(courtCase));
        courtCase.setProsecutorList(createProsecutorList(courtCase));
        return courtCase;
    }

    public static List<DefenceEntity> createDefenceList(CourtCaseEntity courtCase) {
        DefenceEntity defence1 = createDefence(courtCase, "1");
        DefenceEntity defence2 = createDefence(courtCase, "2");
        return List.of(defence1, defence2);
    }

    public static DefenceEntity createDefence(CourtCaseEntity courtCase, String number) {
        DefenceEntity defenceEntity = new DefenceEntity();
        defenceEntity.setCourtCase(courtCase);
        defenceEntity.setName("defence_" + courtCase.getCaseNumber() + "_" + number);
        return defenceEntity;
    }

    public static List<DefendantEntity> createDefendantList(CourtCaseEntity courtCase) {
        DefendantEntity defendant1 = createDefendant(courtCase, "1");
        DefendantEntity defendant2 = createDefendant(courtCase, "2");
        return List.of(defendant1, defendant2);
    }

    public static DefendantEntity createDefendant(CourtCaseEntity courtCase, String number) {
        DefendantEntity defendantEntity = new DefendantEntity();
        defendantEntity.setCourtCase(courtCase);
        defendantEntity.setName("defendant_" + courtCase.getCaseNumber() + "_" + number);
        return defendantEntity;
    }

    public static List<ProsecutorEntity> createProsecutorList(CourtCaseEntity courtCase) {
        ProsecutorEntity prosecutor1 = createProsecutor(courtCase, "1");
        ProsecutorEntity prosecutor2 = createProsecutor(courtCase, "2");
        return List.of(prosecutor1, prosecutor2);
    }

    public static ProsecutorEntity createProsecutor(CourtCaseEntity courtCase, String number) {
        ProsecutorEntity prosecutorEntity = new ProsecutorEntity();
        prosecutorEntity.setCourtCase(courtCase);
        prosecutorEntity.setName("prosecutor_" + courtCase.getCaseNumber() + "_" + number);
        return prosecutorEntity;
    }

    public static HearingEntity createHearing(CourtCaseEntity courtCase, CourtroomEntity courtroom, LocalDate date) {
        HearingEntity hearing1 = new HearingEntity();
        hearing1.setCourtCase(courtCase);
        hearing1.setCourtroom(courtroom);
        hearing1.setHearingDate(date);
        return hearing1;
    }

    public static HearingEntity createHearing(String caseNumber, LocalTime scheduledStartTime) {
        HearingEntity hearing1 = new HearingEntity();
        hearing1.setCourtCase(createCase(caseNumber));
        hearing1.setCourtroom(createCourtroom("1"));
        hearing1.setHearingDate(LocalDate.of(2023, 6, 20));
        hearing1.setScheduledStartTime(scheduledStartTime);
        return hearing1;
    }

    public static HearingEntity createHearing(CourtCaseEntity courtCaseEntity, CourtroomEntity courtroomEntity) {
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setCourtCase(courtCaseEntity);
        hearingEntity.setCourtroom(courtroomEntity);
        return hearingEntity;
    }

    public static List<HearingEntity> createHearings(int numOfHearings) {
        List<HearingEntity> returnList = new ArrayList<>();
        LocalTime time = LocalTime.of(9, 0, 0);
        for (int counter = 1; counter <= numOfHearings; counter++) {
            returnList.add(createHearing("caseNum_" + counter, time));
            time = time.plusHours(1);
        }
        return returnList;
    }

    public static MediaEntity createMedia(CourtroomEntity courtroomEntity) {
        MediaEntity media = new MediaEntity();
        media.setCourtroom(courtroomEntity);
        return media;
    }

    public static HearingMediaEntity createHearingMedia(HearingEntity hearingEntity, MediaEntity mediaEntity) {
        var hearingMediaEntity = new HearingMediaEntity();
        hearingMediaEntity.setHearing(hearingEntity);
        hearingMediaEntity.setMedia(mediaEntity);

        return hearingMediaEntity;
    }

    public static ExternalObjectDirectoryEntity createExternalObjectDirectory(MediaEntity mediaEntity,
                                                                              ObjectDirectoryStatusEntity objectDirectoryStatusEntity,
                                                                              ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                              UUID externalLocation) {
        var externalObjectDirectory = new ExternalObjectDirectoryEntity();
        externalObjectDirectory.setMedia(mediaEntity);
        externalObjectDirectory.setStatus(objectDirectoryStatusEntity);
        externalObjectDirectory.setExternalLocationType(externalLocationTypeEntity);
        externalObjectDirectory.setExternalLocation(externalLocation);
        externalObjectDirectory.setChecksum(null);
        externalObjectDirectory.setTransferAttempts(null);

        UserAccount user = new UserAccount();
        externalObjectDirectory.setModifiedBy(user);


        return externalObjectDirectory;
    }

    public static TransientObjectDirectoryEntity createTransientObjectDirectory(MediaRequestEntity mediaRequestEntity,
                                                                                ObjectDirectoryStatusEntity objectDirectoryStatusEntity,
                                                                                UUID externalLocation) {

        var transientObjectDirectory = new TransientObjectDirectoryEntity();
        transientObjectDirectory.setMediaRequest(mediaRequestEntity);
        transientObjectDirectory.setStatus(objectDirectoryStatusEntity);
        transientObjectDirectory.setExternalLocation(externalLocation);
        transientObjectDirectory.setChecksum(null);
        transientObjectDirectory.setTransferAttempts(null);

        return transientObjectDirectory;
    }

    public static JudgeEntity createJudge(HearingEntity hearingEntity, String name) {
        JudgeEntity judge = new JudgeEntity();
        judge.setHearing(hearingEntity);
        judge.setName(name);
        return judge;
    }

}
