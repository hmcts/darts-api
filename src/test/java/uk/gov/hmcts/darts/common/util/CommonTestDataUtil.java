package uk.gov.hmcts.darts.common.util;

import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@UtilityClass
public class CommonTestDataUtil {

    public static EventEntity createEventWith(String eventName, String eventText, HearingEntity hearingEntity) {

        return createEventWith(eventName, eventText,
                               hearingEntity, createOffsetDateTime("2023-07-01T10:00:00")
        );
    }

    public static EventEntity createEventWith(String eventName, String eventText,
                                              HearingEntity hearingEntity, OffsetDateTime eventTimestamp) {

        EventEntity event = new EventEntity();
        event.setHearingEntities(List.of(hearingEntity));
        event.setCourtroom(hearingEntity.getCourtroom());
        event.setEventName(eventName);
        event.setEventText(eventText);
        event.setId(1);
        event.setTimestamp(eventTimestamp);

        return event;
    }

    public static EventEntity createEventWith(String eventName, String eventText,
                                              HearingEntity hearingEntity,
                                              EventHandlerEntity eventHandlerEntity) {

        return createEventWith(eventName, eventText, hearingEntity,
                               eventHandlerEntity, createOffsetDateTime("2023-07-01T10:00:00")
        );
    }

    public static EventEntity createEventWith(String eventName, String eventText, HearingEntity hearingEntity,
                                              EventHandlerEntity eventHandlerEntity, OffsetDateTime eventTimestamp) {

        EventEntity event = new EventEntity();
        event.setHearingEntities(List.of(hearingEntity));
        event.setCourtroom(hearingEntity.getCourtroom());
        event.setEventName(eventName);
        event.setEventText(eventText);
        event.setId(1);
        event.setTimestamp(eventTimestamp);
        event.setEventType(eventHandlerEntity);

        return event;
    }

    public EventHandlerEntity createEventHandlerWith(String eventName, String type, String subType) {
        EventHandlerEntity eventHandlerEntity = new EventHandlerEntity();
        eventHandlerEntity.setEventName(eventName);
        eventHandlerEntity.setType(type);
        eventHandlerEntity.setSubType(subType);
        return eventHandlerEntity;
    }


    public static OffsetDateTime createOffsetDateTime(String timestamp) {

        return OffsetDateTime.parse(String.format("%sZ", timestamp));
    }

    public CourthouseEntity createCourthouse(String name) {
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName(name);
        return courthouse;
    }

    public CourtroomEntity createCourtroom(CourthouseEntity courthouse, String name) {
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(name);
        return courtroom;
    }

    public CourtroomEntity createCourtroom(String name) {
        createCourthouse("SWANSEA");
        return createCourtroom(createCourthouse("SWANSEA"), name);
    }

    public CourtCaseEntity createCase(String caseNumber, CourthouseEntity courthouseEntity) {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCourthouse(courthouseEntity);
        courtCase.setCaseNumber(caseNumber);
        courtCase.setDefenceList(createDefenceList(courtCase));
        courtCase.setDefendantList(createDefendantList(courtCase));
        courtCase.setProsecutorList(createProsecutorList(courtCase));
        return courtCase;
    }

    public CourtCaseEntity createCase(String caseNumber) {
        return createCaseWithId(caseNumber, 101);
    }

    public CourtCaseEntity createCaseWithId(String caseNumber, Integer id) {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber(caseNumber);
        courtCase.setDefenceList(createDefenceList(courtCase));
        courtCase.setDefendantList(createDefendantList(courtCase));
        courtCase.setProsecutorList(createProsecutorList(courtCase));
        courtCase.setCourthouse(createCourthouse("case_courthouse"));
        courtCase.setJudges(createJudges(2));
        courtCase.setId(id);
        return courtCase;
    }

    public static List<DefenceEntity> createDefenceList(CourtCaseEntity courtCase) {
        DefenceEntity defence1 = createDefence(courtCase, "1");
        DefenceEntity defence2 = createDefence(courtCase, "2");
        return new ArrayList<>(List.of(defence1, defence2));
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
        return new ArrayList<>(List.of(defendant1, defendant2));
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
        return new ArrayList<>(List.of(prosecutor1, prosecutor2));
    }

    public static ProsecutorEntity createProsecutor(CourtCaseEntity courtCase, String number) {
        ProsecutorEntity prosecutorEntity = new ProsecutorEntity();
        prosecutorEntity.setCourtCase(courtCase);
        prosecutorEntity.setName("prosecutor_" + courtCase.getCaseNumber() + "_" + number);
        return prosecutorEntity;
    }

