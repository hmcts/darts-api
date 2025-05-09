package uk.gov.hmcts.darts.common.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@SuppressWarnings("PMD.TooManyMethods")//TODO - refactor to reduce methods when this class is next edited
public class RetrieveCoreObjectServiceTransactionalSupport implements RetrieveCoreObjectService {

    private final RetrieveCoreObjectServiceSupport retrieveCoreObjectServiceSupport;
    private final EntityManager entityManager;

    @Override
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate) {
        HearingEntity hearing = retrieveCoreObjectServiceSupport.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate);
        return mergeStateHearing(hearing);
    }

    @Override
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                 UserAccountEntity userAccount) {
        HearingEntity hearing = retrieveCoreObjectServiceSupport.retrieveOrCreateHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount);
        return mergeStateHearing(hearing);
    }

    @Override
    public HearingEntity retrieveOrCreateHearingWithMedia(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                          UserAccountEntity userAccount, MediaEntity mediaEntity) {
        HearingEntity hearing = retrieveCoreObjectServiceSupport.retrieveOrCreateHearingWithMedia(
            courthouseName, courtroomName, caseNumber, hearingDate, userAccount, mediaEntity);
        return mergeStateHearing(hearing);
    }

    @Override
    public CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount) {
        CourtroomEntity courtroom = retrieveCoreObjectServiceSupport.retrieveOrCreateCourtroom(courthouse, courtroomName, userAccount);
        return mergeState(courtroom);
    }

    @Override
    public CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName, UserAccountEntity userAccount) {
        CourtroomEntity courtroom = retrieveCoreObjectServiceSupport.retrieveOrCreateCourtroom(courthouseName, courtroomName, userAccount);
        return mergeState(courtroom);
    }

    @Override
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber) {
        CourtCaseEntity courtCase = retrieveCoreObjectServiceSupport.retrieveOrCreateCase(courthouseName, caseNumber);
        return mergeState(courtCase);
    }

    @Override
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber, UserAccountEntity userAccount) {
        CourtCaseEntity courtCase = retrieveCoreObjectServiceSupport.retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
        return mergeState(courtCase);
    }

    @Override
    public CourtCaseEntity retrieveOrCreateCase(CourthouseEntity courthouse, String caseNumber, UserAccountEntity userAccount) {
        CourtCaseEntity courtCase = retrieveCoreObjectServiceSupport.retrieveOrCreateCase(courthouse, caseNumber, userAccount);
        return mergeState(courtCase);
    }

    @Override
    public CourthouseEntity retrieveCourthouse(String courthouseName) {
        CourthouseEntity courthouse = retrieveCoreObjectServiceSupport.retrieveCourthouse(courthouseName);
        return mergeState(courthouse);
    }

    @Override
    public JudgeEntity retrieveOrCreateJudge(String judgeName) {
        JudgeEntity judge = retrieveCoreObjectServiceSupport.retrieveOrCreateJudge(judgeName);
        return mergeState(judge);
    }

    @Override
    public JudgeEntity retrieveOrCreateJudge(String judgeName, UserAccountEntity userAccount) {
        JudgeEntity judge = retrieveCoreObjectServiceSupport.retrieveOrCreateJudge(judgeName, userAccount);
        return mergeState(judge);
    }

    private HearingEntity mergeStateHearing(HearingEntity entity) {
        HearingEntity hearing = mergeState(entity);
        hearing.setNew(entity.isNew());
        return hearing;
    }

    private <T> T mergeState(T entity) {
        //If a transaction was present before we created a REQUIRED_NEW transaction merge the entity states
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return entityManager.merge(entity);
        }
        return entity;
    }
}