package uk.gov.hmcts.darts.common.util;

import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
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
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount", "PMD.ExcessiveImports", "PMD.CouplingBetweenObjects"})
@UtilityClass
public class CommonTestDataUtil {

    public static final LocalTime TEN_AM = LocalTime.of(10, 0);

    public static EventEntity createEventWith(String eventName, String eventText, HearingEntity hearingEntity) {

        return createEventWith(eventName, eventText,
                               hearingEntity, createOffsetDateTime("2023-07-01T10:00:00")
        );
    }

    public static EventEntity createEventWith(String eventName, String eventText,
                                              HearingEntity hearingEntity, OffsetDateTime eventTimestamp) {
        EventHandlerEntity eventType = new EventHandlerEntity();
        eventType.setEventName(eventName);

        return createEventWith(1, 1, eventText, hearingEntity, eventType, eventTimestamp, null, true);
    }

    public static EventEntity createEventWith(String eventText,
                                              HearingEntity hearingEntity,
                                              EventHandlerEntity eventHandlerEntity) {

        return createEventWith(eventText, hearingEntity,
                               eventHandlerEntity, createOffsetDateTime("2023-07-01T10:00:00")
        );
    }

    public static EventEntity createEventWith(String eventText, HearingEntity hearingEntity,
                                              EventHandlerEntity eventHandlerEntity, OffsetDateTime eventTimestamp) {

        return createEventWith(1, 1, eventText, hearingEntity, eventHandlerEntity, eventTimestamp, null, true);
    }

    public static EventEntity createEventWith(int id, Integer eventId, String eventText, HearingEntity hearingEntity,
                                              EventHandlerEntity eventHandlerEntity, OffsetDateTime eventTimestamp, OffsetDateTime createdDateTime,
                                              boolean isCurrent) {

        EventEntity event = new EventEntity();
        event.setHearingEntities(List.of(hearingEntity));
        event.setCourtroom(hearingEntity.getCourtroom());
        event.setEventText(eventText);
        event.setId(id);
        event.setEventId(eventId);
        event.setTimestamp(eventTimestamp);
        event.setEventType(eventHandlerEntity);
        event.setCreatedDateTime(createdDateTime);
        event.setIsCurrent(isCurrent);

        return event;
    }

    public static OffsetDateTime createOffsetDateTime(String timestamp) {

        return OffsetDateTime.parse(String.format("%sZ", timestamp));
    }

    public CourthouseEntity createCourthouse(String name) {
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setId(1001);
        courthouse.setCourthouseName(name);
        courthouse.setDisplayName(name);
        return courthouse;
    }

    public CourtroomEntity createCourtroom(CourthouseEntity courthouse, String name) {
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setId(getStringId(courthouse.getCourthouseName() + name));
        courtroom.setCourthouse(courthouse);
        courtroom.setName(name);
        return courtroom;
    }

    public CourtroomEntity createCourtroom(String name) {
        CourthouseEntity courthouse = createCourthouse("SWANSEA");
        return createCourtroom(courthouse, name);
    }

    //gets an ID that is unique-ish to that string. The same string will always produce the same ID.
    private int getStringId(String input) {
        int sum = 0;
        char[] ch = input.toCharArray();
        for (char c : ch) {
            sum += c;
        }
        return sum;
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
        courtCase.setCreatedBy(createUserAccount());
        courtCase.setLastModifiedBy(createUserAccount());
        courtCase.setId(id);
        courtCase.setCreatedDateTime(createOffsetDateTime("2024-03-25T10:00:00"));
        courtCase.setLastModifiedDateTime(createOffsetDateTime("2024-03-25T10:00:00"));
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
        hearing1.setCreatedDateTime(createOffsetDateTime("2024-03-25T10:00:00"));
        return hearing1;
    }

    public HearingEntity createHearing(String caseNumber, LocalTime time) {
        return createHearing(caseNumber, LocalDate.of(2023, 6, 20), time);
    }

    public HearingEntity createHearing(String caseNumber, LocalDate date) {
        return createHearing(caseNumber, date, LocalTime.NOON);
    }

