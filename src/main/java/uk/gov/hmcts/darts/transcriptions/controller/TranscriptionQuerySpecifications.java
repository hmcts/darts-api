package uk.gov.hmcts.darts.transcriptions.controller;

import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity_;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity_;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity_;
import uk.gov.hmcts.darts.common.entity.HearingEntity_;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity_;

import java.time.LocalDate;

import static java.util.Objects.isNull;

public class TranscriptionQuerySpecifications {

    private TranscriptionQuerySpecifications() {
    }

    public static Specification<TranscriptionEntity> hasId(Integer userId) {
        if (isNull(userId)) {
            return null;
        }
        return (root, query, builder) -> root.get(TranscriptionEntity_.id).in(userId);
    }

    public static Specification<TranscriptionEntity> isForCase(String caseNumber) {
        if (isNull(caseNumber)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            var hearingToTranscriptionJoin = root.join(TranscriptionEntity_.hearings.getName());
            var caseToHearingJoin = hearingToTranscriptionJoin.join(HearingEntity_.courtCase.getName());
            return criteriaBuilder.equal(caseToHearingJoin.get(CourtCaseEntity_.caseNumber.getName()), caseNumber);
        };
    }

    public static Specification<TranscriptionEntity> createdFrom(LocalDate requestedAtFrom) {
        if (isNull(requestedAtFrom)) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.greaterThanOrEqualTo(root.get(TranscriptionEntity_.CREATED_DATE_TIME), requestedAtFrom);
    }

    public static Specification<TranscriptionEntity> createdTo(LocalDate requestedAtTo) {
        if (isNull(requestedAtTo)) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.lessThanOrEqualTo(root.get(TranscriptionEntity_.CREATED_DATE_TIME), requestedAtTo);
    }

    public static Specification<TranscriptionEntity> partiallyMatchesOwner(String owner) {
        if (isNull(owner)) {
            return null;
        }
        //        SELECT t.*
        //        FROM transcription t
        //        INNER JOIN (
        //            SELECT tw.tra_id, MAX(tw.workflow_ts) AS latest_workflow_ts
        //        FROM transcription_workflow tw
        //        JOIN user_account ua ON tw.workflow_actor = ua.usr_id
        //        WHERE ua.user_name like '%43%'
        //        GROUP BY tw.tra_id) latest_workflow ON t.tra_id = latest_workflow.tra_id;
//        return new Specification<TranscriptionEntity>() {
//            @Override
//            public Predicate toPredicate(Root<TranscriptionEntity> tRoot, CriteriaQuery<?> mainQuery, CriteriaBuilder cb) {
//
//                Subquery<Tuple> subquery = mainQuery.subquery(Tuple.class);
//                Root<TranscriptionWorkflowEntity> workflowRoot = subquery.from(TranscriptionWorkflowEntity.class);
//                Join<TranscriptionWorkflowEntity, UserAccountEntity> userJoin = workflowRoot.join("workflowActor");
//
//                var tupleCompoundSelection = cb.tuple(
//                    workflowRoot.get("transcription").get("id").alias("tra_id"),
//                    cb.max(workflowRoot.get("workflowTimestamp")).alias("max_ts"));
//
//                subquery.select(tupleCompoundSelection)
//                    .where(cb.like(userJoin.get("userName"), "%43%"))
//                    .groupBy(workflowRoot.get("transcription").get("id"));
//
//                // Join with Transcription entity
//                Root<TranscriptionEntity> transcriptionRoot = mainQuery.from(TranscriptionEntity.class);
//                Join<TranscriptionEntity, Tuple> latestWorkflowJoin = transcriptionRoot.join("transcriptionWorkflows");
//                Predicate joinPredicate = cb.equal(transcriptionRoot.get("id"), latestWorkflowJoin.get("tra_id"));
//                return cb.equal(latestWorkflowJoin.get("workflowTimestamp"),
//                                subquery.getSelection().get("max_ts"));
//            }
//        };
        return null;

    }

    public static Specification<TranscriptionEntity> partiallyMatchesCourthouseDisplayName(String courthouseDisplayName) {
        if (isNull(courthouseDisplayName)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            var hearingToTranscriptionJoin = root.join(TranscriptionEntity_.hearings.getName());
            var courtroomToHearingJoin = hearingToTranscriptionJoin.join(HearingEntity_.courtroom.getName());
            var courthouseToCourtroomJoin = courtroomToHearingJoin.join(CourtroomEntity_.courthouse.getName());
            return criteriaBuilder.like(courthouseToCourtroomJoin.get(CourthouseEntity_.DISPLAY_NAME), "%" + courthouseDisplayName + "%");
        };
    }
}
