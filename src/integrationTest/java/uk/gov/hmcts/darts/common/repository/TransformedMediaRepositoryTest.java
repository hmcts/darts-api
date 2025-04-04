package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.TransformedMediaStub;

import java.util.List;
import java.util.Locale;

class TransformedMediaRepositoryTest extends PostgresIntegrationBase {

    @Autowired
    private TransformedMediaStub transformedMediaStub;

    @Autowired
    private TransformedMediaRepository transformedMediaRepository;

    private List<TransformedMediaEntity> generatedMediaEntities;

    private static final int GENERATION_COUNT = 20;


    @BeforeEach
    public void before() {
        openInViewUtil.openEntityManager();
        generatedMediaEntities = transformedMediaStub.generateTransformedMediaEntities(GENERATION_COUNT);
    }

    @AfterEach
    void closeHibernateSession() {
        openInViewUtil.closeEntityManager();
    }

    @Test
    void testFindTransformedMediaWithoutAnyParameters() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null,
            null, null, null, null, null);
        Assertions.assertEquals(generatedMediaEntities.size(), transformedMediaEntityList.size());
        Assertions.assertTrue(transformedMediaStub.getTransformedMediaIds(transformedMediaEntityList)
                                  .containsAll(transformedMediaStub.getTransformedMediaIds(generatedMediaEntities)));
    }

    @Test
    void testFindTransformedMediaWithId() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            generatedMediaEntities.getFirst().getMediaRequest().getId(),
            null, null, null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.getFirst().getId(), transformedMediaEntityList.size());
    }

    @Test
    void testFindTransformedMediaWithCaseNumber() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, generatedMediaEntities.get(3).getMediaRequest().getHearing().getCourtCase().getCaseNumber(), null, null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(transformedMediaEntityList.getFirst().getId(), generatedMediaEntities.get(3).getId());
    }

    @Test
    void testFindTransformedMediaWithCourtDisplayNameSubstringPrefixMatchOne() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null,
            TransformedMediaSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(Integer.toString(nameMatchIndex)), null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithCourtDisplayNameCaseInsensitiveSubstringPrefixMatchOne() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null,
            TransformedMediaSubStringQueryEnum.COURT_HOUSE
                .getQueryStringPrefix(Integer.toString(nameMatchIndex)).toUpperCase(Locale.getDefault()), null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithCourtDisplayNameSubstringPostFixMatchOne() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null,
            TransformedMediaSubStringQueryEnum.COURT_HOUSE.getQueryStringPostfix(Integer.toString(nameMatchIndex)), null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithCourtDisplayNameCaseInsensitiveSubstringPostFixMatchOne() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null,
            TransformedMediaSubStringQueryEnum.COURT_HOUSE
                .getQueryStringPostfix(Integer.toString(nameMatchIndex)).toUpperCase(Locale.getDefault()), null, null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithCourtDisplayNameSubstringMatchAll() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null,
            TransformedMediaSubStringQueryEnum.COURT_HOUSE.getQueryStringPrefix(), null, null, null, null, null);
        Assertions.assertEquals(GENERATION_COUNT, transformedMediaEntityList.size());
        Assertions.assertTrue(transformedMediaStub.getTransformedMediaIds(transformedMediaEntityList)
                                  .containsAll(transformedMediaStub.getTransformedMediaIds(generatedMediaEntities)));
    }

    @Test
    void testFindTransformedMediaWithHearingDate() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null,
            generatedMediaEntities.get(3).getMediaRequest().getHearing().getHearingDate(), null, null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(3).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithOwnerExact() {
        int nameMatchIndex = 3;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null,
            TransformedMediaSubStringQueryEnum.OWNER.getQueryString(Integer.toString(nameMatchIndex)), null, null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithOwnerPrefix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null,
            TransformedMediaSubStringQueryEnum.OWNER.getQueryStringPostfix(Integer.toString(nameMatchIndex)), null, null, null);

        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithOwnerCaseInsensitivePrefix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null,
            TransformedMediaSubStringQueryEnum.OWNER
                .getQueryStringPostfix(Integer.toString(nameMatchIndex))
                .toLowerCase(Locale.getDefault()), null, null, null);

        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithOwnerPostfix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null,
            TransformedMediaSubStringQueryEnum.OWNER.getQueryStringPostfix(Integer.toString(nameMatchIndex)), null, null, null);
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithOwnerCaseInsensitivePostfix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null,
            TransformedMediaSubStringQueryEnum.OWNER
                .getQueryStringPostfix(Integer.toString(nameMatchIndex)).toUpperCase(Locale.getDefault()), null, null, null);
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithOwnerSubstringMatchAll() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null,
            TransformedMediaSubStringQueryEnum.OWNER.getQueryStringPostfix(), null, null, null);
        Assertions.assertEquals(GENERATION_COUNT, transformedMediaEntityList.size());
        Assertions.assertTrue(transformedMediaStub
                                  .getTransformedMediaIds(transformedMediaEntityList)
                                  .containsAll(transformedMediaStub.getTransformedMediaIds(generatedMediaEntities)));
    }

    @Test
    void testFindTransformedMediaWithRequestedByExact() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null, null,
            TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryString(Integer.toString(nameMatchIndex)), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithRequestedByPrefix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null, null,
            TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryStringPrefix(Integer.toString(13)), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithRequestedByCaseInsensitivePrefix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null, null,
            TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryStringPrefix(Integer.toString(13)).toUpperCase(Locale.getDefault()), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithRequestedByPostfix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null, null,
            TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(Integer.toString(13)), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithRequestedByCaseInsensitivePostfix() {
        int nameMatchIndex = 13;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null, null,
            TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(Integer.toString(13)).toLowerCase(Locale.getDefault()), null, null);
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities.get(nameMatchIndex).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithRequestedBySubstringMatchAll() {
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null, null,
            TransformedMediaSubStringQueryEnum.REQUESTED_BY.getQueryStringPostfix(), null, null);
        Assertions.assertEquals(GENERATION_COUNT, transformedMediaEntityList.size());
        Assertions.assertTrue(transformedMediaStub
                                  .getTransformedMediaIds(transformedMediaEntityList)
                                  .containsAll(transformedMediaStub.getTransformedMediaIds(generatedMediaEntities)));
    }

    @Test
    void testFindTransformedMediaWithRequestedAtFromAndRequestedAtToSameDay() {
        int fromAtPosition = 3;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null, null, null,
            generatedMediaEntities.get(fromAtPosition).getMediaRequest().getCreatedDateTime(),
            generatedMediaEntities.get(fromAtPosition).getMediaRequest().getCreatedDateTime());
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(generatedMediaEntities
                                    .get(fromAtPosition).getId(), transformedMediaEntityList.getFirst().getId());
    }

    @Test
    void testFindTransformedMediaWithRequestedAtFrom() {
        int fromAtPosition = 3;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(
            null, null, null, null, null,
            null, generatedMediaEntities.get(fromAtPosition).getMediaRequest().getCreatedDateTime(), null);
        Assertions.assertEquals(GENERATION_COUNT - fromAtPosition, transformedMediaEntityList.size());
        Assertions.assertTrue(transformedMediaStub
                                  .getTransformedMediaIds(transformedMediaEntityList)
                                  .containsAll(transformedMediaStub.getExpectedStartingFrom(fromAtPosition, generatedMediaEntities)));
    }

    @Test
    void testFindTransformedMediaWithRequestedAtTo() {
        int toPosition = 3;
        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(null, null,
                                                              null, null, null, null, null, generatedMediaEntities.get(toPosition).getCreatedDateTime());
        Assertions.assertEquals(toPosition + 1, transformedMediaEntityList.size());
        Assertions.assertTrue(transformedMediaStub
                                  .getTransformedMediaIds(transformedMediaEntityList)
                                  .containsAll(transformedMediaStub.getExpectedTo(toPosition, generatedMediaEntities)));
    }

    @Test
    void testFindTransformedMediaWithAllQueryParameters() {
        TransformedMediaEntity transformedMediaEntityFind = generatedMediaEntities.get(1);
        UserAccountEntity transformedMediaCreatedBy = dartsDatabase.getUserAccountRepository()
            .findById(transformedMediaEntityFind.getCreatedById()).orElseThrow();
        UserAccountEntity mediaRequestCreatedBy = dartsDatabase.getUserAccountRepository()
            .findById(transformedMediaEntityFind.getMediaRequest().getCreatedById()).orElseThrow();

        List<TransformedMediaEntity> transformedMediaEntityList
            = transformedMediaRepository.findTransformedMedia(transformedMediaEntityFind.getMediaRequest().getId(), transformedMediaEntityFind.getMediaRequest()
                                                                  .getHearing().getCourtCase().getCaseNumber(), transformedMediaEntityFind.getMediaRequest()
                                                                  .getHearing().getCourtroom().getCourthouse().getDisplayName(),
                                                              transformedMediaEntityFind.getMediaRequest()
                                                                  .getHearing().getHearingDate(), transformedMediaEntityFind.getMediaRequest()
                                                                  .getCurrentOwner().getUserFullName(),
                                                              transformedMediaCreatedBy.getUserFullName(),
                                                              mediaRequestCreatedBy.getCreatedDateTime(), generatedMediaEntities.get(1).getCreatedDateTime());
        Assertions.assertEquals(1, transformedMediaEntityList.size());
        Assertions.assertEquals(transformedMediaEntityFind.getId(), transformedMediaEntityList.getFirst().getId());
    }
}