    public HearingEntity createHearing(String caseNumber, LocalDateTime datetime) {
        return createHearing(caseNumber, datetime.toLocalDate(), datetime.toLocalTime());
    }

    public HearingEntity createHearing(CourtCaseEntity courtCase, LocalDateTime datetime) {
        return createHearing(courtCase, createCourtroom("1"), datetime.toLocalDate(), datetime.toLocalTime());
    }

    public HearingEntity createHearing(String caseNumber, LocalDate date, LocalTime time) {
        return createHearing(createCase(caseNumber), createCourtroom("1"), date, time);
    }

    public HearingEntity createHearing(CourtCaseEntity courtCase, CourtroomEntity courtroom, LocalDateTime dateTime) {
        return createHearing(courtCase, courtroom, dateTime.toLocalDate(), dateTime.toLocalTime());
    }

    public HearingEntity createHearing(CourtCaseEntity courtCase, CourtroomEntity courtroom, LocalDate date, LocalTime time) {
        HearingEntity hearing1 = new HearingEntity();
        hearing1.setCourtCase(courtCase);
        hearing1.setCourtroom(courtroom);
        hearing1.setHearingDate(date);
        hearing1.setScheduledStartTime(time);
        hearing1.setId(102);
        hearing1.setTranscriptions(createTranscriptionList(hearing1));
        hearing1.addJudges(createJudges(2));
        return hearing1;
    }

    public MediaEntity createMedia(HearingEntity hearing) {
        String caseNumber = hearing.getCourtCase().getCaseNumber();
        MediaEntity mediaEntity = new MediaEntity();
        OffsetDateTime startTime = OffsetDateTime.of(hearing.getHearingDate(), hearing.getScheduledStartTime(), ZoneOffset.UTC);
        mediaEntity.setStart(startTime);
        mediaEntity.setEnd(startTime.plusHours(1));
        mediaEntity.setChannel(1);
        mediaEntity.setHearingList(List.of(hearing));
        mediaEntity.setCourtroom(hearing.getCourtroom());
        mediaEntity.setId(getStringId("MEDIA_ID" + caseNumber));
        return mediaEntity;
    }

    public MediaEntity createMedia(String caseNumber) {
        HearingEntity hearing = createHearing(caseNumber, TEN_AM);
        return createMedia(hearing);
    }

    public List<TranscriptionEntity> createTranscriptionList(HearingEntity hearing) {
        return createTranscriptionList(hearing, true, true, false, null);
    }

    public List<TranscriptionEntity> createTranscriptionList(HearingEntity hearing, boolean generateStatus) {
        return createTranscriptionList(hearing, generateStatus, true, false, null);
    }

    public List<TranscriptionEntity> createTranscriptionList(HearingEntity hearing, boolean generateStatus, boolean excludeWorkflow) {
        return createTranscriptionList(hearing, generateStatus, excludeWorkflow, false, null);
    }

    public List<TranscriptionEntity> createTranscriptionList(
        HearingEntity hearing,
        boolean generateStatus,
        boolean excludeWorkflow,
        boolean generateRequestor) {
        return createTranscriptionList(hearing, generateStatus, excludeWorkflow, generateRequestor, null);
    }

