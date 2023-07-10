package uk.gov.hmcts.darts.common.util;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingMediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccount;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@UtilityClass
@SuppressWarnings({"PMD.TooManyMethods", "HideUtilityClassConstructor"})
public class CommonTestDataUtil {

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

    public static CaseEntity createCase(String caseNumber) {
        CaseEntity courtCase = new CaseEntity();
        courtCase.setCourthouse(createCourthouse("some-courthouse"));
        courtCase.setCaseNumber(caseNumber);
        courtCase.setDefenders(List.of("defender_" + caseNumber + "_1", "defender_" + caseNumber + "_2"));
        courtCase.setDefendants(List.of("defendant_" + caseNumber + "_1", "defendant_" + caseNumber + "_2"));
        courtCase.setProsecutors(List.of("Prosecutor_" + caseNumber + "_1", "Prosecutor_" + caseNumber + "_2"));
        return courtCase;
    }

    public static HearingEntity createHearing(CaseEntity courtCase, CourtroomEntity courtroom, LocalDate date) {
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

    public static HearingEntity createHearing(CaseEntity caseEntity, CourtroomEntity courtroomEntity) {
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setCourtCase(caseEntity);
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

}
