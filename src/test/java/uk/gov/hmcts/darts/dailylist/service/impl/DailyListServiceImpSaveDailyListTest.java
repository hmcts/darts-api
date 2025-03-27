package uk.gov.hmcts.darts.dailylist.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.dailylist.mapper.DailyListMapper;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.DailyListPatchRequestInternal;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequestInternal;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@ExtendWith(MockitoExtension.class)
class DailyListServiceImpSaveDailyListTest {

    ObjectMapper objectMapper;

    @InjectMocks
    DailyListServiceImpl service;

    @Mock
    DailyListRepository dailyListRepository;

    @Mock
    DailyListMapper dailyListMapper;

    @Mock
    UserIdentity userIdentity;

    @Captor
    private ArgumentCaptor<DailyListEntity> dailyListEntityArgumentCaptor;


    @BeforeEach
    void beforeEach() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @Test
    void ok_WhenCodeNotMatchExceptionThrown() throws IOException {
        when(dailyListMapper.createDailyListFromJson(any(DailyListPostRequestInternal.class)))
            .thenReturn(new DailyListEntity());

        DailyListPostRequestInternal request = new DailyListPostRequestInternal(SourceType.CPP.toString(), null, null, null,
                                                                                null, null, getDailyListJson(), "some-message-id");
        service.saveDailyListToDatabase(request);

        //make sure an exception is not thrown.
        verify(dailyListRepository).saveAndFlush(any(DailyListEntity.class));
    }


    @Test
    void ok_Xml() {
        UserAccountEntity user = new UserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(user);

        DailyListPostRequestInternal request = new DailyListPostRequestInternal(SourceType.CPP.toString(), "Swansea", LocalDate.now(), "Thexml",
                                                                                "uniqueId",
                                                                                OffsetDateTime.now(),
                                                                                null,
                                                                                "some-message-id"
        );
        service.saveDailyListToDatabase(request);

        verify(dailyListRepository).saveAndFlush(dailyListEntityArgumentCaptor.capture());

        DailyListEntity savedDailyList = dailyListEntityArgumentCaptor.getValue();
        assertThat(savedDailyList.getUniqueId()).isEqualTo("uniqueId");
        assertThat(savedDailyList.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(savedDailyList.getSource()).isEqualTo("CPP");
        assertThat(savedDailyList.getListingCourthouse()).isNotNull();
        assertThat(savedDailyList.getXmlContent()).isEqualTo("Thexml");
        assertThat(savedDailyList.getContent()).isNull();
        assertThat(savedDailyList.getCreatedById()).isEqualTo(user.getId());
        assertThat(savedDailyList.getLastModifiedById()).isEqualTo(user.getId());
    }

    @Test
    void ok_JsonCreateDailyList() throws IOException {
        UserAccountEntity user = new UserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(user);
        when(dailyListMapper.createDailyListFromJson(any(DailyListPostRequestInternal.class)))
            .thenReturn(new DailyListEntity());

        DailyListPostRequestInternal request = new DailyListPostRequestInternal(SourceType.CPP.toString(), "Swansea", LocalDate.now(), "Thexml",
                                                                                "uniqueId",
                                                                                OffsetDateTime.now(),
                                                                                getDailyListJson(),
                                                                                "some-message-id"
        );
        service.saveDailyListToDatabase(request);

        verify(dailyListRepository).saveAndFlush(dailyListEntityArgumentCaptor.capture());

        DailyListEntity savedDailyList = dailyListEntityArgumentCaptor.getValue();
        assertThat(savedDailyList.getCreatedById()).isEqualTo(user.getId());
        assertThat(savedDailyList.getLastModifiedById()).isEqualTo(user.getId());
    }


    @Test
    void ok_PatchDailyList() throws IOException {
        UserAccountEntity createdByUser = new UserAccountEntity();
        UserAccountEntity updatedByUser = new UserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(updatedByUser);
        var dailyListEntity = new DailyListEntity();
        dailyListEntity.setCreatedBy(createdByUser);

        when(dailyListRepository.findById(1)).thenReturn(Optional.of(dailyListEntity));

        DailyListPatchRequestInternal request = new DailyListPatchRequestInternal(1, getDailyListJson());
        service.updateDailyListInDatabase(request);

        verify(dailyListRepository).saveAndFlush(dailyListEntityArgumentCaptor.capture());

        DailyListEntity savedDailyList = dailyListEntityArgumentCaptor.getValue();
        assertThat(savedDailyList.getCreatedById()).isEqualTo(createdByUser.getId());
        assertThat(savedDailyList.getLastModifiedById()).isEqualTo(updatedByUser.getId());
    }

    private DailyListJsonObject getDailyListJson() throws IOException {
        String dailyListJson = getContentsFromFile(
            "Tests/dailylist/DailyListServiceImplTest/processIncomingDailyList/DailyListRequest.json");
        return objectMapper.readValue(dailyListJson, DailyListJsonObject.class);
    }

}
