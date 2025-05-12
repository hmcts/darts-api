package uk.gov.hmcts.darts.cases.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.model.AdminCaseAudioResponseItem;
import uk.gov.hmcts.darts.cases.service.AdminCaseService;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.util.pagination.PaginatedList;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCaseServiceImpl implements AdminCaseService {

    private final CaseService caseService;
    private final MediaRepository mediaRepository;

    @Transactional
    @Override
    public PaginatedList<AdminCaseAudioResponseItem> getAudiosByCaseId(Integer caseId,
                                                                       PaginationDto<AdminCaseAudioResponseItem> paginationDto) {
        caseService.getCourtCaseById(caseId).validateIsExpired();
        return paginationDto.toPaginatedList(
            pageable -> mediaRepository.findByCaseIdAndIsCurrentTruePageable(caseId, pageable),
            t -> t,
            List.of("startTime"),
            List.of(Sort.Direction.DESC),
            Map.of("audioId", "med.id",
                   "courtroom", "med.courtroom.name",
                   "startTime", "med.start",
                   "endTime", "med.end",
                   "channel", "med.channel")
        );
    }

}
