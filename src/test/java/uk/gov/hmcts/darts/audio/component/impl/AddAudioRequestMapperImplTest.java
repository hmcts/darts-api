package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddAudioRequestMapperImplTest {
    @Mock
    RetrieveCoreObjectService courtroomRepository;

    @Mock
    UserIdentity userIdentity;
    @Mock
    AuthorisationApi authorisationApi;

    AddAudioRequestMapperImpl addAudioRequestMapperImpl;
    UserAccountEntity userAccount;

    @BeforeEach
    void setUp() {
        addAudioRequestMapperImpl = new AddAudioRequestMapperImpl(courtroomRepository, userIdentity);
        userAccount = new UserAccountEntity();
        userAccount.setId(0);
    }

    @Test
    void testMapToMedia() {
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("SWANSEA");

        CourtroomEntity courtroomEntity = new CourtroomEntity(1, "1", courthouse);
        when(courtroomRepository.retrieveOrCreateCourtroom(anyString(), anyString(), any(UserAccountEntity.class))).thenReturn(
            courtroomEntity);

        OffsetDateTime start = OffsetDateTime.now().minusHours(1);
        OffsetDateTime endDate = OffsetDateTime.now();

        MediaEntity media = new MediaEntity();
        media.setStart(start);
        media.setEnd(endDate);
        media.setChannel(1);
        media.setTotalChannels(2);
        media.setCourtroom(courtroomEntity);
        media.setCaseNumberList(List.of("case1", "case2"));

        MediaEntity result = addAudioRequestMapperImpl.mapToMedia(
            new AddAudioMetadataRequest(
                start,
                endDate,
                1,
                2,
                "mp3",
                "test",
                "courthouse",
                "courtroom",
                1000L,
                List.of("case1", "case2")
            ), userAccount);
        Assertions.assertEquals(media.getStart(), result.getStart());
        Assertions.assertEquals(media.getEnd(), result.getEnd());
        Assertions.assertEquals(media.getChannel(), result.getChannel());
        Assertions.assertEquals(media.getTotalChannels(), result.getTotalChannels());
        Assertions.assertEquals(media.getCourtroom().getName(), result.getCourtroom().getName());
        Assertions.assertEquals(media.getCaseNumberList().size(), result.getCaseNumberList().size());
        Assertions.assertEquals(media.getCreatedBy(), userIdentity.getUserAccount());
        Assertions.assertEquals(media.getLastModifiedBy(), userIdentity.getUserAccount());
    }
}

