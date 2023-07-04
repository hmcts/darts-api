package uk.gov.hmcts.darts.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.common.entity.HearingMediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommonTestDataUtil {

    public static final String COURTHOUSE_NAME_SWANSEA = "SWANSEA";
    @Autowired
    private HearingRepository hearingRepository;
    @Autowired
    private CaseRepository caseRepository;
    @Autowired
    private CourthouseRepository courthouseRepository;
    @Autowired
    private CourtroomRepository courtroomRepository;


    public CourthouseEntity createCourthouse(String name) {
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseName(name);
        if (foundCourthouse.isEmpty()) {
            CourthouseEntity courthouse = new CourthouseEntity();
            courthouse.setCourthouseName(name);
            courthouseRepository.saveAndFlush(courthouse);
            return courthouse;
        } else {
            return foundCourthouse.get();
        }
    }

    public CourtroomEntity createCourtroom(CourthouseEntity courthouse, String name) {
        CourtroomEntity foundCourtroom = courtroomRepository.findByNames(courthouse.getCourthouseName(), name);
        if (foundCourtroom == null) {
            CourtroomEntity courtroom = new CourtroomEntity();
            courtroom.setCourthouse(courthouse);
            courtroom.setName(name);
            courtroomRepository.saveAndFlush(courtroom);
            return courtroom;
        } else {
            return foundCourtroom;
        }
    }

    public CourtroomEntity createCourtroom(String name) {
        return createCourtroom(createCourthouse(COURTHOUSE_NAME_SWANSEA), name);
    }

    public CaseEntity createCase(String caseNumber) {
        CaseEntity courtCase = new CaseEntity();
        courtCase.setCaseNumber(caseNumber);
        courtCase.setDefenders(List.of("defender_" + caseNumber + "_1", "defender_" + caseNumber + "_2"));
        courtCase.setDefendants(List.of("defendant_" + caseNumber + "_1", "defendant_" + caseNumber + "_2"));
        courtCase.setProsecutors(List.of("Prosecutor_" + caseNumber + "_1", "Prosecutor_" + caseNumber + "_2"));
        courtCase.setCourthouse(createCourthouse(COURTHOUSE_NAME_SWANSEA));
        caseRepository.saveAndFlush(courtCase);
        return courtCase;
    }

    public HearingEntity createHearing(CaseEntity courtcase, CourtroomEntity courtroom, LocalDate date) {
        HearingEntity hearing1 = new HearingEntity();
        hearing1.setCourtCase(courtcase);
        hearing1.setCourtroom(courtroom);
        hearing1.setHearingDate(date);
        hearingRepository.saveAndFlush(hearing1);
        return hearing1;
    }

    public HearingEntity createHearing(String caseNumber, LocalTime scheduledStartTime) {
        HearingEntity hearing1 = new HearingEntity();
        hearing1.setCourtCase(createCase(caseNumber));
        hearing1.setCourtroom(createCourtroom("1"));
        hearing1.setHearingDate(LocalDate.of(2023, 6, 20));
        hearing1.setScheduledStartTime(scheduledStartTime);
        hearingRepository.saveAndFlush(hearing1);
        return hearing1;
    }

    public HearingEntity createHearing(CaseEntity caseEntity, CourtroomEntity courtroomEntity) {
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setCourtCase(caseEntity);
        hearingEntity.setCourtroom(courtroomEntity);
        return hearingEntity;
    }

    public List<HearingEntity> createHearings(int numOfHearings) {
        List<HearingEntity> returnList = new ArrayList<>();
        LocalTime time = LocalTime.of(9, 0, 0);
        for (int counter = 1; counter <= numOfHearings; counter++) {
            returnList.add(createHearing("caseNum_" + counter, time));
            time = time.plusHours(1);
        }
        return returnList;
    }

    public MediaEntity createMedia() {
        return new MediaEntity();
    }

    public HearingMediaEntity createHearingMedia(HearingEntity hearingEntity, MediaEntity mediaEntity) {
        var hearingMediaEntity = new HearingMediaEntity();
        hearingMediaEntity.setHearing(hearingEntity);
        hearingMediaEntity.setMedia(mediaEntity);

        return hearingMediaEntity;
    }

}
