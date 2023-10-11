package uk.gov.hmcts.darts.cases.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.helper.AdvancedSearchRequestHelper;
import uk.gov.hmcts.darts.cases.mapper.AdvancedSearchResponseMapper;
import uk.gov.hmcts.darts.cases.mapper.CasesMapper;
import uk.gov.hmcts.darts.cases.mapper.HearingEntityToCaseHearing;
import uk.gov.hmcts.darts.cases.mapper.TranscriptionMapper;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.cases.model.PatchRequestObject;
import uk.gov.hmcts.darts.cases.model.PostCaseResponse;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.model.SingleCase;
import uk.gov.hmcts.darts.cases.model.Transcript;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.cases.util.CourtCaseUtil;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class CaseServiceImpl implements CaseService {

    private static final int MAX_RESULTS = 500;
    private final CasesMapper casesMapper;

    private final HearingRepository hearingRepository;
    private final CaseRepository caseRepository;
    private final EventRepository eventRepository;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final AdvancedSearchRequestHelper advancedSearchRequestHelper;
    private final TranscriptionRepository transcriptionRepository;

    @Override
    @Transactional
    public List<ScheduledCase> getHearings(GetCasesRequest request) {

        List<HearingEntity> hearings = hearingRepository.findByCourthouseCourtroomAndDate(
            request.getCourthouse(),
            request.getCourtroom(),
            request.getDate()
        );
        createCourtroomIfMissing(hearings, request);
        return casesMapper.mapToScheduledCases(hearings);

    }

    @Override
    public List<Hearing> getCaseHearings(Integer caseId) {

        List<HearingEntity> hearingList = hearingRepository.findByCaseIds(List.of(caseId));

        if (hearingList.isEmpty()) {
            getCourtCaseById(caseId);
        }

        return HearingEntityToCaseHearing.mapToHearingList(hearingList);

    }

    @Override
    public SingleCase getCasesById(Integer caseId) {
        CourtCaseEntity caseEntity = getCourtCaseById(caseId);
        return casesMapper.mapToSingleCase(caseEntity);
    }

    public CourtCaseEntity getCourtCaseById(Integer caseId) {
        Optional<CourtCaseEntity> caseEntity = caseRepository.findById(caseId);

        if (caseEntity.isEmpty()) {
            throw new DartsApiException(CaseApiError.CASE_NOT_FOUND);
        }
        return caseEntity.get();
    }

    private void createCourtroomIfMissing(List<HearingEntity> hearings, GetCasesRequest request) {
        if (CollectionUtils.isEmpty(hearings)) {
            //find out if courthouse or courtroom are missing.
            retrieveCoreObjectService.retrieveOrCreateCourtroom(request.getCourthouse(), request.getCourtroom());
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
        CourtCaseEntity updatedCaseEntity = casesMapper.addDefendantProsecutorDefenderJudge(
            existingCase,
            addCaseRequest
        );
        caseRepository.saveAndFlush(updatedCaseEntity);
        return casesMapper.mapToPostCaseResponse(updatedCaseEntity);
    }

    @Override
    public List<AdvancedSearchResult> advancedSearch(GetCasesSearchRequest request) {
        List<CourtCaseEntity> courtCaseEntities = advancedSearchRequestHelper.getMatchingCourtCases(request);
        if (courtCaseEntities.size() > MAX_RESULTS) {
            throw new DartsApiException(CaseApiError.TOO_MANY_RESULTS);
        }
        if (courtCaseEntities.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> caseIds = CourtCaseUtil.getCaseIdList(courtCaseEntities);
        List<HearingEntity> hearings = hearingRepository.findByCaseIds(caseIds);
        return AdvancedSearchResponseMapper.mapResponse(hearings);
    }

    @Override
    public SingleCase patchCase(Integer caseId, PatchRequestObject patchRequestObject) {
        CourtCaseEntity foundCase = getCourtCaseById(caseId);
        foundCase.setRetainUntilTimestamp(patchRequestObject.getRetainUntil());
        caseRepository.save(foundCase);
        return casesMapper.mapToSingleCase(foundCase);
    }

    @Override
    public List<Transcript> getTranscriptsById(Integer caseId) {
        List<TranscriptionEntity> transcriptionEntities = transcriptionRepository.findByCaseId(caseId);
        return TranscriptionMapper.mapResponse(transcriptionEntities);
    }
}
