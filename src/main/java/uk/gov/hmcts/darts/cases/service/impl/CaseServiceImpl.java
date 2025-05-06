package uk.gov.hmcts.darts.cases.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.exception.AdvancedSearchNoResultsException;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.helper.AdminCasesSearchRequestHelper;
import uk.gov.hmcts.darts.cases.helper.AdvancedSearchRequestHelper;
import uk.gov.hmcts.darts.cases.mapper.AdminCasesSearchResponseMapper;
import uk.gov.hmcts.darts.cases.mapper.AdvancedSearchResponseMapper;
import uk.gov.hmcts.darts.cases.mapper.CaseTranscriptionMapper;
import uk.gov.hmcts.darts.cases.mapper.CasesAnnotationMapper;
import uk.gov.hmcts.darts.cases.mapper.CasesMapper;
import uk.gov.hmcts.darts.cases.mapper.HearingEntityToCaseHearing;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.AdminCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.AdminCasesSearchResponseItem;
import uk.gov.hmcts.darts.cases.model.AdminSingleCaseResponseItem;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.Annotation;
import uk.gov.hmcts.darts.cases.model.CaseTranscriptModel;
import uk.gov.hmcts.darts.cases.model.Event;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.cases.model.PostCaseResponse;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.model.SingleCase;
import uk.gov.hmcts.darts.cases.model.Transcript;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity_;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity_;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.util.pagination.PaginatedList;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({
    "PMD.TooManyMethods",
    "PMD.CouplingBetweenObjects"//TODO - refactor to reduce coupling when this class is next edited
})
public class CaseServiceImpl implements CaseService {

    public static final int MAX_RESULTS = 500;
    private final CasesMapper casesMapper;
    private final CasesAnnotationMapper annotationMapper;

    private final HearingRepository hearingRepository;
    private final EventRepository eventRepository;
    private final CaseRepository caseRepository;
    private final AnnotationRepository annotationRepository;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final AdvancedSearchRequestHelper advancedSearchRequestHelper;
    private final AdminCasesSearchRequestHelper adminCasesSearchRequestHelper;
    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final AuthorisationApi authorisationApi;
    private final LogApi logApi;
    private final CaseTranscriptionMapper transcriptionMapper;

    @Value("${darts.cases.admin-search.max-results}")
    private Integer adminSearchMaxResults;


    @Override
    @Transactional
    public List<ScheduledCase> getHearings(GetCasesRequest request) {
        logApi.casesRequestedByDarPc(request);

        List<HearingEntity> hearings = hearingRepository.findByCourthouseCourtroomAndDate(
            request.getCourthouse(),
            request.getCourtroom(),
            request.getDate()
        );
        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        createCourtroomIfMissing(hearings, request, currentUser);

        return casesMapper.mapToScheduledCases(hearings);
    }

    @Override
    public List<Hearing> getCaseHearings(Integer caseId) {
        Optional<CourtCaseEntity> courtCaseEntityOptional = caseRepository.findById(caseId);
        courtCaseEntityOptional.ifPresent(CourtCaseEntity::validateIsExpired);

        List<HearingEntity> hearingList = hearingRepository.findByCaseIds(List.of(caseId));

        if (hearingList.isEmpty() && courtCaseEntityOptional.isEmpty()) {
            throw new DartsApiException(CaseApiError.CASE_NOT_FOUND);
        }

        List<HearingEntity> filteredHearings = hearingList.stream()
            .filter(HearingEntity::getHearingIsActual)
            .sorted((o1, o2) -> o2.getHearingDate().compareTo(o1.getHearingDate()))
            .toList();

        return HearingEntityToCaseHearing.mapToHearingList(filteredHearings, transcriptionDocumentRepository);

    }

    @Override
    @Transactional
    public SingleCase getCasesById(Integer caseId) {
        CourtCaseEntity caseEntity = getCourtCaseById(caseId);

        if (caseEntity.getHearings().stream().noneMatch(HearingEntity::getHearingIsActual)) {
            throw new DartsApiException(CaseApiError.HEARINGS_NOT_ACTUAL);
        }
        return casesMapper.mapToSingleCase(caseEntity);
    }

    @Override
    public CourtCaseEntity getCourtCaseById(Integer caseId) {
        return caseRepository.findById(caseId)
            .orElseThrow(() -> new DartsApiException(CaseApiError.CASE_NOT_FOUND));
    }

    private void createCourtroomIfMissing(List<HearingEntity> hearings, GetCasesRequest request, UserAccountEntity userAccount) {
        if (CollectionUtils.isEmpty(hearings)) {
            //find out if courthouse or courtroom are missing.
            retrieveCoreObjectService.retrieveOrCreateCourtroom(request.getCourthouse(), request.getCourtroom(), userAccount);
        }
    }

    @Transactional
    @Override
    public PostCaseResponse addCaseOrUpdate(AddCaseRequest addCaseRequest) {
        CourtCaseEntity courtCase = retrieveCoreObjectService.retrieveOrCreateCase(
            addCaseRequest.getCourthouse(),
            addCaseRequest.getCaseNumber()
        );
        return updateCase(addCaseRequest, courtCase);
    }

