package uk.gov.hmcts.darts.cases.helper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.exception.AdvancedSearchNoResultsException;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity_;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity_;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity_;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity_;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity_;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity_;
import uk.gov.hmcts.darts.common.entity.UserAccountCourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountCourtCaseEntity_;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static uk.gov.hmcts.darts.cases.service.impl.CaseServiceImpl.MAX_RESULTS;

@Component
@SuppressWarnings({"PMD.TooManyMethods"})
@RequiredArgsConstructor
@Slf4j
public class AdvancedSearchRequestHelper {
    @PersistenceContext
    private final EntityManager entityManager;

    private final UserIdentity userIdentity;
    private final CourtroomRepository courtroomRepository;
    private final CourthouseRepository courthouseRepository;

    public List<Integer> getMatchingCourtCases(GetCasesSearchRequest request) throws AdvancedSearchNoResultsException {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery(Integer.class);
        Root<UserAccountCourtCaseEntity> caseRoot = criteriaQuery.from(UserAccountCourtCaseEntity.class);

        Join<UserAccountCourtCaseEntity, CourtCaseEntity> courtCaseJoin = caseRoot.join(UserAccountCourtCaseEntity_.COURT_CASE);

        List<Predicate> predicates = new ArrayList<>();
        predicates.addAll(createCourtCasePredicates(request, criteriaBuilder, courtCaseJoin));
        predicates.addAll(createUserPredicates(criteriaBuilder, caseRoot));

        Predicate finalAndPredicate = criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        criteriaQuery.where(finalAndPredicate);
        Path<Integer> namePath = caseRoot.get(UserAccountCourtCaseEntity_.COURT_CASE).get(CourtCaseEntity_.ID);
        criteriaQuery.select(namePath).distinct(true);

        TypedQuery<Integer> query = entityManager.createQuery(criteriaQuery);
        query.setMaxResults(MAX_RESULTS + 1);
        return query.getResultList();
    }

