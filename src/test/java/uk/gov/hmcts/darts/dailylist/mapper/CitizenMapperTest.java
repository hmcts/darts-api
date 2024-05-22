package uk.gov.hmcts.darts.dailylist.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.dailylist.model.CitizenName;

public class CitizenMapperTest {
    @Test
    public void testCreateCitizenMapperFromStringWithNoBreaks() {
        String name = "DMP-723-AC1-D001";
        CitizenNameMapper mapper = new CitizenNameMapper();
        CitizenName citizenName = mapper.getCitizenName(name);
        Assertions.assertEquals(name, citizenName.getCitizenNameForename());
        Assertions.assertEquals("", citizenName.getCitizenNameSurname());
    }

    @Test
    public void testCreateCitizenMapperFromStringWithBreaks() {
        String name = "Jow Bloggs Test";
        CitizenNameMapper mapper = new CitizenNameMapper();
        CitizenName citizenName = mapper.getCitizenName(name);
        Assertions.assertEquals("Jow", citizenName.getCitizenNameForename());
        Assertions.assertEquals("Bloggs Test", citizenName.getCitizenNameSurname());
    }

    @Test
    public void testGetStringFromCitizenMapperWithSurnameAndForename() {
        String name = "Jow Bloggs Test";
        CitizenNameMapper mapper = new CitizenNameMapper();
        CitizenName citizenName = mapper.getCitizenName(name);
        Assertions.assertEquals(name, mapper.getCitizenName(citizenName));
    }

    @Test
    public void testGetStringFromCitizenMapperWithForename() {
        String name = "DMP-723-AC1-D001";
        CitizenNameMapper mapper = new CitizenNameMapper();
        CitizenName citizenName = mapper.getCitizenName(name);
        Assertions.assertEquals(name, mapper.getCitizenName(citizenName));
    }
}