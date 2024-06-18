package uk.gov.hmcts.darts.casedocument.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.casedocument.template.ExternalObjectDirectoryCaseDocument;
import uk.gov.hmcts.darts.casedocument.template.MediaCaseDocument;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.util.List;

@Mapper(componentModel = "spring", uses = {BasicMapper.class, ExternalObjectDirectoryRepository.class})
public abstract class MediaCaseDocumentMapper implements CourtCaseMapper {

    @Autowired
    ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Mappings({
        @Mapping(expression = "java(mapEods(externalObjectDirectoryRepository.findByMedia(mediaEntity)))", target = "externalObjectDirectories"),
    })
    public abstract MediaCaseDocument mapCustom(MediaEntity mediaEntity);

    public abstract List<ExternalObjectDirectoryCaseDocument> mapEods(List<ExternalObjectDirectoryEntity> entities);
}
