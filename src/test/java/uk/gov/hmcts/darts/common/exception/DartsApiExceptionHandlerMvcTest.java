package uk.gov.hmcts.darts.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import uk.gov.hmcts.darts.audio.component.AudioRequestResponseMapper;
import uk.gov.hmcts.darts.audio.controller.AudioRequestsController;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DartsApiExceptionHandlerMvcTest {

    private static final String UPLOAD_URI = "/test/upload";
    private static final String DOWNLOAD_OPERATION = "download";

    private final MediaRequestService mediaRequestService = mock(MediaRequestService.class);
    private final UploadController uploadController = mock(UploadController.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AudioRequestsController audioController = new AudioRequestsController(mediaRequestService, mock(AudioRequestResponseMapper.class));
        mockMvc = MockMvcBuilders.standaloneSetup(audioController, uploadController)
            .setControllerAdvice(new DartsApiExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapperConfig().objectMapper()))
            .build();
    }

    @Test
    void givenMalformedMultipartUpload_whenHandled_thenReturnsBadRequest() throws Exception {
        when(uploadController.upload()).thenThrow(new MultipartException("Missing multipart boundary"));

        mockMvc.perform(post(UPLOAD_URI).contentType(MediaType.MULTIPART_FORM_DATA).content("malformed upload"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Invalid multipart request"))
            .andExpect(jsonPath("$.instance").value(UPLOAD_URI));
    }

    @Test
    void givenOversizedMultipartUpload_whenHandled_thenRetainsUploadSizeHandler() throws Exception {
        when(uploadController.upload()).thenThrow(new MaxUploadSizeExceededException(1024));

        mockMvc.perform(post(UPLOAD_URI).contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Maximum upload size exceeded"))
            .andExpect(jsonPath("$.instance").value(UPLOAD_URI));
    }

    @ParameterizedTest
    @ValueSource(strings = {DOWNLOAD_OPERATION, "playback"})
    void givenAudioResourceIoException_whenRequested_thenReturnsStructuredServerError(String operation) throws Exception {
        try (DownloadResponseMetaData metadata = mock(DownloadResponseMetaData.class)) {
            if (DOWNLOAD_OPERATION.equals(operation)) {
                when(mediaRequestService.download(1)).thenReturn(metadata);
                when(metadata.getResource()).thenThrow(new IOException("Audio resource unavailable"));
            } else {
                Resource resource = mock(Resource.class);
                when(mediaRequestService.playback(1)).thenReturn(metadata);
                when(metadata.getResource()).thenReturn(resource);
                when(resource.getInputStream()).thenThrow(new IOException("Audio resource unavailable"));
            }

            String requestUri = "/audio-requests/" + operation;
            mockMvc.perform(get(requestUri).param("transformed_media_id", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Audio resource unavailable"))
                .andExpect(jsonPath("$.instance").value(requestUri));
        }
    }

    @Test
    void givenBusinessErrorExtensions_whenHandled_thenPreservesRootFieldsAndValueTypes() throws Exception {
        when(uploadController.upload()).thenThrow(new DartsApiException(CommonApiError.BAD_REQUEST, Map.of(
            "duplicate_transcription_id", 123,
            "partial_failure", List.of(Map.of("transcription_id", 456)),
            "latest_automated_retention_date", "2026-02-07"
        )));

        mockMvc.perform(post(UPLOAD_URI))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("COMMON_104"))
            .andExpect(jsonPath("$.duplicate_transcription_id").value(123))
            .andExpect(jsonPath("$.partial_failure[0].transcription_id").value(456))
            .andExpect(jsonPath("$.latest_automated_retention_date").value("2026-02-07"))
            .andExpect(jsonPath("$.properties").doesNotExist());
    }

    @RestController
    static class UploadController {

        @PostMapping(UPLOAD_URI)
        public ResponseEntity<Void> upload() {
            return ResponseEntity.ok().build();
        }
    }
}
