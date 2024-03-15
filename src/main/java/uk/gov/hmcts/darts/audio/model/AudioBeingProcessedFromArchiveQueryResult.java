package uk.gov.hmcts.darts.audio.model;

public record AudioBeingProcessedFromArchiveQueryResult(Integer mediaId,
                                                        Integer unstructuredExternalObjectDirectoryId,
                                                        Integer armExternalObjectDirectoryId) {
}