    public List<TranscriptionEntity> createTranscriptionList(
        HearingEntity hearing,
        boolean generateStatus,
        boolean excludeWorkflow,
        boolean generateRequestor,
        CourtroomEntity courtroom
    ) {
        TranscriptionEntity transcription = new TranscriptionEntity();
        transcription.setTranscriptionType(createTranscriptionTypeEntityFromEnum(TranscriptionTypeEnum.SENTENCING_REMARKS));

        if (hearing != null) {
            transcription.addHearing(hearing);
        }

        if (courtroom != null) {
            transcription.setCourtroom(courtroom);
        }

        transcription.setCreatedDateTime(OffsetDateTime.of(2020, 6, 20, 10, 10, 0, 0, ZoneOffset.UTC));
        transcription.setLegacyObjectId("legacyObjectId");
        transcription.setId(1);
        if (generateRequestor) {
            transcription.setCreatedBy(createUserAccountWithId());
        } else {
            transcription.setCreatedBy(createUserAccount());
        }
        transcription.setTranscriptionDocumentEntities(createTranscriptionDocuments());
        transcription.setTranscriptionUrgency(createTranscriptionUrgencyEntityFromEnum(TranscriptionUrgencyEnum.STANDARD));
        transcription.setTranscriptionCommentEntities(createTranscriptionComments());
        transcription.setIsManualTranscription(true);

        if (!excludeWorkflow) {
            transcription.setTranscriptionWorkflowEntities(createTranscriptionWorkflow());
        }

        if (generateStatus) {
            TranscriptionStatusEntity transcriptionStatus = new TranscriptionStatusEntity();
            transcriptionStatus.setId(TranscriptionStatusEnum.APPROVED.getId());
            transcriptionStatus.setStatusType(TranscriptionStatusEnum.APPROVED.name());
            transcriptionStatus.setDisplayName(TranscriptionStatusEnum.APPROVED.name());
            transcription.setTranscriptionStatus(transcriptionStatus);
        }

        return List.of(transcription);
    }

    private static List<TranscriptionDocumentEntity> createTranscriptionDocuments() {
        List<TranscriptionDocumentEntity> transcriptionDocumentEntities = new ArrayList<>();
        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setFileName("test.doc");
        transcriptionDocumentEntities.add(transcriptionDocumentEntity);
        return transcriptionDocumentEntities;
    }

    private static List<TranscriptionWorkflowEntity> createTranscriptionWorkflow() {
        TranscriptionWorkflowEntity transcriptionWorkflowEntity = new TranscriptionWorkflowEntity();
        transcriptionWorkflowEntity.setTranscriptionComments(createTranscriptionComments("workflowcommenta"));
        transcriptionWorkflowEntity.setWorkflowTimestamp(OffsetDateTime.of(2020, 6, 20, 10, 10, 0, 0, ZoneOffset.UTC));
        transcriptionWorkflowEntity.setWorkflowActor(createUserAccount("workflow user"));
        transcriptionWorkflowEntity.setTranscriptionStatus(createTranscriptionStatus(TranscriptionStatusEnum.REQUESTED));

        TranscriptionWorkflowEntity transcriptionWorkflowEntity2 = new TranscriptionWorkflowEntity();
        transcriptionWorkflowEntity2.setTranscriptionComments(createTranscriptionComments("workflowcommentb"));
        transcriptionWorkflowEntity2.setWorkflowActor(createUserAccount("workflow user 2"));
        transcriptionWorkflowEntity2.setTranscriptionStatus(createTranscriptionStatus(TranscriptionStatusEnum.APPROVED));

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = new ArrayList<>();
        transcriptionWorkflowEntities.add(transcriptionWorkflowEntity);
        transcriptionWorkflowEntities.add(transcriptionWorkflowEntity2);
        return transcriptionWorkflowEntities;
    }

    private static TranscriptionStatusEntity createTranscriptionStatus(TranscriptionStatusEnum statusEnum) {
        TranscriptionStatusEntity entity = new TranscriptionStatusEntity();
        entity.setStatusType(statusEnum.name());
        entity.setId(statusEnum.getId());
        entity.setStatusType(statusEnum.name());
        return entity;

    }

    private static List<TranscriptionCommentEntity> createTranscriptionComments() {
        return createTranscriptionComments("comment");
    }

    private static List<TranscriptionCommentEntity> createTranscriptionComments(String prefixMessage) {
        List<TranscriptionCommentEntity> transcriptionCommentEntities = new ArrayList<>();
        TranscriptionCommentEntity transcriptionCommentEntity = new TranscriptionCommentEntity();
        TranscriptionCommentEntity transcriptionCommentEntity2 = new TranscriptionCommentEntity();
        transcriptionCommentEntity.setComment(prefixMessage + "1");
        transcriptionCommentEntity2.setComment(prefixMessage + "2");
        transcriptionCommentEntities.add(transcriptionCommentEntity);
        transcriptionCommentEntities.add(transcriptionCommentEntity2);
        return transcriptionCommentEntities;
    }