    public HearingEntity createHearing(CourtCaseEntity courtcase, CourtroomEntity courtroom, LocalDate date) {
        HearingEntity hearing1 = new HearingEntity();
        hearing1.setId(1);
        hearing1.setCourtCase(courtcase);
        hearing1.setCourtroom(courtroom);
        hearing1.setHearingDate(date);
        return hearing1;
    }

    public HearingEntity createHearing(String caseNumber, LocalTime time) {
        HearingEntity hearing1 = new HearingEntity();
        hearing1.setCourtCase(createCase(caseNumber));
        hearing1.setCourtroom(createCourtroom("1"));
        hearing1.setHearingDate(LocalDate.of(2023, 6, 20));
        hearing1.setScheduledStartTime(time);
        hearing1.setId(102);
        hearing1.setTranscriptions(createTranscriptionList(hearing1));
        hearing1.addJudges(createJudges(2));
        return hearing1;
    }

    public List<TranscriptionEntity> createTranscriptionList(HearingEntity hearing) {
        TranscriptionEntity transcription = new TranscriptionEntity();
        transcription.setCourtCase(hearing.getCourtCase());
        transcription.setTranscriptionType(new TranscriptionTypeEntity());
        transcription.setCourtroom(hearing.getCourtroom());
        transcription.setHearing(hearing);
        return List.of(transcription);
    }

    public List<JudgeEntity> createJudges(int numOfJudges) {
        List<JudgeEntity> returnList = new ArrayList<>();
        for (int counter = 1; counter <= numOfJudges; counter++) {
            returnList.add(createJudge("Judge_" + counter));
        }
        return returnList;
    }

    public JudgeEntity createJudge(String name) {
        JudgeEntity judgeEntity = new JudgeEntity();
        judgeEntity.setName(name);
        return judgeEntity;
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

    public AddCaseRequest createAddCaseRequest() {

        AddCaseRequest request = new AddCaseRequest("Swansea", "case_number");
        request.setCaseNumber("2");
        request.setDefendants(Lists.newArrayList("Defendant1"));
        request.setJudges(Lists.newArrayList("Judge1"));
        request.setProsecutors(Lists.newArrayList("Prosecutor1"));
        request.setDefenders(Lists.newArrayList("Defender1"));
        return request;
    }


    public AddCaseRequest createUpdateCaseRequest() {

        AddCaseRequest request = new AddCaseRequest("Swansea", "case_number");
        request.setCaseNumber("case1");
        request.setDefendants(Lists.newArrayList("UpdatedDefendant1"));
        request.setJudges(Lists.newArrayList("UpdateJudge1"));
        request.setProsecutors(Lists.newArrayList("UpdateProsecutor1"));
        request.setDefenders(Lists.newArrayList("UpdateDefender1"));
        return request;
    }

    public DailyListEntity createDailyList(LocalTime time, String source, String filelocation) throws IOException {
        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setStatus(JobStatusType.NEW);
        dailyListEntity.setCourthouse(createCourthouse("SWANSEA"));
        dailyListEntity.setContent(TestUtils.substituteHearingDateWithToday(getContentsFromFile(filelocation)));
        dailyListEntity.setPublishedTimestamp(OffsetDateTime.of(LocalDate.now(), time, ZoneOffset.UTC));
        dailyListEntity.setSource(source);
        return dailyListEntity;
    }


    public DailyListEntity createInvalidDailyList(LocalTime time) {
        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setStatus(JobStatusType.NEW);
        dailyListEntity.setCourthouse(createCourthouse("SWANSEA"));
        dailyListEntity.setContent("blah");
        dailyListEntity.setPublishedTimestamp(OffsetDateTime.of(LocalDate.now(), time, ZoneOffset.UTC));
        dailyListEntity.setSource(String.valueOf(SourceType.XHB));
        return dailyListEntity;
    }

    public DailyListEntity createInvalidXmlDailyList(LocalTime time) {
        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setStatus(JobStatusType.NEW);
        dailyListEntity.setCourthouse(createCourthouse("SWANSEA"));
        dailyListEntity.setXmlContent("blah");
        dailyListEntity.setPublishedTimestamp(OffsetDateTime.of(LocalDate.now(), time, ZoneOffset.UTC));
        dailyListEntity.setSource(String.valueOf(SourceType.XHB));
        return dailyListEntity;
    }


}
