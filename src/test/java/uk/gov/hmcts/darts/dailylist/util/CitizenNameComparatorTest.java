package uk.gov.hmcts.darts.dailylist.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.dailylist.mapper.CitizenNameMapper;

class CitizenNameComparatorTest {
    @Test
    void testComparatorTrueWithForenameAndSurname() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        Assertions.assertEquals(0, new CitizenNameComparator(mapper)
            .compare(mapper.getCitizenName("Joe Bloggs"), mapper.getCitizenName("Joe Bloggs")));
    }

    @Test
    void testComparatorTrueWithForenameAndSurnames() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        Assertions.assertEquals(0, new CitizenNameComparator(mapper)
            .compare(mapper.getCitizenName("Joe Bloggs Test"), mapper.getCitizenName("Joe Bloggs Test")));
    }

    @Test
    void testComparatorTrueOnlyWithForename() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        Assertions.assertEquals(0, new CitizenNameComparator(mapper)
            .compare(mapper.getCitizenName("Joe"), mapper.getCitizenName("Joe")));
    }

    @Test
    void testComparatorFalseForenameNotSame() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        Assertions.assertNotEquals(0, new CitizenNameComparator(mapper)
            .compare(mapper.getCitizenName("Joe Bloggs"), mapper.getCitizenName("Joe1 Bloggs")));
    }

    @Test
    void testComparatorFalseSurnamesNotSame() {
        CitizenNameMapper mapper = new CitizenNameMapper();
        Assertions.assertNotEquals(0, new CitizenNameComparator(mapper)
            .compare(mapper.getCitizenName("Joe Bloggs"), mapper.getCitizenName("Joe1 Bloggs Test")));
    }
}