package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionCommentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.transcriptions.mapper.TranscriptionResponseMapper;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionWorkflowsResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceGetWorkflowsTest {

    @Mock
    private TranscriptionRepository transcriptionRepository;
    @Mock
    private TranscriptionWorkflowRepository transcriptionWorkflowRepository;
    @Mock
    private TranscriptionCommentRepository transcriptionCommentRepository;
    @Mock
    private TranscriptionResponseMapper transcriptionResponseMapper;
    @Mock
    private GetTranscriptionWorkflowsResponse mockTranscriptionWorkflowResponse;
    @Mock
    private GetTranscriptionWorkflowsResponse mockTranscriptionWorkflowResponse2;
    @Mock
    TranscriptionWorkflowEntity mockTranscriptionWorkflowEntity;
    @Mock
    TranscriptionWorkflowEntity mockTranscriptionWorkflowEntity2;
    @Mock
    TranscriptionCommentEntity transcriptionComment;
    @InjectMocks
    private TranscriptionServiceImpl transcriptionService;

    @Test
    void getCurrentTranscriptionWorkflowSuccess() {
        when(transcriptionRepository.findById(anyLong())).thenReturn(Optional.of(new TranscriptionEntity()));
        when(transcriptionWorkflowRepository.findByTranscriptionOrderByWorkflowTimestampDesc(any())).thenReturn(getListOfTranscriptionWorkflows());
        when(transcriptionResponseMapper.mapToTranscriptionWorkflowsResponse(List.of(mockTranscriptionWorkflowEntity), List.of()))
            .thenReturn(List.of(mockTranscriptionWorkflowResponse));

        List<GetTranscriptionWorkflowsResponse> transcriptionWorkflows = transcriptionService.getTranscriptionWorkflows(1L, true);

        assertEquals(1, transcriptionWorkflows.size());
        verify(transcriptionRepository, times(1)).findById(anyLong());
        verify(transcriptionWorkflowRepository, times(1)).findByTranscriptionOrderByWorkflowTimestampDesc(any());
    }

    @Test
    void getTranscriptionWorkflowSuccess() {
        TranscriptionEntity transcription = new TranscriptionEntity();
        when(transcriptionRepository.findById(anyLong())).thenReturn(Optional.of(transcription));
        when(transcriptionWorkflowRepository.findByTranscriptionOrderByWorkflowTimestampDesc(any())).thenReturn(getListOfTranscriptionWorkflows());
        when(transcriptionCommentRepository.getByTranscriptionAndTranscriptionWorkflowIsNull(any())).thenReturn(List.of(transcriptionComment));
        when(transcriptionResponseMapper.mapToTranscriptionWorkflowsResponse(getListOfTranscriptionWorkflows(), List.of(transcriptionComment)))
            .thenReturn(List.of(mockTranscriptionWorkflowResponse, mockTranscriptionWorkflowResponse2));

        List<GetTranscriptionWorkflowsResponse> transcriptionWorkflows = transcriptionService.getTranscriptionWorkflows(1L, false);

        assertEquals(2, transcriptionWorkflows.size());
        verify(transcriptionRepository, times(1)).findById(anyLong());
        verify(transcriptionWorkflowRepository, times(1)).findByTranscriptionOrderByWorkflowTimestampDesc(any());
        verify(transcriptionCommentRepository).getByTranscriptionAndTranscriptionWorkflowIsNull(transcription);
    }

    @Test
    void handleNoTranscriptionWorkflows() {
        when(transcriptionRepository.findById(anyLong())).thenReturn(Optional.of(new TranscriptionEntity()));
        when(transcriptionWorkflowRepository.findByTranscriptionOrderByWorkflowTimestampDesc(any())).thenReturn(Collections.emptyList());
        when(transcriptionResponseMapper.mapToTranscriptionWorkflowsResponse(List.of(), List.of()))
            .thenReturn(List.of(mockTranscriptionWorkflowResponse));

        List<GetTranscriptionWorkflowsResponse> transcriptionWorkflows = transcriptionService.getTranscriptionWorkflows(1L, true);

        assertEquals(1, transcriptionWorkflows.size());
        verify(transcriptionRepository, times(1)).findById(anyLong());
        verify(transcriptionWorkflowRepository, times(1)).findByTranscriptionOrderByWorkflowTimestampDesc(any());
    }

    @Test
    void shouldReturnEmptyListWhenTranscriptionIdNotFound() {
        when(transcriptionRepository.findById(anyLong())).thenReturn(Optional.empty());

        var transcriptionWorkflows = transcriptionService.getTranscriptionWorkflows(1L, true);

        verify(transcriptionRepository, times(1)).findById(any());
        assertEquals(0, transcriptionWorkflows.size());
    }

    private List<TranscriptionWorkflowEntity> getListOfTranscriptionWorkflows() {

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = new ArrayList<>();
        transcriptionWorkflowEntities.add(mockTranscriptionWorkflowEntity);
        transcriptionWorkflowEntities.add(mockTranscriptionWorkflowEntity2);

        return transcriptionWorkflowEntities;
    }

}
