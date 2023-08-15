package uk.gov.hmcts.darts.dailylist.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.dailylist.exception.DailyListError;
import uk.gov.hmcts.darts.dailylist.model.CitizenName;
import uk.gov.hmcts.darts.dailylist.model.CourtList;
import uk.gov.hmcts.darts.dailylist.model.DailyList;
import uk.gov.hmcts.darts.dailylist.model.Defendant;
import uk.gov.hmcts.darts.dailylist.model.Hearing;
import uk.gov.hmcts.darts.dailylist.model.Sitting;
import uk.gov.hmcts.darts.dailylist.repository.DailyListRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyListProcessorImpl implements DailyListProcessor {
    private final DailyListRepository dailyListRepository;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final CourthouseRepository courthouseRepository;
    private final CaseRepository caseRepository;
    private final HearingRepository hearingRepository;

    private final ObjectMapper objectMapper;

    @Override
    public void processAllDailyLists(LocalDate date) {
        List<CourthouseEntity> allCourthouses = courthouseRepository.findAll();
        for (CourthouseEntity allCourthouse : allCourthouses) {
            List<DailyListEntity> dailyLists = dailyListRepository.findByCourthouse_IdAndStatusAndStartDateOrderByPublishedTimestampDesc(
                    allCourthouse.getId(),
                    String.valueOf(JobStatusType.NEW),
                    date
            );

            // Daily lists are being ordered descending by date so first item will be the most recent version
            if (!dailyLists.isEmpty()) {
                try {
                    processDailyList(dailyLists.get(0));
                } catch (JsonProcessingException e) {
                    throw new DartsApiException(DailyListError.FAILED_TO_PROCESS_DAILYLIST, e);
                }

            }
            if (dailyLists.size() > 1) {
                ignoreOldDailyList(dailyLists.subList(1, dailyLists.size()));
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
                        CourtCaseEntity courtCase = retrieveCoreObjectService.retrieveOrCreateCase(
                                courtHouseName,
                                dailyListHearing.getCaseNumber()
                        );
                        HearingEntity hearing = retrieveCoreObjectService.retrieveOrCreateHearing(
                                courtHouseName,
                                String.valueOf(sitting.getCourtRoomNumber()),
                                dailyListHearing.getCaseNumber(),
                                dailyListHearing.getHearingDetails().getHearingDate()
                        );

                        addJudges(sitting, hearing);
                        addDefendants(courtCase, dailyListHearing.getDefendants());
                        addProsecutions(
                                courtCase,
                                buildFullName(dailyListHearing.getProsecution().getAdvocate().getPersonalDetails().getName())
                        );
                        addDefenders(courtCase, dailyListHearing.getDefendants());
                        setCourtroom(hearing, courtCase, sitting.getCourtRoomNumber());
                        setCaseNumber(courtCase, dailyListHearing.getDefendants(), dailyListEntity, dailyListHearing);
                        hearing.setHearingDate(dailyListHearing.getHearingDetails().getHearingDate());

                        courtCase.addHearing(hearing);
                        hearingRepository.saveAndFlush(hearing);
                        caseRepository.saveAndFlush(courtCase);


                    }
                }
            } else {
                statusType = JobStatusType.PARTIALLY_PROCESSED;
                log.info("Unregistered courthouse " + courtHouseName + " daily list entry has not been processed");
            }
        }
        dailyListEntity.setStatus(statusType.name());
    }

    private void setCaseNumber(CourtCaseEntity courtCase, List<Defendant> defendants, DailyListEntity dailyListEntity, Hearing hearing) {
        // CPP don't provide case id, use URN
        if (String.valueOf(SourceType.CPP).equalsIgnoreCase(dailyListEntity.getSource())) {
            if (defendants.isEmpty()) {
                courtCase.setCaseNumber(hearing.getCaseNumber());
            } else {
                String urn = defendants.get(0).getUrn();
                if (StringUtils.isEmpty(urn)) {
                    log.error("Hearing not added - HearingInfo does not contain a URN value");
                } else {
                    courtCase.setCaseNumber(urn);
                }
            }
        } else {
            courtCase.setCaseNumber(hearing.getCaseNumber());
        }

    }

    private void setCourtroom(HearingEntity hearing, CourtCaseEntity courtCase, Integer courtRoomNumber) {
        Optional<CourtroomEntity> courtroomEntity = courtCase.getCourthouse().getCourtrooms().stream().filter(
                c -> c.getName().equals(String.valueOf(courtRoomNumber))).findFirst();
        courtroomEntity.ifPresent(hearing::setCourtroom);

    }

    private void addProsecutions(CourtCaseEntity courtCase, String prosecutionName) {
        courtCase.addProsecutor(retrieveCoreObjectService.retrieveOrCreateProsecutor(prosecutionName, courtCase));
    }


    private void addDefenders(CourtCaseEntity courtCase, List<Defendant> defendants) {
        for (Defendant defendant : defendants) {
            String fullName = buildFullName(defendant.getCounsel().getAdvocate().getPersonalDetails().getName());
            courtCase.addDefence(retrieveCoreObjectService.retrieveOrCreateDefence(fullName, courtCase));
        }
    }

    private void addDefendants(CourtCaseEntity courtCase, List<Defendant> defendants) {
        for (Defendant defendant : defendants) {
            courtCase.addDefendant(retrieveCoreObjectService.retrieveOrCreateDefendant(
                    buildFullName(defendant.getPersonalDetails().getName()), courtCase));
        }

    }

    private void addJudges(Sitting sitting, HearingEntity courtCase) {
        for (CitizenName judge : sitting.getJudiciary()) {
            JudgeEntity judgeEntity = retrieveCoreObjectService.retrieveOrCreateJudge(buildFullName(judge));
            courtCase.addJudge(judgeEntity);
        }
    }

    private String buildFullName(CitizenName citizenName) {
        return citizenName.getCitizenNameForename() + " " + citizenName.getCitizenNameSurname();
    }
}
