package uk.gov.hmcts.darts.dailylist.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.dailylist.model.CitizenName;

class CitizenMapperTest {
    @Test
    void testCreateCitizenMapperFromStringWithNoBreaks() {
        String name = "DMP-723-AC1-D001";
        CitizenNameMapper mapper = new CitizenNameMapper();
        CitizenName citizenName = mapper.getCitizenName(name);
        Assertions.assertEquals(name, citizenName.getCitizenNameForename());
        Assertions.assertEquals("", citizenName.getCitizenNameSurname());
    }

    @Test
    void testCreateCitizenMapperFromStringWithBreaks() {
        String name = " Jow Bloggs Test ";
        CitizenNameMapper mapper = new CitizenNameMapper();
        CitizenName citizenName = mapper.getCitizenName(name);
        Assertions.assertEquals("Jow", citizenName.getCitizenNameForename());
        Assertions.assertEquals("Bloggs Test", citizenName.getCitizenNameSurname());
    }

    @Test
    void testGetStringFromCitizenMapperWithSurnameAndForename() {
        String name = " Jow Bloggs Test ";
        CitizenNameMapper mapper = new CitizenNameMapper();
        CitizenName citizenName = mapper.getCitizenName(name);
        Assertions.assertEquals("Jow Bloggs Test", mapper.getCitizenName(citizenName));
    }

    @Test
    void testGetStringFromCitizenMapperWithForename() {
        String name = " DMP-723-AC1-D001 ";
        CitizenNameMapper mapper = new CitizenNameMapper();
        CitizenName citizenName = mapper.getCitizenName(name);
        Assertions.assertEquals("DMP-723-AC1-D001", mapper.getCitizenName(citizenName));
    }

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