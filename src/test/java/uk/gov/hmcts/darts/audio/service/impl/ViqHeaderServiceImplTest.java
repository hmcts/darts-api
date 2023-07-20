package uk.gov.hmcts.darts.audio.service.impl;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith(MockitoExtension.class)
class ViqHeaderServiceImplTest {

    @InjectMocks
    ViqHeaderServiceImpl viQheaderService;

    @Test
    void generatePlaylist() {
        assertThrows(NotImplementedException.class, () ->
            viQheaderService.generatePlaylist(null, null, null));
    }

    @Test
    void generateAnnotation() {
        assertThrows(NotImplementedException.class, () ->
            viQheaderService.generateAnnotation(null, null, null));
    }

    @Test
    void generateReadme() {
        assertThrows(NotImplementedException.class, () ->
            viQheaderService.generateReadme(null));
    }
}
