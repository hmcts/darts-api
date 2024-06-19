package uk.gov.hmcts.darts.testutils;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.IntegerRangeRandomizer;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;

@UtilityClass
public class CourtCaseTestData {

    public static CourtCaseEntity createCourtCaseAndAssociatedEntitiesWithRandomValues() {
        EasyRandomParameters parameters = new EasyRandomParameters()
            .randomize(Integer.class, new IntegerRangeRandomizer(1, 100))
            .collectionSizeRange(1, 1)
            .overrideDefaultInitialization(true);

        EasyRandom generator = new EasyRandom(parameters);
        return generator.nextObject(CourtCaseEntity.class);
    }
}
