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

    public CitizenName getCitizenName(String name) {
        String[] citizenName = name.split(NAME_DELIMITER);
        CitizenName retCitizenName = new CitizenName();

        if (citizenName.length > 0) {
            retCitizenName.setCitizenNameForename(citizenName[0]);
            retCitizenName.setCitizenNameSurname(citizenName[1]);
        }

        retCitizenName.setCitizenNameForename(citizenName[0]);
        return retCitizenName;
    }

    public String getCitizenName(CitizenName citizenName) {
        return citizenName.getCitizenNameForename() + NAME_DELIMITER + citizenName.getCitizenNameSurname();
    }
}