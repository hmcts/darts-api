package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.JudgeRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class RetrieveCoreObjectServiceImpl implements RetrieveCoreObjectService {
    private final HearingRepository hearingRepository;
    private final CaseRepository caseRepository;
    private final JudgeRepository judgeRepository;
    private final CourthouseRepository courthouseRepository;
    private final CourtroomRepository courtroomRepository;

    @Override
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDate hearingDate) {
        Optional<HearingEntity> foundHearing = hearingRepository.findHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate
        );
        return foundHearing.orElseGet(() -> createHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate
        ));
    }

    private HearingEntity createHearing(String courthouseName, String courtroomName, String caseNumber, LocalDate hearingDate) {
        CourtCaseEntity courtCase = retrieveOrCreateCase(courthouseName, caseNumber);
        CourtroomEntity courtroom = retrieveOrCreateCourtroom(courtCase.getCourthouse(), courtroomName);
        HearingEntity hearing = new HearingEntity();
        hearing.setCourtCase(courtCase);
        hearing.setCourtroom(courtroom);
        hearing.setHearingDate(hearingDate);
        hearing.setNew(true);
        hearingRepository.saveAndFlush(hearing);
        return hearing;
    }

    @Override
    public CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName) {
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByNameAndId(
            courthouse.getId(),
            courtroomName
        );
        return foundCourtroom.orElseGet(() -> createCourtroom(courthouse, courtroomName));
    }


    @Override
    public CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName) {
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByCourthouseNameAndCourtroomName(
            courthouseName,
            courtroomName
        );
        if (foundCourtroom.isPresent()) {
            return foundCourtroom.get();
        }

        CourthouseEntity courthouse = retrieveCourthouse(courthouseName);
        return createCourtroom(courthouse, courtroomName);
    }


    private CourtroomEntity createCourtroom(CourthouseEntity courthouse, String courtroomName) {
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(courtroomName);
        courtroomRepository.saveAndFlush(courtroom);
        return courtroom;
    }

    @Override
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber) {
        Optional<CourtCaseEntity> foundCase = caseRepository.findByCaseNumberAndCourthouse_CourthouseName(
            caseNumber,
            courthouseName
        );
        return foundCase.orElseGet(() -> createCase(courthouseName, caseNumber));
    }

    private CourtCaseEntity createCase(String courthouseName, String caseNumber) {
        CourthouseEntity foundCourthouse = retrieveCourthouse(courthouseName);
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber(caseNumber);
        courtCase.setCourthouse(foundCourthouse);
        caseRepository.saveAndFlush(courtCase);
        return courtCase;
    }

    @Override
    public CourthouseEntity retrieveCourthouse(String courthouseName) {
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseNameIgnoreCase(courthouseName);
        if (foundCourthouse.isEmpty()) {
            //Courthouses need to be created manually in the screens. throw an error.
            String message = MessageFormat.format("Courthouse ''{0}'' not found.", courthouseName);
            log.error(message);
            throw new DartsApiException(CommonApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST, message);
        }
        return foundCourthouse.get();
    }

    @Override
    public JudgeEntity retrieveJudge(String judgeName) {
        Optional<JudgeEntity> foundJudge = judgeRepository.findByNameIgnoreCase(judgeName);
        return foundJudge.orElseGet(() -> createJudge(judgeName));
    }

    private JudgeEntity createJudge(String judgeName) {
        JudgeEntity judge = new JudgeEntity();
        judge.setName(judgeName);
        judgeRepository.saveAndFlush(judge);
        return judge;
    }
}
