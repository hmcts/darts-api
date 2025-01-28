package uk.gov.hmcts.darts.dailylist.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.dailylist.model.CitizenName;
import uk.gov.hmcts.darts.util.DataUtil;

@Component
@RequiredArgsConstructor
@Slf4j
public class CitizenNameMapper {

    private static final String NAME_DELIMITER = " ";

    public String getCitizenName(CitizenName citizenName) {
        String returnName = "";
        if (citizenName.getCitizenNameForename() != null) {
            returnName = DataUtil.trim(citizenName.getCitizenNameForename());
            if (citizenName.getCitizenNameSurname() != null && !citizenName.getCitizenNameSurname().isEmpty()) {
                returnName = returnName + NAME_DELIMITER + DataUtil.trim(citizenName.getCitizenNameSurname());
            }
        }
        return DataUtil.trim(returnName);
    }
}