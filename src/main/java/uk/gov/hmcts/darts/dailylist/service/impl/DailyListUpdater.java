package uk.gov.hmcts.darts.dailylist.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.dailylist.mapper.CitizenNameMapper;
import uk.gov.hmcts.darts.dailylist.model.CitizenName;
import uk.gov.hmcts.darts.dailylist.model.CourtList;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.Defendant;
import uk.gov.hmcts.darts.dailylist.model.Hearing;
import uk.gov.hmcts.darts.dailylist.model.PersonalDetails;
import uk.gov.hmcts.darts.dailylist.model.Sitting;
import uk.gov.hmcts.darts.dailylist.util.CitizenNameComparator;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
class DailyListUpdater {
    public static final String DL_TIME_NOT_BEFORE = "NOT BEFORE ";
    public static final String DL_TIME_SITTING_AT = "SITTING AT ";
    public static final String TIME_MARKING_NOTE_FORMAT = "h:mm a";
    public static final String SITTING_AT_FORMAT = "HH:mm:ss";

    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final CourthouseRepository courthouseRepository;
    private final HearingRepository hearingRepository;
    private final ObjectMapper objectMapper;
    private final SystemUserHelper systemUserHelper;
    private final CurrentTimeHelper currentTimeHelper;
    private final CitizenNameMapper citizenNameMapper;
    private final CitizenNameComparator citizenNameComparator;

    @SuppressWarnings({"checkstyle:VariableDeclarationUsageDistance", "PMD.CognitiveComplexity"})
    @Transactional
    public void processDailyList(DailyListEntity dailyListEntity) throws JsonProcessingException {
        UserAccountEntity dailyListSystemUser = systemUserHelper.getDailyListProcessorUser();
        DailyListJsonObject dailyList = objectMapper.readValue(dailyListEntity.getContent(), DailyListJsonObject.class);
        JobStatusType statusType = JobStatusType.PROCESSED;

        for (CourtList courtList : dailyList.getCourtLists()) {

            String courtHouseName = courtList.getCourtHouse().getCourtHouseName();
            Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseNameIgnoreCase(
                courtHouseName);

            if (foundCourthouse.isPresent()) {
                List<Sitting> sittings = courtList.getSittings();
                for (Sitting sitting : sittings) {
                    List<Hearing> hearings = sitting.getHearings();
                    for (Hearing dailyListHearing : hearings) {

                        String caseNumber = getCaseNumber(dailyListEntity, dailyListHearing);
                        if (caseNumber == null) {
                            statusType = JobStatusType.PARTIALLY_PROCESSED;
                            continue;
                        }

                        LocalTime scheduledStartTime = getScheduledStartTime(sitting, dailyListHearing);
                        LocalDateTime hearingDateTime = dailyListHearing.getHearingDetails().getHearingDate().atTime(scheduledStartTime);

                        HearingEntity hearing = retrieveCoreObjectService.retrieveOrCreateHearing(
                            courtHouseName, sitting.getCourtRoomNumber(),
                            caseNumber, hearingDateTime,
                            dailyListSystemUser
                        );

                        hearing.setLastModifiedDateTime(currentTimeHelper.currentOffsetDateTime());

                        CourtCaseEntity courtCase = hearing.getCourtCase();
                        courtCase.setLastModifiedDateTime(currentTimeHelper.currentOffsetDateTime());
                        addJudges(sitting, hearing);
                        addDefendants(courtCase, dailyListHearing.getDefendants());
                        addProsecution(courtCase, dailyListHearing);
                        addDefenders(courtCase, dailyListHearing.getDefendants());
                        hearingRepository.saveAndFlush(hearing);
                    }
                }
            } else {
                statusType = JobStatusType.PARTIALLY_PROCESSED;
                log.error("Unregistered courthouse " + courtHouseName + " daily list entry with id "
                              + dailyListEntity.getId() + " has not been processed");
            }
        }
        dailyListEntity.setLastModifiedBy(dailyListSystemUser);
        dailyListEntity.setStatus(statusType);
    }

    private LocalTime getScheduledStartTime(Sitting sitting, Hearing dailyListHearing) {
        String timeMarkingNoteText = dailyListHearing.getTimeMarkingNote();
        if (StringUtils.isNotBlank(timeMarkingNoteText)) {
            try {
                return getTimeFromTimeMarkingNote(timeMarkingNoteText);
            } catch (DateTimeException dateTimeException) {
                log.warn("Ignore error and continue, Parsing failed for field TimeMarkingNote with value: "
                             + timeMarkingNoteText, dateTimeException);
            }
        }

        if (StringUtils.isNotBlank(sitting.getSittingAt())) {
            try {
                return getTimeFromSittingAt(sitting);
            } catch (DateTimeException dateTimeException) {
                log.warn("Ignore error and continue, Parsing failed for field SittingAt with value: "
                             + sitting.getSittingAt(), dateTimeException);
            }
        }
        return null;
    }

    private LocalTime getTimeFromSittingAt(Sitting sitting) {
        if (StringUtils.isNotBlank(sitting.getSittingAt())) {
            return LocalTime.parse(sitting.getSittingAt(), new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(SITTING_AT_FORMAT)
                .toFormatter(Locale.ENGLISH));
        }
        return null;
    }


