package uk.gov.hmcts.darts.cases.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.cases.model.AdminCaseAudioResponseItem;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@UtilityClass
public class AdminCaseAudioResponseItemMapper {

    public List<AdminCaseAudioResponseItem> mapToAdminCaseAudioResponseItems(List<MediaEntity> mediaEntities) {
        return emptyIfNull(mediaEntities).stream()
            .map(AdminCaseAudioResponseItemMapper::mapToAdminCaseAudioResponseItem)
            .toList();
    }

    public AdminCaseAudioResponseItem mapToAdminCaseAudioResponseItem(MediaEntity mediaEntity) {
        AdminCaseAudioResponseItem adminCaseAudioResponseItem = new AdminCaseAudioResponseItem();
        adminCaseAudioResponseItem.setId(mediaEntity.getId());
        adminCaseAudioResponseItem.channel(mediaEntity.getChannel());
        adminCaseAudioResponseItem.setStartAt(mediaEntity.getStart());
        adminCaseAudioResponseItem.setEndAt(mediaEntity.getEnd());
        adminCaseAudioResponseItem.setCourtroom(mediaEntity.getCourtroom().getName());
        return adminCaseAudioResponseItem;

    }
}
