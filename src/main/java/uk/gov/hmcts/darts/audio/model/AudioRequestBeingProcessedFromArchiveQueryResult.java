package uk.gov.hmcts.darts.audio.model;

public record AudioRequestBeingProcessedFromArchiveQueryResult(Integer mediaId,
                                                               Integer unstructuredExternalObjectDirectoryId,
                                                               Integer armExternalObjectDirectoryId) {

}
