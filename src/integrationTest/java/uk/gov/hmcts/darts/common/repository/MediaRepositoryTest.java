package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.test.common.data.MediaTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;

public class MediaRepositoryTest extends PostgresIntegrationBase {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private DartsPersistence dartsPersistence;

    @Nested
    class FindAllByChronicleIdTest {

        @BeforeEach
        void setUp() {
            // Just some standing data
            MediaEntity media = PersistableFactory.getMediaTestData().someMinimalBuilder()
                .chronicleId("1000")
                .build()
                .getEntity();

            dartsPersistence.save(media);
        }

        @Test
        void shouldReturnEmptyList_whenThereAreNoOtherVersions() {
            // Given
            final String chronicleIdForWhichNoRecordsExist = "0";

            // When
            List<MediaEntity> allByChronicleId = mediaRepository.findAllByChronicleId(chronicleIdForWhichNoRecordsExist);

            // Then
            assertThat(allByChronicleId, empty());
        }

        @Test
        void shouldReturnTwoMedias_whenThereAreTwoOtherVersions() {
            // Given
            MediaTestData mediaTestData = PersistableFactory.getMediaTestData();

            final String someCommonChronicleId = "2000";

            final OffsetDateTime someStartTime = OffsetDateTime.parse("2025-01-01T00:00:00Z");
            MediaEntity media1 = mediaTestData.someMinimalBuilder()
                .chronicleId(someCommonChronicleId)
                .start(someStartTime)
                .build()
                .getEntity();
            dartsPersistence.save(media1);

            final OffsetDateTime someLaterStartTime = someStartTime.plusDays(1);
            MediaEntity media2 = mediaTestData.someMinimalBuilder()
                .chronicleId(someCommonChronicleId)
                .start(someLaterStartTime)
                .build()
                .getEntity();
            dartsPersistence.save(media2);

            // When
            List<MediaEntity> foundMedias = mediaRepository.findAllByChronicleId(someCommonChronicleId);

            // Then
            assertThat(foundMedias, hasSize(2));

            MediaEntity firstFoundMedia = foundMedias.get(0);
            assertThat(firstFoundMedia.getId(), equalTo(media1.getId()));

            MediaEntity secondFoundMedia = foundMedias.get(1);
            assertThat(secondFoundMedia.getId(), equalTo(media2.getId()));
        }

    }

}
