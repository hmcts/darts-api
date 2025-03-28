package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestCourtCaseEntity;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import static java.time.OffsetDateTime.now;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.DefenceTestData.createDefenceForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.ProsecutorTestData.createProsecutorForCaseWithName;

public final class CaseTestData implements Persistable<TestCourtCaseEntity.TestCourtCaseBuilderRetrieve,
    CourtCaseEntity, TestCourtCaseEntity.TestCourtCaseEntityBuilder> {
    
    CaseTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public CourtCaseEntity createSomeMinimalCase() {
        var postfix = random(10, false, true);
        var courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setCourthouse(someMinimalCourthouse());
        courtCaseEntity.setCaseNumber("case-1-" + postfix);
        courtCaseEntity.setClosed(false);
        courtCaseEntity.setInterpreterUsed(false);
        courtCaseEntity.setCreatedById(0);
        courtCaseEntity.setLastModifiedById(0);
        return courtCaseEntity;
    }

    public CourtCaseEntity caseWithCaseNumber(String caseNumber) {
        var someMinimalCase = createSomeMinimalCase();
        someMinimalCase.setCaseNumber(caseNumber);
        return someMinimalCase;
    }

    public CourtCaseEntity createCaseWith(String caseNumber, CourthouseEntity courthouseEntity) {
        var courtCaseEntity = createSomeMinimalCase();

        courtCaseEntity.setCourthouse(Objects.requireNonNullElseGet(courthouseEntity, CourthouseTestData::someMinimalCourthouse));

        courtCaseEntity.setCaseNumber(caseNumber);
        courtCaseEntity.setClosed(false);
        courtCaseEntity.setInterpreterUsed(false);
        return courtCaseEntity;
    }

    @Deprecated
    // Not a minimal case. refactor
    public CourtCaseEntity someMinimalCase() {
        var courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setCaseNumber("case-1");
        courtCaseEntity.setCourthouse(someMinimalCourthouse());
        courtCaseEntity.addDefendant(createDefendantForCaseWithName(courtCaseEntity, "aDefendant"));
        courtCaseEntity.addDefence(createDefenceForCaseWithName(courtCaseEntity, "aDefence"));
        courtCaseEntity.addProsecutor(createProsecutorForCaseWithName(courtCaseEntity, "aProsecutor"));
        courtCaseEntity.setClosed(false);
        courtCaseEntity.setInterpreterUsed(false);
        courtCaseEntity.setCreatedById(0);
        courtCaseEntity.setLastModifiedById(0);
        return courtCaseEntity;
    }

    /**
     * Creates a CourtCaseEntity. Passes the created case to the client for further customisations
     */
    public CourtCaseEntity someMinimalCase(Consumer<CourtCaseEntity> createdCaseConsumer) {
        var courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setCaseNumber("case-1");
        courtCaseEntity.setCourthouse(someMinimalCourthouse());
        courtCaseEntity.addDefendant(createDefendantForCaseWithName(courtCaseEntity, "aDefendant"));
        courtCaseEntity.addDefence(createDefenceForCaseWithName(courtCaseEntity, "aDefence"));
        courtCaseEntity.addProsecutor(createProsecutorForCaseWithName(courtCaseEntity, "aProsecutor"));
        courtCaseEntity.setClosed(false);
        courtCaseEntity.setInterpreterUsed(false);
        createdCaseConsumer.accept(courtCaseEntity);
        return courtCaseEntity;
    }

    public CourtCaseEntity createCaseAt(CourthouseEntity courthouse) {
        var courtCase = someMinimalCase();
        courtCase.setCourthouse(courthouse);
        return courtCase;
    }

    public CourtCaseEntity createCaseAt(CourthouseEntity courthouse, String caseNumber) {
        var courtCase = someMinimalCase();
        courtCase.setCourthouse(courthouse);
        courtCase.setCaseNumber(caseNumber);
        return courtCase;
    }

    public CourtCaseEntity createCaseWithCaseNumber(String caseNumber) {
        var courtCase = someMinimalCase();
        courtCase.setCaseNumber(caseNumber);
        return courtCase;
    }

    @Override
    public CourtCaseEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestCourtCaseEntity.TestCourtCaseEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

    @Override
    public TestCourtCaseEntity.TestCourtCaseBuilderRetrieve someMinimalBuilderHolder() {
        TestCourtCaseEntity.TestCourtCaseBuilderRetrieve retrieve
            = new TestCourtCaseEntity.TestCourtCaseBuilderRetrieve();

        var postfix = random(10, false, true);
        retrieve.getBuilder().courthouse(someMinimalCourthouse())
            .caseNumber("case-1-" + postfix)
            .closed(false)
            .interpreterUsed(false)
            .createdById(0)
            .lastModifiedById(0)
            .retentionUpdated(false)
            .deleted(false)
            .dataAnonymised(false).defenceList(new ArrayList<>())
            .defendantList(new ArrayList<>()).prosecutorList(new ArrayList<>())
            .createdDateTime(now());
        return retrieve;
    }

}