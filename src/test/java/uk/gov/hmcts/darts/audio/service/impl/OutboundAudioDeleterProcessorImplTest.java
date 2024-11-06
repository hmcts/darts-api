package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessorSingleElement;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.MEDIA_IN_PERPETUITY;
import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityGroupEnum.SUPER_USER;

@ExtendWith(MockitoExtension.class)
class OutboundAudioDeleterProcessorImplTest {
    @Mock
    LastAccessedDeletionDayCalculator lastAccessedDeletionDayCalculator;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserAccountEntity userAccountEntity;
    @Mock
    private OutboundAudioDeleterProcessorSingleElement singleElementProcessor;
    private OutboundAudioDeleterProcessorImpl outboundAudioDeleterProcessorImpl;

    @Mock
    private SystemUserHelper systemUserHelper;

    @Mock
    private TransformedMediaRepository transformedMediaRepository;

    @BeforeEach
    void setUp() {
        this.outboundAudioDeleterProcessorImpl = new OutboundAudioDeleterProcessorImpl(
            userAccountRepository, lastAccessedDeletionDayCalculator,
            systemUserHelper, transformedMediaRepository,
            singleElementProcessor
        );
        when(systemUserHelper.getReferenceTo(SystemUsersEnum.OUTBOUND_AUDIO_DELETER_AUTOMATED_TASK)).thenReturn(userAccountEntity);
    }

    @Test
    void testContinuesProcessingNextIterationOnException() {
        // given
        var transformedMedia1 = someTransformedMediaNotOwnedByUserInGroup(List.of(MEDIA_IN_PERPETUITY, SUPER_ADMIN, SUPER_USER));
        var transformedMedia2 = someTransformedMediaNotOwnedByUserInGroup(List.of(MEDIA_IN_PERPETUITY, SUPER_ADMIN, SUPER_USER));

        when(transformedMediaRepository.findAllDeletableTransformedMedia(any()))
            .thenReturn(List.of(transformedMedia1, transformedMedia2));

        var deletedValues = List.of(new TransientObjectDirectoryEntity());
        when(singleElementProcessor.markForDeletion(any(), any()))
            .thenThrow(new RuntimeException("Some error"))
            .thenReturn(deletedValues);

        // when
        List<TransientObjectDirectoryEntity> result = outboundAudioDeleterProcessorImpl.markForDeletion();

        // then
        assertThat(result).isEqualTo(deletedValues);
    }

    @Test
    void doesntMarkForDeletionWhenCurrentOwnerIsInGroupMediaInPerpetuity() {
        // given
        var transformedMediaOwnedByUserInMediaInPerpetuityGroup = someTransformedMediaOwnedByUserInGroup(
            List.of(MEDIA_IN_PERPETUITY, SUPER_ADMIN, SUPER_USER));
        var transformedMediaNotOwnedByUserInMediaInPerpetuityGroup = someTransformedMediaNotOwnedByUserInGroup(
            List.of(MEDIA_IN_PERPETUITY, SUPER_ADMIN, SUPER_USER));
        when(transformedMediaRepository.findAllDeletableTransformedMedia(any()))
            .thenReturn(List.of(
                transformedMediaOwnedByUserInMediaInPerpetuityGroup,
                transformedMediaNotOwnedByUserInMediaInPerpetuityGroup));

        // when
        outboundAudioDeleterProcessorImpl.markForDeletion();

        // then
        verify(singleElementProcessor, times(1))
            .markForDeletion(any(), eq(transformedMediaNotOwnedByUserInMediaInPerpetuityGroup));
        verify(singleElementProcessor, times(0))
            .markForDeletion(any(), eq(transformedMediaOwnedByUserInMediaInPerpetuityGroup));
    }

    private TransformedMediaEntity someTransformedMediaOwnedByUserInGroup(List<SecurityGroupEnum> securityGroupEnum) {
        var transformedMediaEntity = mock(TransformedMediaEntity.class);
        when(transformedMediaEntity.isOwnerInSecurityGroup(securityGroupEnum)).thenReturn(true);
        return transformedMediaEntity;
    }

    private TransformedMediaEntity someTransformedMediaNotOwnedByUserInGroup(List<SecurityGroupEnum> securityGroupEnum) {
        var transformedMediaEntity = mock(TransformedMediaEntity.class);
        when(transformedMediaEntity.isOwnerInSecurityGroup(securityGroupEnum)).thenReturn(false);
        return transformedMediaEntity;
    }
}