    public UserAccountEntity createUserAccount() {
        return createUserAccount("testUsername");
    }

    public UserAccountEntity createUserAccount(String userName) {
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setUserName(userName);
        userAccount.setUserFullName(userName);
        userAccount.setEmailAddress("test@test.com");
        return userAccount;
    }

    public UserAccountEntity createUserAccountWithId() {
        UserAccountEntity userAccount = createUserAccount("testUsername");
        userAccount.setId(1002);
        return userAccount;
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
        LocalTime time = LocalTime.of(9, 0, 30);
        for (int counter = 1; counter <= numOfHearings; counter++) {
            returnList.add(createHearing("caseNum_" + counter, time));
            time = time.plusHours(1);
        }
        return returnList;
    }

    public void createHearingsForCase(CourtCaseEntity courtCase, int numOfCourtrooms, int numOfHearingsPerCourtroom) {
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        List<HearingEntity> hearings = new ArrayList<>();
        for (int courtroomCounter = 1; courtroomCounter <= numOfCourtrooms; courtroomCounter++) {
            CourtroomEntity courtroom = createCourtroom("courtroom" + courtroomCounter);
            for (int hearingCounter = 1; hearingCounter <= numOfHearingsPerCourtroom; hearingCounter++) {
                HearingEntity hearing = createHearing(courtCase, courtroom, startDate);
                hearings.add(hearing);
                startDate = startDate.plusDays(1);
            }
        }
        courtCase.setHearings(hearings);
    }

    public AddCaseRequest createAddCaseRequest() {

        AddCaseRequest request = new AddCaseRequest();
        request.setCourthouse("Swansea");
        request.setCaseNumber("2");
        request.setDefendants(Lists.newArrayList("Defendant1"));
        request.setJudges(Lists.newArrayList("Judge1"));
        request.setProsecutors(Lists.newArrayList("Prosecutor1"));
        request.setDefenders(Lists.newArrayList("Defender1"));
        return request;
    }


    public AddCaseRequest createUpdateCaseRequest() {

        AddCaseRequest request = new AddCaseRequest();
        request.setCourthouse("Swansea");
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
        dailyListEntity.setListingCourthouse("SWANSEA");
        dailyListEntity.setContent(TestUtils.substituteHearingDateWithToday(getContentsFromFile(filelocation)));
        dailyListEntity.setPublishedTimestamp(OffsetDateTime.of(LocalDate.now(), time, ZoneOffset.UTC));
        dailyListEntity.setSource(source);
        return dailyListEntity;
    }


    public DailyListEntity createInvalidDailyList(LocalTime time) {
        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setStatus(JobStatusType.NEW);
        dailyListEntity.setListingCourthouse("SWANSEA");
        dailyListEntity.setContent("blah");
        dailyListEntity.setPublishedTimestamp(OffsetDateTime.of(LocalDate.now(), time, ZoneOffset.UTC));
        dailyListEntity.setSource(String.valueOf(SourceType.XHB));
        return dailyListEntity;
    }

    public DailyListEntity createInvalidXmlDailyList(LocalTime time) {
        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setStatus(JobStatusType.NEW);
        dailyListEntity.setListingCourthouse("SWANSEA");
        dailyListEntity.setXmlContent("blah");
        dailyListEntity.setPublishedTimestamp(OffsetDateTime.of(LocalDate.now(), time, ZoneOffset.UTC));
        dailyListEntity.setSource(String.valueOf(SourceType.XHB));
        return dailyListEntity;
    }

    public List<TranscriptionTypeEntity> createTranscriptionTypeEntities() {
        List<TranscriptionTypeEntity> transcriptionTypeEntities = new ArrayList<>();
        for (TranscriptionTypeEnum transcriptionTypeEnum : TranscriptionTypeEnum.values()) {
            transcriptionTypeEntities.add(createTranscriptionTypeEntityFromEnum(transcriptionTypeEnum));
        }
        return transcriptionTypeEntities;
    }