    private PostCaseResponse updateCase(AddCaseRequest addCaseRequest, CourtCaseEntity existingCase) {
        CourtCaseEntity updatedCaseEntity = casesMapper.addDefendantProsecutorDefenderJudgeType(
            existingCase,
            addCaseRequest
        );
        caseRepository.saveAndFlush(updatedCaseEntity);
        return casesMapper.mapToPostCaseResponse(updatedCaseEntity);
    }

    @Override
    public List<AdvancedSearchResult> advancedSearch(GetCasesSearchRequest request) {
        List<Integer> caseIds;
        try {
            caseIds = advancedSearchRequestHelper.getMatchingCourtCases(request);
        } catch (AdvancedSearchNoResultsException e) {
            return new ArrayList<>();
        }
        if (caseIds.size() > MAX_RESULTS) {
            throw new DartsApiException(CaseApiError.TOO_MANY_RESULTS);
        }
        if (caseIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<HearingEntity> hearings = hearingRepository.findByIsActualCaseIds(caseIds);
        return AdvancedSearchResponseMapper.mapResponse(hearings);
    }

    @Transactional
    @Override
    public PaginatedList<Event> getEventsByCaseId(Integer caseId, PaginationDto<Event> paginationDto) {
        getCourtCaseById(caseId).validateIsExpired();

        return paginationDto.toPaginatedList(
            pageable -> eventRepository.findAllByCaseIdPaginated(caseId, pageable),
            event -> event,
            List.of(HearingEntity_.HEARING_DATE, EventEntity_.TIMESTAMP),
            List.of(Sort.Direction.DESC, Sort.Direction.DESC),
            Map.of("eventId", "ee.id",
                   "hearingDate", "he.hearingDate",
                   "timestamp", "ee.timestamp",
                   "eventName", "et.eventName",
                   "courtroom", "ee.courtroom",
                   "text", "ee.eventText")
        );
    }

    @Override
    @Transactional
    public List<Transcript> getTranscriptsByCaseId(Integer caseId) {
        getCourtCaseById(caseId).validateIsExpired();
        List<TranscriptionEntity> transcriptionEntities = transcriptionRepository.findByCaseIdManualOrLegacy(caseId, false);
        List<CaseTranscriptModel> caseTranscriptModelList = transcriptionMapper.mapResponse(transcriptionEntities);
        caseTranscriptModelList.sort((o1, o2) -> o2.getRequestedOn().compareTo(o1.getRequestedOn()));
        return transcriptionMapper.getTranscriptList(caseTranscriptModelList);
    }

    @Override
    public List<Annotation> getAnnotations(Integer caseId) {
        List<Annotation> annotations = new ArrayList<>();
        CourtCaseEntity courtCaseEntity = getCourtCaseById(caseId).validateIsExpired();
        List<HearingEntity> hearingEntities = courtCaseEntity.getHearings();

        if (authorisationApi.userHasOneOfRoles(List.of(SecurityRoleEnum.SUPER_ADMIN))) {
            List<AnnotationEntity> annotationsEntities =
                annotationRepository.findByListOfHearingIds(
                    hearingEntities
                        .stream()
                        .map(HearingEntity::getId)
                        .toList());

            for (AnnotationEntity annotationEntity : annotationsEntities) {
                for (HearingEntity hearingEntity : annotationEntity.getHearings()) {
                    annotations.add(annotationMapper.map(hearingEntity, annotationEntity));
                }
            }
        } else {
            List<AnnotationEntity> annotationsEntities =
                annotationRepository.findByListOfHearingIdsAndUser(
                    hearingEntities
                        .stream()
                        .map(HearingEntity::getId)
                        .toList(), authorisationApi.getCurrentUser());

            for (AnnotationEntity annotationEntity : annotationsEntities) {
                for (HearingEntity hearingEntity : annotationEntity.getHearings()) {
                    annotations.add(annotationMapper.map(hearingEntity, annotationEntity));
                }
            }
        }
        return annotations;
    }

    @Override
    @Transactional
    public List<AdminCasesSearchResponseItem> adminCaseSearch(AdminCasesSearchRequest request) {
        List<Integer> matchingCaseIds = adminCasesSearchRequestHelper.getMatchingCaseIds(request);
        if (matchingCaseIds.size() > adminSearchMaxResults) {
            throw new DartsApiException(CaseApiError.TOO_MANY_RESULTS);
        }
        List<CourtCaseEntity> matchingCases = caseRepository.findAllWithIdMatchingOneOf(matchingCaseIds);

        return AdminCasesSearchResponseMapper.mapResponse(matchingCases);
    }

    @Override
    public CourtCaseEntity saveCase(CourtCaseEntity courtCase) {
        return caseRepository.saveAndFlush(courtCase);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSingleCaseResponseItem adminGetCaseById(Integer caseId) {
        CourtCaseEntity caseEntity = getCourtCaseById(caseId);
        return casesMapper.mapToAdminSingleCaseResponseItem(caseEntity);
    }
}