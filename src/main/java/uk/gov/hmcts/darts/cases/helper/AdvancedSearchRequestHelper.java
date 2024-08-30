package uk.gov.hmcts.darts.cases.helper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
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
import org.hibernate.Session;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.exception.AdvancedSearchNoResultsException;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity_;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity_;
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
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.darts.cases.service.impl.CaseServiceImpl.MAX_RESULTS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;

@Component
@SuppressWarnings({"PMD.TooManyMethods"})
@RequiredArgsConstructor
@Slf4j
public class AdvancedSearchRequestHelper {
    @PersistenceContext
    private final EntityManager entityManager;

    private final AuthorisationApi authorisationApi;
    private final CourtroomRepository courtroomRepository;
    private final CourthouseRepository courthouseRepository;

    public List<Integer> getMatchingCourtCases(GetCasesSearchRequest request) throws AdvancedSearchNoResultsException {
        HibernateCriteriaBuilder criteriaBuilder = entityManager.unwrap(Session.class).getCriteriaBuilder();
        CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery(Integer.class);
        Root<CourtCaseEntity> caseRoot = criteriaQuery.from(CourtCaseEntity.class);
        List<Predicate> predicates = createPredicates(request, criteriaBuilder, caseRoot);

        Predicate finalAndPredicate = criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        criteriaQuery.where(finalAndPredicate);
        Path<Integer> namePath = caseRoot.get(CourtCaseEntity_.ID);
        criteriaQuery.select(namePath).distinct(true);

        TypedQuery<Integer> query = entityManager.createQuery(criteriaQuery);
        query.setMaxResults(MAX_RESULTS + 1);
        return query.getResultList();
    }

    private List<Predicate> createPredicates(GetCasesSearchRequest request,
                                             HibernateCriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) throws AdvancedSearchNoResultsException {
        List<Predicate> predicates = new ArrayList<>();
        CollectionUtils.addAll(predicates, addCourthouseAccessCriteria(caseRoot));
        CollectionUtils.addAll(predicates, addCaseCriteria(request, criteriaBuilder, caseRoot));
        CollectionUtils.addAll(predicates, addHearingDateCriteria(request, criteriaBuilder, caseRoot));
        CollectionUtils.addAll(predicates, addCourthouseCourtroomCriteria(request, criteriaBuilder, caseRoot));
        CollectionUtils.addAll(predicates, addJudgeCriteria(request, criteriaBuilder, caseRoot));
        CollectionUtils.addAll(predicates, addDefendantCriteria(request, criteriaBuilder, caseRoot));
        CollectionUtils.addAll(predicates, addEventCriteria(request, criteriaBuilder, caseRoot));
        CollectionUtils.addAll(predicates, addEUseInterpreterCriteria(criteriaBuilder, caseRoot));

        return predicates;
    }

    private Predicate addCourthouseAccessCriteria(Root<CourtCaseEntity> caseRoot) {

        //add courthouse permissions
        List<Integer> courthouseIdsUserHasAccessTo = authorisationApi.getListOfCourthouseIdsUserHasAccessTo();
        Join<CourtCaseEntity, CourthouseEntity> courthouseJoin = joinCourthouse(caseRoot);
        return courthouseJoin.get(CourthouseEntity_.ID).in(courthouseIdsUserHasAccessTo);
    }

