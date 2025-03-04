package uk.gov.hmcts.darts.audio.service.impl;

import java.util.List;
import java.util.Optional;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.audio.service.OutboundAudioDeleterProcessorSingleElement;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

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
    private UserIdentity userIdentity;

    @Mock
    private TransformedMediaRepository transformedMediaRepository;

    @BeforeEach
    void setUp() {
        this.outboundAudioDeleterProcessorImpl = new OutboundAudioDeleterProcessorImpl(
            userAccountRepository, lastAccessedDeletionDayCalculator,
            userIdentity, transformedMediaRepository,
            singleElementProcessor,
            new OutboundAudioDeleterProcessorImpl.TransformedMediaEntityProcessor(singleElementProcessor, transformedMediaRepository)
        );
        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);
    }

    @Test
    void testContinuesProcessingNextIterationOnException() {
        // given
        var transformedMedia1 = someTransformedMediaNotOwnedByUserInGroup(List.of(MEDIA_IN_PERPETUITY, SUPER_ADMIN, SUPER_USER));
        var transformedMedia2 = someTransformedMediaNotOwnedByUserInGroup(List.of(MEDIA_IN_PERPETUITY, SUPER_ADMIN, SUPER_USER));

        when(transformedMediaRepository.findAllDeletableTransformedMedia(any(), eq(Limit.of(1000))))
            .thenReturn(List.of(1, 2));
        when(transformedMediaRepository.findById(1)).thenReturn(Optional.of(transformedMedia1));
        when(transformedMediaRepository.findById(2)).thenReturn(Optional.of(transformedMedia2));

        var deletedValues = List.of(new TransientObjectDirectoryEntity());
        when(singleElementProcessor.markForDeletion(any(), any()))
            .thenThrow(new RuntimeException("Some error"))
            .thenReturn(deletedValues);

        // when
        List<TransientObjectDirectoryEntity> result = outboundAudioDeleterProcessorImpl.markForDeletion(1000);

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
        when(transformedMediaRepository.findAllDeletableTransformedMedia(any(), eq(Limit.of(1000))))
            .thenReturn(List.of(1, 2));
        when(transformedMediaRepository.findById(1)).thenReturn(Optional.of(transformedMediaOwnedByUserInMediaInPerpetuityGroup));
        when(transformedMediaRepository.findById(2)).thenReturn(Optional.of(transformedMediaNotOwnedByUserInMediaInPerpetuityGroup));

        // when
        outboundAudioDeleterProcessorImpl.markForDeletion(1000);

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

