package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.JudgeRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Locale;
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
    private final AuthorisationApi authorisationApi;

    @Override
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate) {
        UserAccountEntity userAccount = authorisationApi.getCurrentUser();
        return retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount);
    }

    @Override
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber,
                                                 LocalDateTime hearingDate, UserAccountEntity userAccount) {
        Optional<HearingEntity> foundHearing = hearingRepository.findHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate.toLocalDate()
        );

        return foundHearing.map(hearingEntity -> setHearingLastDateModifiedBy(hearingEntity, userAccount)).orElseGet(() -> createHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate,
            userAccount
        ));
    }

    @Override
    public HearingEntity retrieveOrCreateHearingWithMedia(String courthouseName, String courtroomName, String caseNumber,
                                                          LocalDateTime hearingDate, UserAccountEntity userAccount, MediaEntity mediaEntity) {
        Optional<HearingEntity> foundHearing = hearingRepository.findHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate.toLocalDate()
        );

        return foundHearing.map(hearingEntity -> setHearingLastDateModifiedBy(hearingEntity, userAccount)).orElseGet(() -> createHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate,
            userAccount,
            mediaEntity
        ));
    }

    private HearingEntity createHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                        UserAccountEntity userAccount) {
        return createHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount, null);
    }

    private HearingEntity createHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                        UserAccountEntity userAccount, MediaEntity mediaEntity) {
        final CourtCaseEntity courtCase = retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
        final CourtroomEntity courtroom = retrieveOrCreateCourtroom(courtCase.getCourthouse(), courtroomName, userAccount);
        HearingEntity hearing = new HearingEntity();

        hearing.setCourtCase(courtCase);
        hearing.setCourtroom(courtroom);
        hearing.setHearingDate(hearingDate.toLocalDate());
        hearing.setScheduledStartTime(hearingDate.toLocalTime().withNano(0));
        hearing.setNew(true);
        hearing.setHearingIsActual(false);
        hearing.setCreatedBy(userAccount);
        hearing.setLastModifiedBy(userAccount);
        hearing.getMediaList().add(mediaEntity);
        hearingRepository.saveAndFlush(hearing);

        return hearing;
    }

    @Override
    public CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount) {
        courtroomName = courtroomName.toUpperCase(Locale.ROOT);
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByNameAndId(
            courthouse.getId(),
            courtroomName
        );
        if (foundCourtroom.isPresent()) {
            return foundCourtroom.get();
        } else {
            return createCourtroom(courthouse, courtroomName, userAccount);
        }
    }


    @Override
    public CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName, UserAccountEntity userAccount) {
        courtroomName = courtroomName.toUpperCase(Locale.ROOT);
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByCourthouseNameAndCourtroomName(
            courthouseName,
            courtroomName
        );
        if (foundCourtroom.isPresent()) {
            return foundCourtroom.get();
        }

        CourthouseEntity courthouse = retrieveCourthouse(courthouseName);
        return createCourtroom(courthouse, courtroomName, userAccount);
    }


    private CourtroomEntity createCourtroom(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount) {
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(courtroomName);
        courtroom.setCreatedBy(userAccount);
        courtroomRepository.saveAndFlush(courtroom);
        return courtroom;
    }

    @Override
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber) {
        UserAccountEntity userAccount = authorisationApi.getCurrentUser();
        return retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
    }

    @Override
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber, UserAccountEntity userAccount) {
        Optional<CourtCaseEntity> foundCase = caseRepository.findByCaseNumberAndCourthouse_CourthouseNameIgnoreCase(
            caseNumber,
            courthouseName
        );

        return foundCase.map(entity -> setCourtCaseLastDateModifiedBy(entity, userAccount))
            .orElseGet(() -> createCase(courthouseName, caseNumber, userAccount));

    }

    @Override
    public CourtCaseEntity retrieveOrCreateCase(CourthouseEntity courthouse, String caseNumber, UserAccountEntity userAccount) {
        Optional<CourtCaseEntity> foundCase = caseRepository.findByCaseNumberAndCourthouse(
            caseNumber,
            courthouse
        );

        return foundCase.map(entity -> setCourtCaseLastDateModifiedBy(entity, userAccount))
            .orElseGet(() -> createCase(courthouse, caseNumber, userAccount));

    }

    private CourtCaseEntity createCase(String courthouseName, String caseNumber, UserAccountEntity userAccount) {
        CourthouseEntity foundCourthouse = retrieveCourthouse(courthouseName);
        return createCase(foundCourthouse, caseNumber, userAccount);
    }

    private CourtCaseEntity createCase(CourthouseEntity courthouse, String caseNumber, UserAccountEntity userAccount) {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber(caseNumber);
        courtCase.setCourthouse(courthouse);
        courtCase.setClosed(false);
        courtCase.setInterpreterUsed(false);
        courtCase.setCreatedBy(userAccount);
        courtCase.setLastModifiedBy(userAccount);
        caseRepository.saveAndFlush(courtCase);
        return courtCase;
    }

    private CourtCaseEntity setCourtCaseLastDateModifiedBy(final CourtCaseEntity courtCaseEntity, final UserAccountEntity userAccountEntity) {

        courtCaseEntity.setLastModifiedBy(userAccountEntity);

        caseRepository.saveAndFlush(courtCaseEntity);

        return courtCaseEntity;
    }

    private HearingEntity setHearingLastDateModifiedBy(final HearingEntity hearingEntity, final UserAccountEntity userAccountEntity) {

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
        String upperCaseJudgeName = judgeName != null ? judgeName.toUpperCase(Locale.ROOT) : null;
        judge.setName(upperCaseJudgeName);
        judge.setCreatedBy(userAccount);
        judge.setLastModifiedBy(userAccount);
        judgeRepository.saveAndFlush(judge);
        return judge;
    }

}