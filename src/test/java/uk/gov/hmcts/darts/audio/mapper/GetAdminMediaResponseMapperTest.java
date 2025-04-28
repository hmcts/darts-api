package uk.gov.hmcts.darts.audio.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.model.AdminMediaCourthouseResponse;
import uk.gov.hmcts.darts.audio.model.AdminMediaCourtroomResponse;
import uk.gov.hmcts.darts.audio.model.AdminMediaVersionResponse;
import uk.gov.hmcts.darts.audio.model.AdminVersionedMediaResponse;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAdminMediaResponseMapperTest {


    @Mock
    private CourtroomMapper courtroomMapper;
    @Mock
    private CourthouseMapper courthouseMapper;

    @InjectMocks
    @Spy
    private GetAdminMediaResponseMapper getAdminMediaResponseMapper;

    @Test
    void mapAdminVersionedMediaResponse_shouldMapAdminVersionedMediaResponse() {
        final String legacyObjectId = "legacyObjectId";
        MediaEntity mediaEntity = mock(MediaEntity.class);
        when(mediaEntity.getLegacyObjectId()).thenReturn(legacyObjectId);

        MediaEntity versioendMediaEntity1 = mock(MediaEntity.class);
        MediaEntity versioendMediaEntity2 = mock(MediaEntity.class);
        MediaEntity versioendMediaEntity3 = mock(MediaEntity.class);
        List<MediaEntity> versionedMediaEntities = List.of(versioendMediaEntity1, versioendMediaEntity2, versioendMediaEntity3);

        AdminMediaVersionResponse mediaEntityVersionedResponse = mock(AdminMediaVersionResponse.class);
        AdminMediaVersionResponse versionedMediaEntityVersionedResponse1 = mock(AdminMediaVersionResponse.class);
        AdminMediaVersionResponse versionedMediaEntityVersionedResponse3 = mock(AdminMediaVersionResponse.class);

        doReturn(mediaEntityVersionedResponse).when(getAdminMediaResponseMapper).mapAdminMediaVersionResponse(mediaEntity);
        doReturn(versionedMediaEntityVersionedResponse1).when(getAdminMediaResponseMapper).mapAdminMediaVersionResponse(versioendMediaEntity1);
        doReturn(null).when(getAdminMediaResponseMapper).mapAdminMediaVersionResponse(versioendMediaEntity2);
        doReturn(versionedMediaEntityVersionedResponse3).when(getAdminMediaResponseMapper).mapAdminMediaVersionResponse(versioendMediaEntity3);


        AdminVersionedMediaResponse adminVersionedMediaResponse = getAdminMediaResponseMapper.mapAdminVersionedMediaResponse(mediaEntity,
                                                                                                                             versionedMediaEntities);
        assertThat(adminVersionedMediaResponse).isNotNull();
        assertThat(adminVersionedMediaResponse.getMediaObjectId()).isEqualTo(legacyObjectId);
        assertThat(adminVersionedMediaResponse.getCurrentVersion()).isEqualTo(mediaEntityVersionedResponse);
        //versionedMediaEntityVersionedResponse2 should be excluded as it is null
        assertThat(adminVersionedMediaResponse.getPreviousVersions())
            .containsExactly(versionedMediaEntityVersionedResponse1,
                             versionedMediaEntityVersionedResponse3);
    }

    @Test
    void mapAdminMediaVersionResponse_courtRoomDoesNotExist_shouldMapAdminMediaVersionResponse() {
        MediaEntity media = new MediaEntity();
        media.setId(321L);
        media.setStart(OffsetDateTime.now());
        media.setEnd(OffsetDateTime.now().plusDays(1));
        media.setChannel(2);
        media.setLegacyObjectId("legacyObjectId2");
        media.setChronicleId("chronicleId2");
        media.setAntecedentId("antecedentId2");
        media.setIsCurrent(false);
        media.setCreatedDateTime(OffsetDateTime.now().plusDays(3));

        AdminMediaVersionResponse response = getAdminMediaResponseMapper.mapAdminMediaVersionResponse(media);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(media.getId());
        assertThat(response.getCourtroom()).isNull();
        assertThat(response.getCourthouse()).isNull();
        assertThat(response.getStartAt()).isEqualTo(media.getStart());
        assertThat(response.getEndAt()).isEqualTo(media.getEnd());

        assertThat(response.getChannel()).isEqualTo(media.getChannel());
        assertThat(response.getChronicleId()).isEqualTo(media.getChronicleId());
        assertThat(response.getAntecedentId()).isEqualTo(media.getAntecedentId());
        assertThat(response.getIsCurrent()).isEqualTo(media.getIsCurrent());
        assertThat(response.getCreatedAt()).isEqualTo(media.getCreatedDateTime());
    }

    @Test
    void mapAdminMediaVersionResponse_shouldMapAdminMediaVersionResponse() {
        CourtroomEntity courtroomEntity = mock(CourtroomEntity.class);
        AdminMediaCourtroomResponse courtroom = mock(AdminMediaCourtroomResponse.class);
        when(courtroomMapper.toApiModel(courtroomEntity)).thenReturn(courtroom);

        CourthouseEntity courthouseEntity = mock(CourthouseEntity.class);
        AdminMediaCourthouseResponse courthouse = mock(AdminMediaCourthouseResponse.class);
        when(courthouseMapper.toApiModel(courthouseEntity)).thenReturn(courthouse);
        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);

        MediaEntity media = new MediaEntity();
        media.setId(123L);
        media.setCourtroom(courtroomEntity);
        media.setStart(OffsetDateTime.now());
        media.setEnd(OffsetDateTime.now().plusDays(1));
        media.setChannel(1);
        media.setLegacyObjectId("legacyObjectId");
        media.setChronicleId("chronicleId");
        media.setAntecedentId("antecedentId");
        media.setIsCurrent(true);
        media.setCreatedDateTime(OffsetDateTime.now().plusDays(3));

        AdminMediaVersionResponse response = getAdminMediaResponseMapper.mapAdminMediaVersionResponse(media);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(media.getId());
        assertThat(response.getCourtroom()).isEqualTo(courtroom);
        assertThat(response.getCourthouse()).isEqualTo(courthouse);
        assertThat(response.getStartAt()).isEqualTo(media.getStart());
        assertThat(response.getEndAt()).isEqualTo(media.getEnd());

        assertThat(response.getChannel()).isEqualTo(media.getChannel());
        assertThat(response.getChronicleId()).isEqualTo(media.getChronicleId());
        assertThat(response.getAntecedentId()).isEqualTo(media.getAntecedentId());
        assertThat(response.getIsCurrent()).isEqualTo(media.getIsCurrent());
        assertThat(response.getCreatedAt()).isEqualTo(media.getCreatedDateTime());

        verify(courtroomMapper).toApiModel(courtroomEntity);
        verify(courtroomEntity).getCourthouse();
        verify(courthouseMapper).toApiModel(courthouseEntity);
    }

    @Test
    void mapAdminMediaVersionResponse_ifMediaEntityIsNull_nullShouldBeReturned() {
        assertThat(getAdminMediaResponseMapper.mapAdminMediaVersionResponse(null)).isNull();
    }
}
