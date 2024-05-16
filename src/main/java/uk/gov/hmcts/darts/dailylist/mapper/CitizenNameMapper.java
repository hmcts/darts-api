package uk.gov.hmcts.darts.dailylist.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.dailylist.model.CitizenName;

@Component
@RequiredArgsConstructor
@Slf4j
public class CitizenNameMapper {

    private static final String NAME_DELIMITER = " ";

    private static final int FORENAME_INDEX = 0;

    private static final int SURNAME_INDEX = 1;

    public CitizenName getCitizenName(String name) {
        String[] citizenName = name.split(NAME_DELIMITER);
        CitizenName retCitizenName = new CitizenName();

        if (citizenName.length == 2) {
            retCitizenName.setCitizenNameForename(citizenName[FORENAME_INDEX]);
            retCitizenName.setCitizenNameSurname(citizenName[SURNAME_INDEX]);
        } else if (citizenName.length == 1) {
            retCitizenName.setCitizenNameForename(citizenName[FORENAME_INDEX]);
        }

        return retCitizenName;
    }

    public String getCitizenName(CitizenName citizenName) {
        return citizenName.getCitizenNameForename() + NAME_DELIMITER + citizenName.getCitizenNameSurname();
    }
}