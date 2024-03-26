package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.DefenceRepository;
import uk.gov.hmcts.darts.common.repository.DefendantRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.JudgeRepository;
import uk.gov.hmcts.darts.common.repository.ProsecutorRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class RetrieveCoreObjectServiceImpl implements RetrieveCoreObjectService {
    private final HearingRepository hearingRepository;
    private final CaseRepository caseRepository;
    private final JudgeRepository judgeRepository;
    private final CourthouseRepository courthouseRepository;
    private final CourtroomRepository courtroomRepository;
    private final DefenceRepository defenceRepository;
    private final DefendantRepository defendantRepository;
    private final ProsecutorRepository prosecutorRepository;
    private final AuthorisationApi authorisationApi;
    private final CurrentTimeHelper currentTimeHelper;

    @Override
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDate hearingDate) {
        UserAccountEntity userAccount = authorisationApi.getCurrentUser();
        return retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount);
    }

    @Override
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber,
                                                 LocalDate hearingDate, UserAccountEntity userAccount) {
        Optional<HearingEntity> foundHearing = hearingRepository.findHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate
        );

        return foundHearing.map(hearingEntity -> setHearingLastDateModifiedBy(hearingEntity, userAccount)).orElseGet(()  -> createHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate,
            userAccount
        ));
    }

    private HearingEntity createHearing(String courthouseName, String courtroomName, String caseNumber, LocalDate hearingDate, UserAccountEntity userAccount) {
        final CourtCaseEntity courtCase = retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
        final CourtroomEntity courtroom = retrieveOrCreateCourtroom(courtCase.getCourthouse(), courtroomName);
        HearingEntity hearing = new HearingEntity();

        hearing.setCourtCase(courtCase);
        hearing.setCourtroom(courtroom);
        hearing.setHearingDate(hearingDate);
        hearing.setNew(true);
        hearing.setHearingIsActual(false);
        hearing.setCreatedBy(userAccount);
        hearing.setLastModifiedBy(userAccount);
        hearing.setCreatedDateTime(OffsetDateTime.now());
        hearing.setLastModifiedDateTime(OffsetDateTime.now());

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
        UserAccountEntity userAccount = authorisationApi.getCurrentUser();
        return retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
    }

    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber, UserAccountEntity userAccount) {
        Optional<CourtCaseEntity> foundCase = caseRepository.findByCaseNumberIgnoreCaseAndCourthouse_CourthouseNameIgnoreCase(
            caseNumber,
            courthouseName
        );

        return foundCase.map(entity -> setCourtCaseLastDateModifiedBy(entity, userAccount))
            .orElseGet(() -> createCase(courthouseName, caseNumber, userAccount));

    }

    private CourtCaseEntity createCase(String courthouseName, String caseNumber, UserAccountEntity userAccount) {

        final OffsetDateTime now = OffsetDateTime.now();

        CourthouseEntity foundCourthouse = retrieveCourthouse(courthouseName);
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber(caseNumber);
        courtCase.setCourthouse(foundCourthouse);
        courtCase.setClosed(false);
        courtCase.setInterpreterUsed(false);
        courtCase.setCreatedBy(userAccount);
        courtCase.setLastModifiedBy(userAccount);
        courtCase.setLastModifiedDateTime(now);
        courtCase.setCreatedDateTime(now);
        courtCase.setCreatedBy(userAccount);
        courtCase.setLastModifiedBy(userAccount);
        caseRepository.saveAndFlush(courtCase);
        return courtCase;
    }

    private CourtCaseEntity setCourtCaseLastDateModifiedBy(final CourtCaseEntity courtCaseEntity, final UserAccountEntity userAccountEntity) {

        courtCaseEntity.setLastModifiedDateTime(OffsetDateTime.now());
        courtCaseEntity.setLastModifiedBy(userAccountEntity);

        caseRepository.saveAndFlush(courtCaseEntity);

        return courtCaseEntity;
    }

    private HearingEntity setHearingLastDateModifiedBy(final HearingEntity hearingEntity, final UserAccountEntity userAccountEntity) {

        hearingEntity.setLastModifiedDateTime(OffsetDateTime.now());
        hearingEntity.setLastModifiedBy(userAccountEntity);

        hearingRepository.saveAndFlush(hearingEntity);

        return hearingEntity;
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
    public JudgeEntity retrieveOrCreateJudge(String judgeName) {
        UserAccountEntity userAccount = authorisationApi.getCurrentUser();
        return retrieveOrCreateJudge(judgeName, userAccount);
    }

    @Override
    public JudgeEntity retrieveOrCreateJudge(String judgeName, UserAccountEntity userAccount) {
        Optional<JudgeEntity> foundJudge = judgeRepository.findByNameIgnoreCase(judgeName);
        return foundJudge.orElseGet(() -> createJudge(judgeName, userAccount));
    }

    private JudgeEntity createJudge(String judgeName, UserAccountEntity userAccount) {
        JudgeEntity judge = new JudgeEntity();
        judge.setName(judgeName);
        judge.setCreatedBy(userAccount);
        judge.setLastModifiedBy(userAccount);
        judgeRepository.saveAndFlush(judge);
        return judge;
    }

    @Override
    public DefenceEntity createDefence(String defenceName, CourtCaseEntity courtCase, UserAccountEntity userAccount) {
        DefenceEntity defence = new DefenceEntity();
        defence.setName(defenceName);
        defence.setCourtCase(courtCase);
        defence.setCreatedBy(userAccount);
        defence.setLastModifiedBy(userAccount);
        return defenceRepository.saveAndFlush(defence);
    }

    @Override
    public DefendantEntity createDefendant(String defendantName, CourtCaseEntity courtCase, UserAccountEntity userAccount) {
        DefendantEntity defendant = new DefendantEntity();
        defendant.setName(defendantName);
        defendant.setCourtCase(courtCase);
        defendant.setCreatedBy(userAccount);
        defendant.setLastModifiedBy(userAccount);
        return defendantRepository.saveAndFlush(defendant);
    }

    @Override
    public ProsecutorEntity createProsecutor(String prosecutorName, CourtCaseEntity courtCase, UserAccountEntity userAccount) {
        ProsecutorEntity prosecutor = new ProsecutorEntity();
        prosecutor.setName(prosecutorName);
        prosecutor.setCourtCase(courtCase);
        prosecutor.setCreatedBy(userAccount);
        prosecutor.setLastModifiedBy(userAccount);
        prosecutorRepository.saveAndFlush(prosecutor);
        return prosecutor;
    }
}
