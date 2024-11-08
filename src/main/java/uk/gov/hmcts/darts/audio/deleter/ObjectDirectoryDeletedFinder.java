package uk.gov.hmcts.darts.audio.deleter;

import java.util.List;

public interface ObjectDirectoryDeletedFinder<T> {

    List<T> findMarkedForDeletion(Integer batchSize);

}
