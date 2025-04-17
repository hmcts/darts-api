package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.jeasy.random.DefaultExclusionPolicy;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.ExclusionPolicy;
import org.jeasy.random.api.RandomizerContext;
import org.jeasy.random.randomizers.range.IntegerRangeRandomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class CourtCaseStub {

    private static final LocalDateTime D_2020_10_1 = LocalDateTime.of(2020, 10, 1, 12, 0, 0);
    private static final LocalDateTime D_2020_10_2 = LocalDateTime.of(2020, 10, 2, 12, 0, 0);

    @Autowired
    HearingStubComposable hearingStub;

    @Autowired
    CaseRepository caseRepository;

    @Autowired
    CourthouseRepository courthouseRepository;

    @Autowired
    UserAccountStub userAccountStub;

    @Autowired
    CourthouseStub courthouseStub;

    @Autowired
    DartsDatabaseSaveStub dartsDatabaseSaveStub;

    @Autowired
    DartsPersistence dartsPersistence;

    @Transactional
    public CourtCaseEntity createAndSaveMinimalCourtCase() {
        var courtCase = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();
        return dartsPersistence.save(courtCase);
    }

    @Transactional
    public CourtCaseEntity createAndSaveMinimalCourtCase(String caseNumber, Integer courthouseId) {
        var courtCase = PersistableFactory.getCourtCaseTestData().createCaseWith(caseNumber, courthouseRepository.findById(courthouseId).get());
        return dartsPersistence.save(courtCase);
    }

    public CourtCaseEntity createAndSaveMinimalCourtCase(String caseNumber, CourthouseEntity courthouse) {
        var courtCase = PersistableFactory.getCourtCaseTestData().createCaseWith(caseNumber, courthouse);
        return dartsPersistence.save(courtCase);
    }

    /**
     * Creates a CourtCaseEntity. Passes the created case to the client for further customisations before saving
     */
    @Transactional
    public CourtCaseEntity createAndSaveCourtCase(Consumer<CourtCaseEntity> caseConsumer) {

        var courtCase = createAndSaveMinimalCourtCase();
        caseConsumer.accept(courtCase);
        return dartsDatabaseSaveStub.save(courtCase);
    }

    /**
     * Creates a CourtCaseEntity with 3 hearings. Passes the created case to the client for further customisations before saving
     */
    @Transactional
    public CourtCaseEntity createAndSaveCourtCaseWithHearings(Consumer<CourtCaseEntity> caseConsumer) {
        var courtCase = createAndSaveMinimalCourtCase();
        caseConsumer.accept(courtCase);

        var courthouseName = courtCase.getCourthouse().getCourthouseName();
        var hear1 = hearingStub.createHearing(courthouseName, "testCourtroom", courtCase.getCaseNumber(), D_2020_10_1, courthouseStub, userAccountStub);
        var hear2 = hearingStub.createHearing(courthouseName, "testCourtroom2", courtCase.getCaseNumber(), D_2020_10_1, courthouseStub, userAccountStub);
        var hear3 = hearingStub.createHearing(courthouseName, "testCourtroom", courtCase.getCaseNumber(), D_2020_10_2, courthouseStub, userAccountStub);
        courtCase.getHearings().addAll(List.of(hear1, hear2, hear3));

        return dartsDatabaseSaveStub.save(courtCase);
    }

    @Transactional
    public CourtCaseEntity createAndSaveCourtCaseWithHearings() {
        return createAndSaveCourtCaseWithHearings(courtCase -> {
        });
    }

    @Transactional
    public void createCasesWithHearings(int numOfCases, int numOfCourtrooms, int numOfHearingsPerCourtroom) {
        CourthouseEntity courthouse = courthouseStub.createMinimalCourthouse();
        for (int caseCounter = 1; caseCounter <= numOfCases; caseCounter++) {
            CourtCaseEntity courtCase = createAndSaveMinimalCourtCase("caseNumber" + caseCounter, courthouse);
            hearingStub.createHearingsForCase(courtCase, numOfCourtrooms, numOfHearingsPerCourtroom, courthouseStub, userAccountStub);
        }
    }

    /**
     * Creates a CourtCaseEntity with random values. Passes the created case to the client for further customisations before saving
     *
     * @deprecated use new PersistableFactory builders
     */
    @Deprecated
    public CourtCaseEntity createCourtCaseAndAssociatedEntitiesWithRandomValues() {
        EasyRandomParameters parameters = new EasyRandomParameters()
            .randomize(Integer.class, new IntegerRangeRandomizer(1, 100))
            .collectionSizeRange(1, 1)
            .excludeType(aClass -> aClass.equals(MediaLinkedCaseEntity.class))
            .overrideDefaultInitialization(true);

        parameters.setExclusionPolicy(new ExclusionPolicy() {
            final DefaultExclusionPolicy defaultExclusionPolicy = new DefaultExclusionPolicy();

            @Override
            public boolean shouldBeExcluded(Field field, RandomizerContext context) {
                return (context.getCurrentObject().getClass().equals(ObjectAdminActionEntity.class)
                    && field.getType().equals(MediaEntity.class))
                    || defaultExclusionPolicy.shouldBeExcluded(field, context);
            }

            @Override
            public boolean shouldBeExcluded(Class<?> type, RandomizerContext context) {
                return defaultExclusionPolicy.shouldBeExcluded(type, context);
            }
        });

        EasyRandom generator = new EasyRandom(parameters);
        return generator.nextObject(CourtCaseEntity.class);
    }

    public CourtCaseEntity getCourtCase(int id) {
        return caseRepository.findById(id).orElseThrow();
    }
}