    public TranscriptionTypeEntity createTranscriptionTypeEntityFromEnum(TranscriptionTypeEnum transcriptionTypeEnum) {
        TranscriptionTypeEntity transcriptionTypeEntity = new TranscriptionTypeEntity();
        transcriptionTypeEntity.setId(transcriptionTypeEnum.getId());
        transcriptionTypeEntity.setDescription(transcriptionTypeEnum.name());
        return transcriptionTypeEntity;
    }

    public List<TranscriptionUrgencyEntity> createTranscriptionUrgencyEntities() {
        List<TranscriptionUrgencyEntity> transcriptionUrgencyEntities = new ArrayList<>();
        for (TranscriptionUrgencyEnum transcriptionUrgencyEnum : TranscriptionUrgencyEnum.values()) {
            switch (transcriptionUrgencyEnum.getId()) {
                case 2:
                    transcriptionUrgencyEntities.add(createTranscriptionUrgencyEntityFromEnum(transcriptionUrgencyEnum));
                    break;
                case 3:
                    transcriptionUrgencyEntities.add(createTranscriptionUrgencyEntityFromEnum(transcriptionUrgencyEnum));
                    break;
                case 4:
                    transcriptionUrgencyEntities.add(createTranscriptionUrgencyEntityFromEnum(transcriptionUrgencyEnum));
                    break;
                case 5:
                    transcriptionUrgencyEntities.add(createTranscriptionUrgencyEntityFromEnum(transcriptionUrgencyEnum));
                    break;
                case 6:
                    transcriptionUrgencyEntities.add(createTranscriptionUrgencyEntityFromEnum(transcriptionUrgencyEnum));
                    break;
                case 7:
                    transcriptionUrgencyEntities.add(createTranscriptionUrgencyEntityFromEnum(transcriptionUrgencyEnum));
                    break;
                default:
            }

        }
        return transcriptionUrgencyEntities;
    }

    public TranscriptionUrgencyEntity createTranscriptionUrgencyEntityFromEnum(TranscriptionUrgencyEnum transcriptionUrgencyEnum) {
        TranscriptionUrgencyEntity transcriptionUrgencyEntity = new TranscriptionUrgencyEntity();
        transcriptionUrgencyEntity.setId(transcriptionUrgencyEnum.getId());
        transcriptionUrgencyEntity.setDescription(transcriptionUrgencyEnum.name());
        transcriptionUrgencyEntity.setPriorityOrder(transcriptionUrgencyEnum.getPriorityOrderId());

        return transcriptionUrgencyEntity;
    }

    public static AnnotationEntity createAnnotationEntity(Integer id) {
        AnnotationEntity annotationEntity = new AnnotationEntity();
        annotationEntity.setId(id);
        annotationEntity.setTimestamp(OffsetDateTime.now());
        annotationEntity.setText("Some text");
        annotationEntity.setAnnotationDocuments(createAnnotationDocumentEntityList());
        return annotationEntity;
    }

    public static List<AnnotationDocumentEntity> createAnnotationDocumentEntityList() {
        List<AnnotationDocumentEntity> annotationDocumentEntityList =
            new ArrayList<>();
        annotationDocumentEntityList.add(createAnnotationDocumentEntity(1));
        annotationDocumentEntityList.add(createAnnotationDocumentEntity(2));
        return annotationDocumentEntityList;
    }

    public static AnnotationDocumentEntity createAnnotationDocumentEntity(Integer id) {
        AnnotationDocumentEntity annotationDocumentEntity =
            new AnnotationDocumentEntity();
        annotationDocumentEntity.setId(id);
        annotationDocumentEntity.setFileName("filename");
        annotationDocumentEntity.setFileType("filetype");
        UserAccountEntity userAccountEntity = createUserAccount("annotator");
        userAccountEntity.setUserFullName("annotator user");
        annotationDocumentEntity.setUploadedBy(userAccountEntity);
        annotationDocumentEntity.setUploadedDateTime(OffsetDateTime.now());
        return annotationDocumentEntity;
    }
}