package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourtCaseEntityTest {

    @Test
    void negativeValidateIsExpiredIsExpired() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setDataAnonymised(true);
        DartsApiException exception = assertThrows(DartsApiException.class, courtCase::validateIsExpired);
        assertThat(exception.getError()).isEqualTo(CaseApiError.CASE_EXPIRED);
        assertThat(exception.getMessage()).isEqualTo("Case has expired.");
    }

    @Test
    void positiveValidateIsExpiredIsNotExpired() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setDataAnonymised(false);
        assertDoesNotThrow(courtCase::validateIsExpired);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void getAllAssociatedMedias_hasNullOrEmptyHearings(List<HearingEntity> hearingEntityList) {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setHearings(hearingEntityList);
        assertThat(courtCase.getAllAssociatedMedias()).isEmpty();
    }

    @Test
    void getAllAssociatedMedias_hasHearingsWithMedia() {

        MediaEntity hearing1Media1 = new MediaEntity();
        hearing1Media1.setId(1L);//Make sure the ID is set to ensure uniqueness
        MediaEntity hearing1Media2 = new MediaEntity();
        hearing1Media2.setId(2L);
        MediaEntity hearing2Media1 = new MediaEntity();
        hearing2Media1.setId(3L);
        MediaEntity hearing2Media2 = new MediaEntity();
        hearing2Media2.setId(4L);

        Set<MediaEntity> hearing1Medias = new HashSet<>();
        hearing1Medias.add(hearing1Media1);
        hearing1Medias.add(hearing1Media2);
        hearing1Medias.add(null); // Make sure nulls within MediaEntity::getMedias are ignored

        HearingEntity hearingEntity1 = new HearingEntity();
        HearingEntity hearingEntity2 = new HearingEntity();
        HearingEntity hearingEntity3 = new HearingEntity();


        hearingEntity1.setMedias(hearing1Medias);
        hearingEntity2.setMedias(Set.of(hearing2Media1, hearing2Media2));
        hearingEntity3.setMedias(null);// Make sure nulls are ignored

        CourtCaseEntity courtCase = new CourtCaseEntity();
        List<HearingEntity> hearingEntities = new ArrayList<>();
        hearingEntities.add(hearingEntity1);
        hearingEntities.add(hearingEntity2);
        hearingEntities.add(hearingEntity3);
        hearingEntities.add(null); // Add nulls to ensure they are ignored
        courtCase.setHearings(hearingEntities);
        List<MediaEntity> allMedias = courtCase.getAllAssociatedMedias();
        assertThat(allMedias)
            .hasSize(4)
            .containsExactlyInAnyOrder(
                hearing1Media1,
                hearing1Media2,
                hearing2Media1,
                hearing2Media2
            );

    }

    @ParameterizedTest
    @NullAndEmptySource
    void getAllAssociatedMedias_hasNullOrEmptyMediaLinkedCase(List<MediaLinkedCaseEntity> mediaLinkedCaseEntities) {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setMediaLinkedCaseList(mediaLinkedCaseEntities);
        assertThat(courtCase.getAllAssociatedMedias()).isEmpty();
    }

    @Test
    void getAllAssociatedMedias_hasOnlyMediaLinkedCase() {
        MediaLinkedCaseEntity mediaLinkedCase1 = new MediaLinkedCaseEntity();
        MediaLinkedCaseEntity mediaLinkedCase2 = new MediaLinkedCaseEntity();
        MediaLinkedCaseEntity mediaLinkedCase3 = new MediaLinkedCaseEntity();

        MediaEntity mediaLinkedCase1Media = new MediaEntity();
        mediaLinkedCase1Media.setId(1L);// Make sure the ID is set to ensure uniqueness
        MediaEntity mediaLinkedCase2Media = new MediaEntity();
        mediaLinkedCase2Media.setId(2L);

        mediaLinkedCase1.setMedia(mediaLinkedCase1Media);
        mediaLinkedCase2.setMedia(mediaLinkedCase2Media);
        mediaLinkedCase3.setMedia(null);// Make sure nulls are ignored

        CourtCaseEntity courtCase = new CourtCaseEntity();
        List<MediaLinkedCaseEntity> mediaLinkedCaseList = new ArrayList<>();
        mediaLinkedCaseList.add(mediaLinkedCase1);
        mediaLinkedCaseList.add(mediaLinkedCase2);
        mediaLinkedCaseList.add(mediaLinkedCase3);
        mediaLinkedCaseList.add(null);// Add nulls to ensure they are ignored
        courtCase.setMediaLinkedCaseList(mediaLinkedCaseList);

        List<MediaEntity> allMedias = courtCase.getAllAssociatedMedias();
        assertThat(allMedias)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                mediaLinkedCase1Media,
                mediaLinkedCase2Media
            );
    }

    @Test
    void getAllAssociatedMedias_hasHearingsMediaAndLinkedCaseMedia() {
        HearingEntity hearingEntity = new HearingEntity();
        MediaEntity hearingMedia1 = new MediaEntity();
        hearingMedia1.setId(1L); // Make sure the ID is set to ensure uniqueness
        hearingEntity.setMedias(Set.of(hearingMedia1));

        MediaLinkedCaseEntity mediaLinkedCase = new MediaLinkedCaseEntity();
        MediaEntity mediaLinkedCaseMedia = new MediaEntity();
        mediaLinkedCaseMedia.setId(2L); // Make sure the ID is set to ensure uniqueness
        mediaLinkedCase.setMedia(mediaLinkedCaseMedia);

        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setHearings(List.of(hearingEntity));
        courtCase.setMediaLinkedCaseList(List.of(mediaLinkedCase));

        List<MediaEntity> allMedias = courtCase.getAllAssociatedMedias();
        assertThat(allMedias)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                hearingMedia1,
                mediaLinkedCaseMedia
            );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void getAllAssociatedAnnotationDocuments_hasNullOrEmptyHearings(List<HearingEntity> hearingEntityList) {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setHearings(hearingEntityList);
        assertThat(courtCase.getAllAssociatedAnnotationDocuments()).isEmpty();
    }

    @Test
    void getAllAssociatedAnnotationDocuments_hasHearingsWithAnnotationDocuments() {
        AnnotationDocumentEntity hearing1Annotation1Document1 = new AnnotationDocumentEntity();
        hearing1Annotation1Document1.setId(1L);//Make sure the ID is set to ensure uniqueness
        AnnotationDocumentEntity hearing1Annotation1Document2 = new AnnotationDocumentEntity();
        hearing1Annotation1Document2.setId(2L);
        AnnotationDocumentEntity hearing2Annotation1Document1 = new AnnotationDocumentEntity();
        hearing2Annotation1Document1.setId(3L);


        List<AnnotationDocumentEntity> hearing1Annotation1Documents = new ArrayList<>();
        hearing1Annotation1Documents.add(hearing1Annotation1Document1);
        hearing1Annotation1Documents.add(hearing1Annotation1Document2);
        hearing1Annotation1Documents.add(null);//Make sure nulls within AnnotationEntity::getAnnotationDocuments are ignored

        final AnnotationEntity hearing1Annotation1 = new AnnotationEntity();
        final AnnotationEntity hearing1Annotation2 = new AnnotationEntity();
        final AnnotationEntity hearing2Annotation1 = new AnnotationEntity();
        final AnnotationEntity hearing2Annotation2 = new AnnotationEntity();

        hearing1Annotation1.setAnnotationDocuments(hearing1Annotation1Documents);
        hearing1Annotation2.setAnnotationDocuments(List.of(hearing2Annotation1Document1));
        hearing2Annotation2.setAnnotationDocuments(null);//Make sure nulls within AnnotationEntity::getAnnotationDocuments are ignored

        Set<AnnotationEntity> hearing1Annotations = new HashSet<>();
        hearing1Annotations.add(hearing1Annotation1);
        hearing1Annotations.add(hearing1Annotation2);
        hearing1Annotations.add(null);//Make sure nulls within HearingEntity::getAnnotations are ignored

        HearingEntity hearingEntity1 = new HearingEntity();
        HearingEntity hearingEntity2 = new HearingEntity();
        HearingEntity hearingEntity3 = new HearingEntity();

        hearingEntity1.setAnnotations(hearing1Annotations);
        hearingEntity2.setAnnotations(Set.of(hearing2Annotation1));

        hearingEntity3.setAnnotations(null);//Make sure nulls are ignored

        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setHearings(List.of(hearingEntity1, hearingEntity2, hearingEntity3));

        List<AnnotationDocumentEntity> allAnnotations = courtCase.getAllAssociatedAnnotationDocuments();
        assertThat(allAnnotations)
            .hasSize(3)
            .containsExactlyInAnyOrder(
                hearing1Annotation1Document1,
                hearing1Annotation1Document2,
                hearing2Annotation1Document1
            );
    }
}
