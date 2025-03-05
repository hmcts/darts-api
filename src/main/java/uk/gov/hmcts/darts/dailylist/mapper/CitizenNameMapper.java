package uk.gov.hmcts.darts.dailylist.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.dailylist.model.CitizenName;
import uk.gov.hmcts.darts.util.DataUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CitizenNameMapper {

    private static final String NAME_DELIMITER = " ";

    public String getCitizenName(CitizenName citizenName) {
        List<String> nameSegments = new ArrayList<>();
        nameSegments.add(citizenName.getCitizenNameForename());
        nameSegments.add(citizenName.getCitizenNameSurname());

        return DataUtil.trim(
            nameSegments.stream()
                .map(s -> DataUtil.trim(s))
                .filter(s -> StringUtils.isNotBlank(s))
                .collect(Collectors.joining(NAME_DELIMITER)));
    }
}