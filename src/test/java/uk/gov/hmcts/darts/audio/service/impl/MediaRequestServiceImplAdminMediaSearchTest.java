package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.mapper.GetAdminMediaResponseMapper;
import uk.gov.hmcts.darts.audio.model.AdminActionRequest;
import uk.gov.hmcts.darts.audio.model.MediaHideRequest;
import uk.gov.hmcts.darts.audio.model.MediaHideResponse;
import uk.gov.hmcts.darts.audio.validation.MediaHideOrShowValidator;
import uk.gov.hmcts.darts.audio.validation.SearchMediaValidator;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class MediaRequestServiceImplAdminMediaSearchTest {

    @InjectMocks
    private MediaRequestServiceImpl mediaRequestService;

    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private TransformedMediaRepository mockTransformedMediaRepository;

    @Mock
    private MediaHideOrShowValidator mediaHideOrShowValidator;

    @Mock
    private ObjectAdminActionRepository objectAdminActionRepository;

    @Mock
    private ObjectHiddenReasonRepository objectHiddenReasonRepository;

    @Mock
    private UserIdentity userIdentity;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @Mock
    private SearchMediaValidator searchMediaValidator;

    @Mock
    private AuditApi auditApi;

    @Captor
    ArgumentCaptor<ObjectAdminActionEntity> objectAdminActionEntityArgumentCaptor;

    @Captor
    ArgumentCaptor<MediaEntity> mediaEntityArgumentCaptor;

    private MockedStatic<GetAdminMediaResponseMapper> adminMediaSearchResponseMapperMockedStatic;

    @AfterEach
    void finish() {
        if (adminMediaSearchResponseMapperMockedStatic != null) {
            adminMediaSearchResponseMapperMockedStatic.close();
        }
    }

    @Test
    void testMediaDocumentHide() {
        MediaHideRequest request = new MediaHideRequest();
        request.setIsHidden(true);
        setupTestMediaHide(request);
    }

    @Test
    void testMediaHideDefaultIsHidden() {
        MediaHideRequest request = new MediaHideRequest();
        setupTestMediaHide(request);
    }

    void setupTestMediaHide(MediaHideRequest request) {
        OffsetDateTime testTime = OffsetDateTime.of(2023, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        adminMediaSearchResponseMapperMockedStatic = Mockito.mockStatic(GetAdminMediaResponseMapper.class);

        Integer hideOrShowTranscriptionDocument = 343;
        Integer reasonId = 555;

        String ticketReference = "my ticket reference";
        String comments = "my comments";

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(reasonId);
        adminActionRequest.setTicketReference(ticketReference);
        adminActionRequest.setComments(comments);

        request.setAdminAction(adminActionRequest);

        UserAccountEntity userAccountEntity = mock(UserAccountEntity.class);

        MediaEntity mediaEntity = new MediaEntity();
        when(mediaRepository.findByIdIncludeDeleted(hideOrShowTranscriptionDocument)).thenReturn(Optional.of(mediaEntity));
        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        when(mediaRepository.saveAndFlush(mediaEntityArgumentCaptor.capture())).thenReturn(mediaEntity);
        ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
        ObjectHiddenReasonEntity objectHiddenReasonEntity = new ObjectHiddenReasonEntity();
        MediaHideResponse expectedResponse = new MediaHideResponse();

        when(objectHiddenReasonRepository.findById(reasonId)).thenReturn(Optional.of(objectHiddenReasonEntity));

        when(objectAdminActionRepository.saveAndFlush(objectAdminActionEntityArgumentCaptor.capture())).thenReturn(objectAdminActionEntity);

        adminMediaSearchResponseMapperMockedStatic.when(
                () -> GetAdminMediaResponseMapper.mapHideOrShowResponse(mediaEntity, objectAdminActionEntity))
            .thenReturn(expectedResponse);


        //run the test
        MediaHideResponse actualResponse
            = mediaRequestService.adminHideOrShowMediaById(hideOrShowTranscriptionDocument, request);


        // make the assertion
        Assertions.assertTrue(mediaEntityArgumentCaptor.getValue().isHidden());
        Assertions.assertEquals(expectedResponse, actualResponse);
        Assertions.assertEquals(request.getAdminAction().getComments(), objectAdminActionEntityArgumentCaptor.getValue().getComments());
        Assertions.assertEquals(request.getAdminAction().getReasonId(), reasonId);
        Assertions.assertFalse(objectAdminActionEntityArgumentCaptor.getValue().isMarkedForManualDeletion());
        Assertions.assertNotNull(objectAdminActionEntityArgumentCaptor.getValue().getHiddenBy());
        Assertions.assertNotNull(objectAdminActionEntityArgumentCaptor.getValue().getHiddenDateTime());
        Assertions.assertNull(objectAdminActionEntityArgumentCaptor.getValue().getMarkedForManualDelBy());
        Assertions.assertNull(objectAdminActionEntityArgumentCaptor.getValue().getMarkedForManualDelDateTime());
    }

    @Test
    void testMediaShow() {
        adminMediaSearchResponseMapperMockedStatic = Mockito.mockStatic(GetAdminMediaResponseMapper.class);

        MediaHideRequest request = new MediaHideRequest();
        request.setIsHidden(false);

        Integer hideOrShowTranscriptionDocument = 343;
        Integer reasonId = 555;

        AdminActionRequest adminActionRequest = new AdminActionRequest();
        adminActionRequest.setReasonId(reasonId);
        request.setAdminAction(adminActionRequest);

        MediaEntity mediaEntity = new MediaEntity();
        when(mediaRepository.findByIdIncludeDeleted(hideOrShowTranscriptionDocument)).thenReturn(Optional.of(mediaEntity));

        Integer objectAdminActionEntityId = 1000;
        Integer objectAdminActionEntityId1 = 1001;

        ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
        objectAdminActionEntity.setId(objectAdminActionEntityId);
        ObjectAdminActionEntity objectAdminActionEntity1 = new ObjectAdminActionEntity();
        objectAdminActionEntity1.setId(objectAdminActionEntityId1);

        when(mediaRepository.saveAndFlush(mediaEntityArgumentCaptor.capture())).thenReturn(mediaEntity);
        when(objectAdminActionRepository
                 .findByMedia_Id(hideOrShowTranscriptionDocument)).thenReturn(List.of(objectAdminActionEntity, objectAdminActionEntity1));

        MediaHideResponse expectedResponse = new MediaHideResponse();

        adminMediaSearchResponseMapperMockedStatic.when(() -> GetAdminMediaResponseMapper.mapHideOrShowResponse(mediaEntity, null))
            .thenReturn(expectedResponse);


        // run the test
        MediaHideResponse actualResponse
            = mediaRequestService.adminHideOrShowMediaById(hideOrShowTranscriptionDocument, request);

        // make the assertion
        Assertions.assertFalse(mediaEntityArgumentCaptor.getValue().isHidden());
        Assertions.assertEquals(expectedResponse, actualResponse);
        verify(objectAdminActionRepository, times(1)).deleteById(objectAdminActionEntityId);
        verify(objectAdminActionRepository, times(1)).deleteById(objectAdminActionEntityId1);
    }
}