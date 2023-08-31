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
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.dailylist.model.CitizenName;
import uk.gov.hmcts.darts.dailylist.model.CourtList;
import uk.gov.hmcts.darts.dailylist.model.DailyList;
import uk.gov.hmcts.darts.dailylist.model.Defendant;
import uk.gov.hmcts.darts.dailylist.model.Hearing;
import uk.gov.hmcts.darts.dailylist.model.PersonalDetails;
import uk.gov.hmcts.darts.dailylist.model.Sitting;
import uk.gov.hmcts.darts.dailylist.repository.DailyListRepository;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyListProcessorImpl implements DailyListProcessor {
    public static final String DL_TIME_NOT_BEFORE = "NOT BEFORE ";
    public static final String DL_TIME_SITTING_AT = "SITTING AT ";
    public static final String TIME_MARKING_NOTE_FORMAT = "hh:mm a";
    public static final String SITTING_AT_FORMAT = "HH:mm:ss";

    private final DailyListRepository dailyListRepository;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final CourthouseRepository courthouseRepository;
    private final HearingRepository hearingRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void processAllDailyLists(LocalDate date) {
        processAllDailyListsForDateAndCourthouses(LocalDate.now(), courthouseRepository.findAll());
    }

    @Override
    @Transactional
    public void processAllDailyListForCourthouse(CourthouseEntity courthouseEntity) {
        processAllDailyListsForDateAndCourthouses(
            LocalDate.now(),
            List.of(courthouseEntity)
        );
    }

    private void processAllDailyListsForDateAndCourthouses(LocalDate date, List<CourthouseEntity> courthouseEntityList) {
        for (CourthouseEntity courthouse : courthouseEntityList) {
            for (SourceType source : SourceType.values()) {
                List<DailyListEntity> dailyLists = dailyListRepository.findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                    courthouse.getId(),
                    String.valueOf(JobStatusType.NEW),
                    date, String.valueOf(source)
                );
                // Daily lists are being ordered descending by date so first item will be the most recent version
                if (!dailyLists.isEmpty()) {
                    try {
                        processDailyList(dailyLists.get(0));
                    } catch (JsonProcessingException e) {
                        dailyLists.get(0).setStatus(String.valueOf(JobStatusType.FAILED));
                        log.error("Failed to process dailylist for courthouse: {} with dailylist id: {}",
                                  courthouse.getCourthouseName(), dailyLists.get(0).getId(), e
                        );
                    }

                    if (dailyLists.size() > 1) {
                        ignoreOldDailyList(dailyLists.subList(1, dailyLists.size()));
                    }
                }

            }

        }
    }

    private void ignoreOldDailyList(List<DailyListEntity> dailyLists) {
        for (DailyListEntity dailyList : dailyLists) {
            dailyList.setStatus(String.valueOf(JobStatusType.IGNORED));
        }
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void processDailyList(DailyListEntity dailyListEntity) throws JsonProcessingException {
        DailyList dailyList = objectMapper.readValue(dailyListEntity.getContent(), DailyList.class);
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

                        HearingEntity hearing = retrieveCoreObjectService.retrieveOrCreateHearing(
                            courtHouseName, String.valueOf(sitting.getCourtRoomNumber()),
                            caseNumber, dailyListHearing.getHearingDetails().getHearingDate()
                        );

                        CourtCaseEntity courtCase = hearing.getCourtCase();
                        addJudges(sitting, hearing);
                        addDefendants(courtCase, dailyListHearing.getDefendants());
                        addProsecution(courtCase, dailyListHearing);
                        addDefenders(courtCase, dailyListHearing.getDefendants());
                        hearing.setScheduledStartTime(getScheduledStartTime(sitting, dailyListHearing));
                        hearingRepository.saveAndFlush(hearing);
                    }
                }
            } else {
                statusType = JobStatusType.PARTIALLY_PROCESSED;
                log.error("Unregistered courthouse " + courtHouseName + " daily list entry with id "
                          + dailyListEntity.getId() + " has not been processed");
            }
        }
        dailyListEntity.setStatus(statusType.name());
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

    private LocalTime getTimeFromSittingAt(Sitting sitting) throws DateTimeException {
        if (StringUtils.isNotBlank(sitting.getSittingAt())) {
            return LocalTime.parse(sitting.getSittingAt(), new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(SITTING_AT_FORMAT)
                .toFormatter(Locale.ENGLISH));
        }
        return null;
    }


    private LocalTime getTimeFromTimeMarkingNote(final String timeMarkingNote) throws DateTimeException {
        String rawTime;
        if (StringUtils.isNotBlank(timeMarkingNote)) {

            if (timeMarkingNote.startsWith(DL_TIME_NOT_BEFORE)) {
                rawTime = timeMarkingNote.substring(DL_TIME_NOT_BEFORE.length());
            } else if (timeMarkingNote.startsWith(DL_TIME_SITTING_AT)) {
                rawTime = timeMarkingNote.substring(DL_TIME_SITTING_AT.length());
            } else {
                rawTime = timeMarkingNote;
            }

            return LocalTime.parse(rawTime, new DateTimeFormatterBuilder()
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
                    dailyListEntity.setStatus(String.valueOf(JobStatusType.PARTIALLY_PROCESSED));
                    log.error("Hearing not added - HearingInfo does not contain a URN value");
                } else {
                    return urn;
                }
            }
        }
        return hearing.getCaseNumber();

    }


    private void addProsecution(CourtCaseEntity courtCase, Hearing hearing) {
        List<PersonalDetails> advocates = hearing.getProsecution().getAdvocates();
        advocates.forEach(advocate ->
                              courtCase.addProsecutor(retrieveCoreObjectService.createProsecutor(
                                  buildFullName(advocate.getName()), courtCase)));

    }

    private void addDefenders(CourtCaseEntity courtCase, List<Defendant> defendants) {
        for (Defendant defendant : defendants) {
            for (PersonalDetails personalDetails : defendant.getCounsel()) {
                courtCase.addDefence(retrieveCoreObjectService.createDefence(
                    buildFullName(personalDetails.getName()), courtCase));
            }
        }
    }

    private void addDefendants(CourtCaseEntity courtCase, List<Defendant> defendants) {
        for (Defendant defendant : defendants) {
            courtCase.addDefendant(retrieveCoreObjectService.createDefendant(
                buildFullName(defendant.getPersonalDetails().getName()),
                courtCase
            ));
        }

    }

    private void addJudges(Sitting sitting, HearingEntity hearing) {
        for (CitizenName judge : sitting.getJudiciary()) {
            JudgeEntity judgeEntity = retrieveCoreObjectService.retrieveOrCreateJudge(judge.getCitizenNameRequestedName());
            hearing.addJudge(judgeEntity);

        }
    }

    private String buildFullName(CitizenName citizenName) {
        return citizenName.getCitizenNameForename() + " " + citizenName.getCitizenNameSurname();
    }
}