    protected LocalTime getTimeFromTimeMarkingNote(final String timeMarkingNote) {
        String rawTime;
        if (StringUtils.isNotBlank(timeMarkingNote)) {

            if (timeMarkingNote.startsWith(DL_TIME_NOT_BEFORE)) {
                rawTime = timeMarkingNote.substring(DL_TIME_NOT_BEFORE.length());
            } else if (timeMarkingNote.startsWith(DL_TIME_SITTING_AT)) {
                rawTime = timeMarkingNote.substring(DL_TIME_SITTING_AT.length());
            } else {
                rawTime = timeMarkingNote;
            }

            return LocalTime.parse(rawTime.strip(), new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(TIME_MARKING_NOTE_FORMAT)
                .toFormatter(Locale.ENGLISH));
        }
        return null;
    }

    private String getCaseNumber(DailyListEntity dailyListEntity, Hearing hearing) {
        // CPP don't provide case id, use URN
        if (String.valueOf(SourceType.CPP).equalsIgnoreCase(dailyListEntity.getSource())) {
            if (hearing.getDefendants().isEmpty()) {
                return hearing.getCaseNumber();
            } else {
                String urn = hearing.getDefendants().get(0).getUrn();
                if (StringUtils.isBlank(urn)) {
                    log.warn("Case number not found for hearing: daily_list_id={}, hearing_date={}",
                             dailyListEntity.getId(), hearing.getHearingDetails().getHearingDate());
                    return null;
                } else {
                    return urn;
                }
            }
        }
        return hearing.getCaseNumber();

    }


    private void addProsecution(CourtCaseEntity courtCase, Hearing hearing) {
        if (hearing.getProsecution() == null) {
            return;
        }
        List<PersonalDetails> advocates = hearing.getProsecution().getAdvocates();
        UserAccountEntity dailyListSystemUser = systemUserHelper.getDailyListProcessorUser();
        advocates.forEach(advocate -> {
            if (!isExistingProsecutor(courtCase, advocate)) {
                courtCase.addProsecutor(retrieveCoreObjectService.createProsecutor(
                    citizenNameMapper.getCitizenName(advocate.getName()), courtCase, dailyListSystemUser));
            }
        });
    }

    private void addDefenders(CourtCaseEntity courtCase, List<Defendant> defendants) {
        UserAccountEntity dailyListSystemUser = systemUserHelper.getDailyListProcessorUser();
        for (Defendant defendant : defendants) {
            for (PersonalDetails counselDetails : defendant.getCounsel()) {
                if (counselDetails == null) {
                    continue;
                }
                if (!isExistingDefenders(courtCase, counselDetails)) {
                    courtCase.addDefence(retrieveCoreObjectService.createDefence(
                        citizenNameMapper.getCitizenName(counselDetails.getName()), courtCase, dailyListSystemUser));
                }
            }
        }
    }

    private void addDefendants(CourtCaseEntity courtCase, List<Defendant> defendants) {
        UserAccountEntity dailyListSystemUser = systemUserHelper.getDailyListProcessorUser();
        for (Defendant defendant : defendants) {
            if (!isExistingDefendant(courtCase, defendant)) {
                courtCase.addDefendant(retrieveCoreObjectService.createDefendant(
                    citizenNameMapper.getCitizenName(defendant.getPersonalDetails().getName()),
                    courtCase,
                    dailyListSystemUser
                ));
            }
        }
    }

    private void addJudges(Sitting sitting, HearingEntity hearing) {
        UserAccountEntity dailyListSystemUser = systemUserHelper.getDailyListProcessorUser();
        for (CitizenName judge : sitting.getJudiciary()) {
            JudgeEntity judgeEntity = retrieveCoreObjectService.retrieveOrCreateJudge(judge.getCitizenNameRequestedName(), dailyListSystemUser);
            hearing.addJudge(judgeEntity, true);
        }
    }

    private boolean isExistingDefenders(CourtCaseEntity courtCase, PersonalDetails defenders) {
        boolean existingDefendant = false;
        for (DefenceEntity defenceEntity : courtCase.getDefenceList()) {
            if (isEqual(defenders.getName(), citizenNameMapper.getCitizenName(defenceEntity.getName()))) {
                existingDefendant = true;
            }
        }

        return existingDefendant;
    }

    private boolean isExistingDefendant(CourtCaseEntity courtCase, Defendant defendant) {
        boolean existingDefendant = false;
        for (DefendantEntity defendantEntity : courtCase.getDefendantList()) {
            if (isEqual(defendant.getPersonalDetails().getName(), citizenNameMapper.getCitizenName(defendantEntity.getName()))) {
                existingDefendant = true;
            }
        }

        return existingDefendant;
    }

    private boolean isExistingProsecutor(CourtCaseEntity courtCase, PersonalDetails prosecutor) {
        boolean existingDefendant = false;
        for (ProsecutorEntity prosecutorEntity : courtCase.getProsecutorList()) {

            if (isEqual(prosecutor.getName(), citizenNameMapper.getCitizenName(prosecutorEntity.getName()))) {
                existingDefendant = true;
            }
        }

        return existingDefendant;
    }

    private boolean isEqual(CitizenName name1, CitizenName name2) {
        return citizenNameComparator.compare(name1, name2) == 0;
    }

}