    private List<Predicate> addCaseCriteria(GetCasesSearchRequest request, HibernateCriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCaseNumber())) {
            predicateList.add(criteriaBuilder.ilike(
                caseRoot.get(CourtCaseEntity_.CASE_NUMBER),
                surroundWithPercentages(request.getCaseNumber())
            ));
        }
        return predicateList;
    }

    private String surroundWithPercentages(String value) {
        return surroundValue(value, "%");
    }

    private String surroundValue(String value, String surroundWith) {
        return surroundWith + value + surroundWith;
    }

    private List<Predicate> addDefendantCriteria(GetCasesSearchRequest request, HibernateCriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getDefendantName())) {
            Join<CourtCaseEntity, DefendantEntity> defendantJoin = joinDefendantEntity(caseRoot);

            predicateList.add(criteriaBuilder.ilike(
                defendantJoin.get(DefendantEntity_.NAME),
                surroundWithPercentages(request.getDefendantName())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addEventCriteria(GetCasesSearchRequest request, HibernateCriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getEventTextContains())) {
            Join<CourtCaseEntity, EventEntity> eventJoin = joinEventEntity(caseRoot);

            predicateList.add(criteriaBuilder.ilike(
                eventJoin.get(EventEntity_.EVENT_TEXT),
                surroundWithPercentages(request.getEventTextContains())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addCourthouseCourtroomCriteria(GetCasesSearchRequest request,
                                                           HibernateCriteriaBuilder criteriaBuilder,
                                                           Root<CourtCaseEntity> caseRoot) throws AdvancedSearchNoResultsException {
        List<Predicate> predicateList = new ArrayList<>();

        boolean courtroomProvided = StringUtils.isNotBlank(request.getCourtroom()) || request.getCourtroomId() != null;
        if (courtroomProvided) {
            return addCourtroomIdCriteria(request, criteriaBuilder, caseRoot);
        }
        boolean courthouseProvided = StringUtils.isNotBlank(request.getCourthouse()) || request.getCourthouseId() != null;
        if (courthouseProvided) {
            return addCourthouseIdCriteria(request, criteriaBuilder, caseRoot);
        }

        return predicateList;
    }

    private List<Predicate> addCourtroomIdCriteria(GetCasesSearchRequest request,
                                                   HibernateCriteriaBuilder criteriaBuilder,
                                                   Root<CourtCaseEntity> caseRoot) throws AdvancedSearchNoResultsException {
        List<Predicate> predicateList = new ArrayList<>();
        Join<CourtCaseEntity, HearingEntity> hearingJoin = joinHearing(caseRoot);
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
                                                    HibernateCriteriaBuilder criteriaBuilder,
                                                    Root<CourtCaseEntity> caseRoot) throws AdvancedSearchNoResultsException {
        List<Predicate> predicateList = new ArrayList<>();
        if (request.getCourthouseId() != null) {
            CourthouseEntity courthouseReference = courthouseRepository.getReferenceById(request.getCourthouseId());
            predicateList.add(criteriaBuilder.equal(
                caseRoot.get(CourtCaseEntity_.COURTHOUSE),
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
                caseRoot.get(CourtCaseEntity_.COURTHOUSE).in(
                    courthouseRefList)
            );
        }
        return predicateList;
    }

    private List<Predicate> addJudgeCriteria(GetCasesSearchRequest request, HibernateCriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getJudgeName())) {
            Join<CourtCaseEntity, JudgeEntity> judgeJoin = joinJudge(caseRoot);
            predicateList.add(criteriaBuilder.ilike(
                judgeJoin.get(JudgeEntity_.NAME),
                surroundWithPercentages(request.getJudgeName())
            ));
        }
        return predicateList;
    }

    private List<Predicate> addHearingDateCriteria(GetCasesSearchRequest request, HibernateCriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        if (request.getDateFrom() != null || request.getDateTo() != null) {
            Join<CourtCaseEntity, HearingEntity> hearingJoin = joinHearing(caseRoot);
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
            predicateList.add(criteriaBuilder.isTrue(hearingJoin.get(HearingEntity_.HEARING_IS_ACTUAL)));
        }
        return predicateList;
    }

    private List<Predicate> addEUseInterpreterCriteria(HibernateCriteriaBuilder criteriaBuilder, Root<CourtCaseEntity> caseRoot) {
        List<Predicate> predicateList = new ArrayList<>();
        if (authorisationApi.userHasOneOfRoles(List.of(TRANSLATION_QA))) {
            predicateList.add(criteriaBuilder.isTrue(
                caseRoot.get(CourtCaseEntity_.interpreterUsed)
            ));
        }
        return predicateList;
    }

    @SuppressWarnings({"unchecked", "PMD.LiteralsFirstInComparisons"})
    private Join<CourtCaseEntity, HearingEntity> joinHearing(Root<CourtCaseEntity> caseRoot) {
        Optional<Join<CourtCaseEntity, ?>> foundJoin = caseRoot.getJoins().stream().filter(join -> join.getAttribute().getName().equals(
            CourtCaseEntity_.HEARINGS)).findAny();
        return foundJoin.map(courtCaseEntityJoin -> (Join<CourtCaseEntity, HearingEntity>) courtCaseEntityJoin)
            .orElseGet(() -> caseRoot.join(CourtCaseEntity_.hearings, JoinType.INNER));
    }

    private Join<CourtCaseEntity, JudgeEntity> joinJudge(Root<CourtCaseEntity> caseRoot) {
        return caseRoot.join(CourtCaseEntity_.JUDGES, JoinType.INNER);
    }

    @SuppressWarnings("unchecked")
    private Join<CourtCaseEntity, CourthouseEntity> joinCourthouse(Root<CourtCaseEntity> caseRoot) {
        Optional<Join<CourtCaseEntity, ?>> foundJoin = caseRoot.getJoins().stream().filter(join -> join.getAttribute().getName().equals(
            CourtroomEntity_.COURTHOUSE)).findAny();
        return foundJoin.map(join -> (Join<CourtCaseEntity, CourthouseEntity>) join)
            .orElseGet(() -> caseRoot.join(CourtroomEntity_.COURTHOUSE, JoinType.INNER));
    }

    private Join<CourtCaseEntity, DefendantEntity> joinDefendantEntity(Root<CourtCaseEntity> caseRoot) {
        return caseRoot.join(CourtCaseEntity_.DEFENDANT_LIST, JoinType.INNER);
    }

    private Join<CourtCaseEntity, EventEntity> joinEventEntity(Root<CourtCaseEntity> caseRoot) {
        Join<CourtCaseEntity, HearingEntity> hearingJoin = joinHearing(caseRoot);
        return hearingJoin.join(HearingEntity_.EVENT_LIST, JoinType.INNER);
    }
}
