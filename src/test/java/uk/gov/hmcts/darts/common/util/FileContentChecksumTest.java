package uk.gov.hmcts.darts.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileContentChecksumTest {

    private final FileContentChecksum checksum = new FileContentChecksum();

    @Test
    void calculate() {
        assertEquals("CY9rzUYh03PK3k6DJie09g==", checksum.calculate("test".getBytes()));
    }

}
