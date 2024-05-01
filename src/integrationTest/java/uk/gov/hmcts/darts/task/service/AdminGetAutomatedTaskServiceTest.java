package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.ExcessiveImports")
class AdminGetAutomatedTaskServiceTest extends IntegrationBase {

    private static final Integer[] TASK_IDS__IN_ORDER = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final String[] TASK_NAMES__IN_ORDER = {
        "ProcessDailyList",
        "CloseOldUnfinishedTranscriptions",
        "OutboundAudioDeleter",
        "InboundAudioDeleter",
        "ExternalDataStoreDeleter",
        "InboundToUnstructuredDataStore",
        "UnstructuredAudioDeleter",
        "UnstructuredToArmDataStore",
        "ProcessArmResponseFiles",
        "CleanupArmResponseFiles",
        "ApplyRetention",
        "CloseOldCases",
        "DailyListHousekeeping",
        "ArmRetentionEventDateCalculator",
        "ApplyRetentionCaseAssociatedObjects"};
    private static final String[] TASK_DESCRIPTIONS__IN_ORDER = {
        "Process the latest daily list for each courthouse",
        "Close transcriptions that are old and not in a finished state",
        "Marks for deletion audio that is stored in outbound that was last accessed a certain number of days a ago.",
        "Marks for deletion audio that is stored in outbound that has been successfully uploaded to ARM.",
        "Deletes data marked for deletion in inbound, unstructured, outbound datastores",
        "Move Inbound files to Unstructured data store",
        "Marks data for deletion in unstructured data stores that have been in ARM for a set time",
        "Move files from Unstructured to ARM data store",
        "Processes ARM response files",
        "Cleans up ARM response files",
        "Apply retention after 7 days",
        "Closes cases over 6 years old",
        "Deletes daily lists older than 30 days",
        "Sets the retention event date for ARM records",
        "Apply retention to case associated objects"};
    private static final String[] TASK_CRON__IN_ORDER = {
        "0 5 2 * * *",
        "0 20 11 * * *",
        "0 1 * * * *",
        "0 4 22 * * *",
        "0 0 20 * * *",
        "0 0/5 * * * *",
        "0 0 22 * * *",
        "0 0/5 * * * *",
        "0 0/10 * * * *",
        "0 0 21 * * *",
        "0 0 * * * *",
        "0 0 0 L * *",
        "0 30 16 * * *",
        "0 0 22 * * *",
        "0 0 20 * * *"};
    private static final Boolean[] TASK_IS_ACTIVE__IN_ORDER = {
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true
    };

    @Autowired
    private AdminAutomatedTaskService adminAutomatedTaskService;

    @Test
    void findsAllAutomatedTasks() {
        var automatedTasks = adminAutomatedTaskService.getAllAutomatedTasks();

        assertThat(automatedTasks).extracting("id").isEqualTo(asList(TASK_IDS__IN_ORDER));
        assertThat(automatedTasks).extracting("name").isEqualTo(asList(TASK_NAMES__IN_ORDER));
        assertThat(automatedTasks).extracting("description").isEqualTo(asList(TASK_DESCRIPTIONS__IN_ORDER));
        assertThat(automatedTasks).extracting("cronExpression").isEqualTo(asList(TASK_CRON__IN_ORDER));
        assertThat(automatedTasks).extracting("isActive").isEqualTo(asList(TASK_IS_ACTIVE__IN_ORDER));
    }
}