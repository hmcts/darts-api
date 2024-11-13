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
}