package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.service.CourthouseCommonService;

import java.text.MessageFormat;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CourthouseCommonServiceImpl implements CourthouseCommonService {

    private final CourthouseRepository courthouseRepository;

    @Override
    public CourthouseEntity retrieveCourthouse(String courthouseName) {
        String courthouseNameUpperTrimmed = StringUtils.toRootUpperCase(StringUtils.trimToEmpty(courthouseName));
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseName(courthouseNameUpperTrimmed);
        if (foundCourthouse.isEmpty()) {
            String message = MessageFormat.format("Courthouse ''{0}'' not found.", courthouseNameUpperTrimmed);
            throw new DartsApiException(CommonApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST, message);
        }
        return foundCourthouse.get();
    }
}