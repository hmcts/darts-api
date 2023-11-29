package uk.gov.hmcts.darts.archiverecordsmanagement.model;

public interface ArchiveRecord {

    String getOperation();
    String getRelationId();
    ArchiveMetadata getMetadata();

}
