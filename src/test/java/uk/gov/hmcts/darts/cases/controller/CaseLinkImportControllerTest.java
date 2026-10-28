package uk.gov.hmcts.darts.cases.controller;

import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.mock.web.MockPart;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportService;

import static org.mockito.Mockito.verify;

class CaseLinkImportControllerTest {

    private final CaseLinkImportService service = Mockito.mock(CaseLinkImportService.class);
    private final CaseLinkImportController controller = new CaseLinkImportController(service);
    private final MockMultipartFile file = new MockMultipartFile("file", "cases.csv", "text/csv", new byte[]{1});

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void given_unresolvedServletMultipart_when_uploading_then_countFileParts() {
        var request = new MockHttpServletRequest();
        request.setContentType("multipart/form-data");
        request.addPart(new MockPart("file", "cases.csv", new byte[]{1}));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        controller.adminCaseLinkImportsPost(file);
        verify(service).importFile(file, 1);
    }

    @Test
    void given_wrappedMultipart_when_uploading_then_unwrapAndCountFiles() {
        var request = new MockMultipartHttpServletRequest();
        request.addFile(file);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new HttpServletRequestWrapper(request)));
        controller.adminCaseLinkImportsPost(file);
        verify(service).importFile(file, 1);
    }

    @Test
    void given_extraFileInDifferentField_when_uploading_then_countBothFiles() {
        var request = new MockMultipartHttpServletRequest();
        request.addFile(file);
        request.addFile(new MockMultipartFile("extra", "other.csv", "text/csv", new byte[]{1}));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        controller.adminCaseLinkImportsPost(file);
        verify(service).importFile(file, 2);
    }
}