    private List<Predicate> createUserPredicates(CriteriaBuilder criteriaBuilder, Root<UserAccountCourtCaseEntity> caseRoot) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(
            criteriaBuilder.equal(caseRoot.get(UserAccountCourtCaseEntity_.userAccount),
                                  userIdentity.getUserAccount()
            )
        );
        return predicates;
    }

    private List<Predicate> createCourtCasePredicates(GetCasesSearchRequest request,
                                                      CriteriaBuilder criteriaBuilder,
                                                      Join<UserAccountCourtCaseEntity, CourtCaseEntity> courtCaseJoin) throws AdvancedSearchNoResultsException {
        List<Predicate> predicates = new ArrayList<>();
        CollectionUtils.addAll(predicates, addCourtCaseCriteria(request, criteriaBuilder, courtCaseJoin));
        CollectionUtils.addAll(predicates, addHearingDateCriteria(request, criteriaBuilder, courtCaseJoin));
        CollectionUtils.addAll(predicates, addCourthouseCourtroomCriteria(request, criteriaBuilder, courtCaseJoin));
        CollectionUtils.addAll(predicates, addJudgeCriteria(request, criteriaBuilder, courtCaseJoin));
        CollectionUtils.addAll(predicates, addDefendantCriteria(request, criteriaBuilder, courtCaseJoin));
        CollectionUtils.addAll(predicates, addEventCriteria(request, criteriaBuilder, courtCaseJoin));

        return predicates;
    }

    private List<Predicate> addCourtCaseCriteria(GetCasesSearchRequest request,
                                                 CriteriaBuilder criteriaBuilder, Join<UserAccountCourtCaseEntity, CourtCaseEntity> courtCaseJoin) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCaseNumber())) {
            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(courtCaseJoin.get(CourtCaseEntity_.CASE_NUMBER)),
                surroundWithPercentagesUpper(request.getCaseNumber())
            ));
        }
        return predicateList;
    }

    private String surroundWithPercentagesUpper(String value) {
        return surroundValue(value.toUpperCase(Locale.ROOT), "%");
    }

    private String surroundValue(String value, String surroundWith) {
        return surroundWith + value + surroundWith;
    }

    private List<Predicate> addDefendantCriteria(GetCasesSearchRequest request,
                                                 CriteriaBuilder criteriaBuilder, Join<UserAccountCourtCaseEntity, CourtCaseEntity> courtCaseJoin) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getDefendantName())) {
            Join<CourtCaseEntity, DefendantEntity> defendantJoin = joinDefendantEntity(courtCaseJoin);

            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(defendantJoin.get(DefendantEntity_.NAME)),
                surroundWithPercentagesUpper(request.getDefendantName())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addEventCriteria(GetCasesSearchRequest request,
                                             CriteriaBuilder criteriaBuilder, Join<UserAccountCourtCaseEntity, CourtCaseEntity> courtCaseJoin) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getEventTextContains())) {
            Join<CourtCaseEntity, EventEntity> eventJoin = joinEventEntity(courtCaseJoin);

            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(eventJoin.get(EventEntity_.EVENT_TEXT)),
                surroundWithPercentagesUpper(request.getEventTextContains())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addCourthouseCourtroomCriteria(GetCasesSearchRequest request,
                                                           CriteriaBuilder criteriaBuilder,
                                                           Join<UserAccountCourtCaseEntity,
                                                               CourtCaseEntity> courtCaseJoin) throws AdvancedSearchNoResultsException {
        boolean courtroomProvided = StringUtils.isNotBlank(request.getCourtroom()) || request.getCourtroomId() != null;
        List<Predicate> predicateList = new ArrayList<>();
        if (courtroomProvided) {
            return addCourtroomIdCriteria(request, criteriaBuilder, courtCaseJoin);
        }
        boolean courthouseProvided = StringUtils.isNotBlank(request.getCourthouse()) || request.getCourthouseId() != null;
        if (courthouseProvided) {
            return addCourthouseIdCriteria(request, criteriaBuilder, courtCaseJoin);
        }

        return predicateList;
    }

    private List<Predicate> addCourtroomIdCriteria(GetCasesSearchRequest request,
                                                   CriteriaBuilder criteriaBuilder,
                                                   Join<UserAccountCourtCaseEntity, CourtCaseEntity> courtCaseJoin) throws AdvancedSearchNoResultsException {
        List<Predicate> predicateList = new ArrayList<>();
        Join<CourtCaseEntity, HearingEntity> hearingJoin = joinHearing(courtCaseJoin);
        if (request.getCourtroomId() != null) {
            CourtroomEntity courtroomReference = courtroomRepository.getReferenceById(request.getCourtroomId());
            predicateList.add(criteriaBuilder.equal(
                hearingJoin.get(HearingEntity_.COURTROOM),
                courtroomReference)
            );
            return predicateList;
        }
        if (StringUtils.isNotBlank(request.getCourthouse())) {
            List<Integer> courtroomIdList = courtroomRepository.findAllIdByCourthouseNameAndCourtroomNameLike(
                request.getCourthouse(), request.getCourtroom());
            if (courtroomIdList.isEmpty()) {
                throw new AdvancedSearchNoResultsException();
            }
            List<CourtroomEntity> courtroomEntityList = courtroomIdList.stream().map(id -> courtroomRepository.getReferenceById(id)).toList();
            predicateList.add(
                hearingJoin.get(HearingEntity_.COURTROOM).in(
                    courtroomEntityList)
            );
            return predicateList;
        }

        List<Integer> courtroomIdList = courtroomRepository.findAllIdByCourtroomNameLike(request.getCourtroom());
        if (courtroomIdList.isEmpty()) {
            throw new AdvancedSearchNoResultsException();
        }
        List<CourtroomEntity> courtroomEntityList = courtroomIdList.stream().map(id -> courtroomRepository.getReferenceById(id)).toList();
        predicateList.add(
            hearingJoin.get(HearingEntity_.COURTROOM).in(
                courtroomEntityList)
        );
        return predicateList;
    }

    private List<Predicate> addCourthouseIdCriteria(GetCasesSearchRequest request,
                                                    CriteriaBuilder criteriaBuilder,
                                                    Join<UserAccountCourtCaseEntity, CourtCaseEntity> courtCaseJoin) throws AdvancedSearchNoResultsException {
        List<Predicate> predicateList = new ArrayList<>();
        if (request.getCourthouseId() != null) {
            CourthouseEntity courthouseReference = courthouseRepository.getReferenceById(request.getCourthouseId());
            predicateList.add(criteriaBuilder.equal(
                courtCaseJoin.get(CourtCaseEntity_.COURTHOUSE),
                courthouseReference)
            );
            return predicateList;
        }
        if (StringUtils.isNotBlank(request.getCourthouse())) {
            List<Integer> courthouseIdList = courthouseRepository.findAllIdByDisplayNameOrNameLike(
                request.getCourthouse());
            log.debug("Matching list of courthouse IDs for search = {}", courthouseIdList);
            if (courthouseIdList.isEmpty()) {
                throw new AdvancedSearchNoResultsException();
            }
            List<CourthouseEntity> courthouseRefList = courthouseIdList.stream().map(id -> courthouseRepository.getReferenceById(id)).toList();
            predicateList.add(
                courtCaseJoin.get(CourtCaseEntity_.COURTHOUSE).in(
                    courthouseRefList)
            );
        }
        return predicateList;
    }

    private List<Predicate> addJudgeCriteria(GetCasesSearchRequest request,
                                             CriteriaBuilder criteriaBuilder, Join<UserAccountCourtCaseEntity, CourtCaseEntity> courtCaseJoin) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getJudgeName())) {
            Join<CourtCaseEntity, JudgeEntity> judgeJoin = joinJudge(courtCaseJoin);
            predicateList.add(criteriaBuilder.like(
                criteriaBuilder.upper(judgeJoin.get(JudgeEntity_.NAME)),
                surroundWithPercentagesUpper(request.getJudgeName())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addHearingDateCriteria(GetCasesSearchRequest request,
                                                   CriteriaBuilder criteriaBuilder, Join<UserAccountCourtCaseEntity, CourtCaseEntity> courtCaseJoin) {
        List<Predicate> predicateList = new ArrayList<>();
        if (request.getDateFrom() != null || request.getDateTo() != null) {
            Join<CourtCaseEntity, HearingEntity> hearingJoin = joinHearing(courtCaseJoin);
            if (request.getDateFrom() != null) {
                predicateList.add(criteriaBuilder.greaterThanOrEqualTo(
                    hearingJoin.get(HearingEntity_.HEARING_DATE),
                    request.getDateFrom()
                ));
            }
            if (request.getDateTo() != null) {
                predicateList.add(criteriaBuilder.lessThanOrEqualTo(
                    hearingJoin.get(HearingEntity_.HEARING_DATE),
                    request.getDateTo()
                ));
            }
        }
        return predicateList;
    }

    @SuppressWarnings("unchecked")
    private Join<CourtCaseEntity, HearingEntity> joinHearing(Join<UserAccountCourtCaseEntity, CourtCaseEntity> caseRoot) {
        Optional<Join<CourtCaseEntity, ?>> foundJoin = caseRoot.getJoins().stream().filter(join -> join.getAttribute().getName().equals(
            CourtCaseEntity_.HEARINGS)).findAny();
        return foundJoin.map(courtCaseEntityJoin -> (Join<CourtCaseEntity, HearingEntity>) courtCaseEntityJoin)
            .orElseGet(() -> caseRoot.join(CourtCaseEntity_.hearings, JoinType.INNER));
    }

    private Join<CourtCaseEntity, JudgeEntity> joinJudge(Join<UserAccountCourtCaseEntity, CourtCaseEntity> courtCaseJoin) {
        return courtCaseJoin.join(CourtCaseEntity_.JUDGES, JoinType.INNER);
    }

    @SuppressWarnings("unchecked")
    private Join<HearingEntity, CourtroomEntity> joinCourtroom(Join<UserAccountCourtCaseEntity, CourtCaseEntity> courtCaseJoin) {
        Join<CourtCaseEntity, HearingEntity> hearingJoin = joinHearing(courtCaseJoin);

        Optional<Join<HearingEntity, ?>> foundJoin = hearingJoin.getJoins().stream().filter(join -> join.getAttribute().getName().equals(
            HearingEntity_.COURTROOM)).findAny();
        return foundJoin.map(hearingEntityJoin -> (Join<HearingEntity, CourtroomEntity>) hearingEntityJoin)
            .orElseGet(() -> hearingJoin.join(HearingEntity_.COURTROOM, JoinType.INNER));

    }

    @SuppressWarnings("unchecked")
    private Join<CourtCaseEntity, CourthouseEntity> joinCourthouse(Join<UserAccountCourtCaseEntity, CourtCaseEntity> caseRoot) {
        Optional<Join<CourtCaseEntity, ?>> foundJoin = caseRoot.getJoins().stream().filter(join -> join.getAttribute().getName().equals(
            CourtroomEntity_.COURTHOUSE)).findAny();
        return foundJoin.map(join -> (Join<CourtCaseEntity, CourthouseEntity>) join)
            .orElseGet(() -> caseRoot.join(CourtroomEntity_.COURTHOUSE, JoinType.INNER));
    }

    private Join<CourtCaseEntity, DefendantEntity> joinDefendantEntity(Join<UserAccountCourtCaseEntity, CourtCaseEntity> courtCaseJoin) {
        return courtCaseJoin.join(CourtCaseEntity_.DEFENDANT_LIST, JoinType.INNER);
    }

    private Join<CourtCaseEntity, EventEntity> joinEventEntity(Join<UserAccountCourtCaseEntity, CourtCaseEntity> courtCaseJoin) {
        Join<CourtCaseEntity, HearingEntity> hearingJoin = joinHearing(courtCaseJoin);
        return hearingJoin.join(HearingEntity_.EVENT_LIST, JoinType.INNER);
    }
}
