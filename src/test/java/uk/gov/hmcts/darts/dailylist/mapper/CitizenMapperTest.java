package uk.gov.hmcts.darts.dailylist.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.dailylist.model.CitizenName;

class CitizenMapperTest {

    @Test
    void positiveGetCitizenNameFromCitizenNameObjectTypical() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        CitizenName citizenName = new CitizenName();
        citizenName.setCitizenNameForename("John");
        citizenName.setCitizenNameSurname("Smith");
        Assertions.assertEquals("John Smith", mapper.getCitizenName(citizenName));
    }

    @Test
    void positiveGetCitizenNameFromCitizenNameObjectOnlyForename() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        CitizenName citizenName = new CitizenName();
        citizenName.setCitizenNameForename("John");
        Assertions.assertEquals("John", mapper.getCitizenName(citizenName));
    }

    @Test
    void positiveGetCitizenNameFromCitizenNameObjectOnlyForenameWithSpace() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        CitizenName citizenName = new CitizenName();
        citizenName.setCitizenNameForename(" John  ");
        Assertions.assertEquals("John", mapper.getCitizenName(citizenName));
    }

    @Test
    void positiveGetCitizenNameFromCitizenNameObjectBlankSurname() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        CitizenName citizenName = new CitizenName();
        citizenName.setCitizenNameForename("John");
        citizenName.setCitizenNameSurname("");
        Assertions.assertEquals("John", mapper.getCitizenName(citizenName));
    }

    @Test
    void positiveGetCitizenNameFromCitizenNameObjectExtraSpace() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        CitizenName citizenName = new CitizenName();
        citizenName.setCitizenNameForename("  John  ");
        citizenName.setCitizenNameSurname("  Smith  ");
        Assertions.assertEquals("John Smith", mapper.getCitizenName(citizenName));
    }
}