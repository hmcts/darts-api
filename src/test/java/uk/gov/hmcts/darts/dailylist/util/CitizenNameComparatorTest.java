package uk.gov.hmcts.darts.dailylist.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.dailylist.mapper.CitizenNameMapper;

public class CitizenNameComparatorTest {
    @Test
    public void testComparatorTrueWithForenameAndSurname() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        Assertions.assertEquals(0, new CitizenNameComparator(mapper)
            .compare(mapper.getCitizenName("Joe Bloggs"), mapper.getCitizenName("Joe Bloggs")));
    }

    @Test
    public void testComparatorTrueWithForenameAndSurnames() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        Assertions.assertEquals(0, new CitizenNameComparator(mapper)
            .compare(mapper.getCitizenName("Joe Bloggs Test"), mapper.getCitizenName("Joe Bloggs Test")));
    }

    @Test
    public void testComparatorTrueOnlyWithForename() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        Assertions.assertEquals(0, new CitizenNameComparator(mapper)
            .compare(mapper.getCitizenName("Joe"), mapper.getCitizenName("Joe")));
    }

    @Test
    public void testComparatorFalseForenameNotSame() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        Assertions.assertNotEquals(0, new CitizenNameComparator(mapper)
            .compare(mapper.getCitizenName("Joe Bloggs"), mapper.getCitizenName("Joe1 Bloggs")));
    }

    @Test
    public void testComparatorFalseSurnamesNotSame() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        Assertions.assertNotEquals(0, new CitizenNameComparator(mapper)
            .compare(mapper.getCitizenName("Joe Bloggs"), mapper.getCitizenName("Joe1 Bloggs Test")));
    }
}