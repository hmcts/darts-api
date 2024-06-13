package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.mapper.AdminMediaMapper;
import uk.gov.hmcts.darts.audio.model.AdminMediaResponse;
import uk.gov.hmcts.darts.audio.service.AdminMediaService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRepository;

@Service
@RequiredArgsConstructor
public class AdminMediaServiceImpl implements AdminMediaService {

    private final MediaRepository mediaRepository;
    private final AdminMediaMapper adminMediaMapper;

    public AdminMediaResponse getMediasById(Integer id) {
        var mediaEntity = mediaRepository.findById(id)
            .orElseThrow(() -> new DartsApiException(AudioApiError.MEDIA_NOT_FOUND));

        return adminMediaMapper.toApiModel(mediaEntity);
    }

}
