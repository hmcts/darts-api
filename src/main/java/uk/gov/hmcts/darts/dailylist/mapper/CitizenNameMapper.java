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

    private static final int FORENAME_INDEX = 0;

    private static final int SURNAME_INDEX = 1;

    public CitizenName getCitizenName(String name) {
        String[] citizenName = DataUtil.trim(name).split(NAME_DELIMITER);
        CitizenName retCitizenName = new CitizenName();

        if (citizenName.length == 1) {
            retCitizenName.setCitizenNameForename(citizenName[FORENAME_INDEX]);
            retCitizenName.setCitizenNameSurname("");
        } else if (citizenName.length > 1) {
            retCitizenName.setCitizenNameForename(citizenName[FORENAME_INDEX]);
            retCitizenName.setCitizenNameSurname(getSurnames(citizenName));
        }
        return retCitizenName;
    }

    public String getCitizenName(CitizenName citizenName) {
        String returnName = "";
        if (citizenName.getCitizenNameForename() != null) {
            returnName =  DataUtil.trim(citizenName.getCitizenNameForename());
            if (citizenName.getCitizenNameSurname() != null && !citizenName.getCitizenNameSurname().isEmpty()) {
                returnName = returnName + NAME_DELIMITER + DataUtil.trim(citizenName.getCitizenNameSurname());
            }
        }
        return returnName;
    }

    private String getSurnames(String[] citizenNameParts) {
        String surname = "";
        surname = surname.concat(citizenNameParts[SURNAME_INDEX]);
        for (int position = 0; position < citizenNameParts.length; position++) {
            if (position > SURNAME_INDEX) {
                surname = surname.concat(NAME_DELIMITER).concat(citizenNameParts[position]);
            }
        }

        return DataUtil.trim(surname);
    }
}