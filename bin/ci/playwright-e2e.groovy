import uk.gov.hmcts.contino.AppPipelineConfig
import uk.gov.hmcts.contino.GithubAPI
import uk.gov.hmcts.pipeline.AgentSelector

def call(String environment, String portalUrl, String apiUrl) {
  if (env.CHANGE_FORK) {
    echo 'Skipping Playwright E2E for a fork pull request'
    return
  }
  def githubApi = new GithubAPI(this)
  boolean selectTicketBranch = githubApi.checkForLabel(env.BRANCH_NAME, 'pw_e2e_branch')
  if (!selectTicketBranch && !githubApi.checkForLabel(env.BRANCH_NAME, 'pw_e2e')) {
    echo 'No Playwright E2E label: skipping checkout and tests'
    return
  }
  if (!(env.CHANGE_ID ==~ /[1-9][0-9]*/)) {
    error('Playwright data setup requires an API pull request number')
  }
  String expectedPortal = "https://darts-portal-api-pr-${env.CHANGE_ID}.dev.platform.hmcts.net"
  String expectedApi = "https://darts-api-pr-${env.CHANGE_ID}.dev.platform.hmcts.net"
  if (portalUrl != expectedPortal || apiUrl?.replaceFirst('/$', '') != expectedApi) {
    error('Playwright requires the portal and API for this PR; enable_keep_helm and enable_darts_portal are required')
  }
  // BRANCH_NAME is PR-3503 in a PR job; CHANGE_BRANCH is the actual source branch.
  String ticket = selectTicketBranch ? selectPlaywrightTicket(env.CHANGE_BRANCH) : null
  if (ticket) {
    echo "Playwright branch selection: pw_e2e_branch; API source branch: ${env.CHANGE_BRANCH}; ticket: DMA-${ticket}"
  }

  // Use the requested environment's agents for network/Key Vault access.
  withEnvironmentAgent(environment, 'darts') {
    runTests(environment, portalUrl, expectedApi, ticket)
  }
}

// Parse the ticket boundary, not a fixed number of characters: DMA-9 and DMA-12345 work.
String selectPlaywrightTicket(String sourceBranch) {
  if (!(sourceBranch ==~ /(?i)dma-[1-9][0-9]*(-.+)?/)) {
    error('pw_e2e_branch requires an API source branch named dma-<ticket> or dma-<ticket>-<description>; check CHANGE_BRANCH')
  }
  return sourceBranch.split('-')[1]
}

Map selectPlaywrightRevision(String ticket, String remoteHeads) {
  // Only complete ticket boundaries match: DMA-264 must not select DMA-2640.
  def matches = remoteHeads.readLines().collect { line -> line.tokenize() }.findAll { fields ->
    fields.size() == 2 && fields[1] ==~ /(?i)refs\/heads\/dma-${ticket}(-.+)?/
  }
  if (matches.size() != 1) {
    error("Expected exactly one Playwright branch for DMA-${ticket}; found ${matches.size()}. " +
          'Keep one dma-<ticket> or dma-<ticket>-<description> branch and retry; no fallback was used.')
  }
  String sha = matches[0][0]
  if (!(sha ==~ /[0-9a-f]{40}/)) {
    error('Could not resolve a valid Playwright commit from the remote branch')
  }
  return [branch: matches[0][1].substring('refs/heads/'.length()), sha: sha]
}

def checkoutPlaywright(String revision) {
  checkout(changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: revision]],
            userRemoteConfigs: [[url: 'https://github.com/hmcts/darts-automation-playwright.git']]])
}

def executeShard(String index, String environment, String portalUrl, String apiUrl, String runner) {
  echo "Playwright shard ${index}/2 on agent ${env.NODE_NAME}"
  writeFile file: '.jenkins-run-playwright.sh', text: runner
  withEnv(["DARTS_PORTAL=${portalUrl}", "DARTS_API=${apiUrl}", 'E2E_LOGIN_ROUTE=external',
           'CI=true', 'PW_SHARD_TOTAL=2', "PW_SHARD_INDEX=${index}"]) {
    sh 'bash .jenkins-run-playwright.sh setup'
    def config = new AppPipelineConfig()
    config.vaultSecrets = ['darts-${env}': [
        [secretType: 'Secret', name: 'AutomationTestUserName', version: '', envVariable: 'AUTOMATION_USERNAME'],
        [secretType: 'Secret', name: 'AutomationTestPassword', version: '', envVariable: 'AUTOMATION_PASSWORD'],
        [secretType: 'Secret', name: 'AzureAdB2CFuncTestROPCClientId', version: '', envVariable: 'AAD_B2C_ROPC_CLIENT_ID'],
        [secretType: 'Secret', name: 'AzureAdB2CFuncTestROPCClientSecret', version: '', envVariable: 'AAD_B2C_ROPC_CLIENT_SECRET'],
        [secretType: 'Secret', name: 'AzureAdB2CFuncTestROPCGlobalUsername', version: '', envVariable: 'AZURE_AD_FUNCTIONAL_TEST_GLOBAL_USERNAME'],
        [secretType: 'Secret', name: 'AzureAdB2CFuncTestROPCGlobalPassword', version: '', envVariable: 'AZURE_AD_FUNCTIONAL_TEST_GLOBAL_PASSWORD'],
        [secretType: 'Secret', name: 'DartsInboundStorageSasUrl', version: '', envVariable: 'E2E_AUDIO_INBOUND_SAS_URL']
    ]]
    withTeamSecrets(config, environment, 'darts') {
      sh 'bash .jenkins-run-playwright.sh test'
    }
  }
}

def runTests(String environment, String portalUrl, String apiUrl, String ticket = null) {
  // pw_e2e keeps the existing spike branch; pw_e2e_branch selects the source ticket.
  String playwrightBranch = 'dma-264-spike-pr-e2e-testing'
  String runner = readFile('bin/ci/run-playwright.sh')
  String firstAgent = env.NODE_NAME
  String agentLabel = AgentSelector.labelForEnvironment(environment, env, 'darts')
  // A second executor on the same machine would not distribute the browser workload.
  String escapedAgent = firstAgent.replace('\\', '\\\\').replace('"', '\\"')
  String secondAgentLabel = "(${agentLabel}) && !\"${escapedAgent}\""
  dir('build/playwright-e2e') {
    deleteDir()
    try {
      timeout(time: 60, unit: 'MINUTES') {
        String playwrightSha
        stage('Resolve Playwright revision') {
          String requestedSha
          if (ticket) {
            // The URL and shell command are fixed; source branch text is never shell code.
            String remoteHeads = sh(
                script: 'GIT_TERMINAL_PROMPT=0 git ls-remote --heads https://github.com/hmcts/darts-automation-playwright.git',
                returnStdout: true)
            def revision = selectPlaywrightRevision(ticket, remoteHeads)
            playwrightBranch = revision.branch
            requestedSha = revision.sha
          }
          checkoutPlaywright(requestedSha ?: "*/${playwrightBranch}")
          playwrightSha = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
          if (!(playwrightSha ==~ /[0-9a-f]{40}/)) {
            error('Could not resolve a Playwright commit for both shards')
          }
          if (requestedSha && playwrightSha != requestedSha) {
            error('Checked-out Playwright commit differs from the selected ticket branch revision')
          }
          echo "Playwright branch: ${playwrightBranch}; commit: ${playwrightSha}; portal: ${portalUrl}; API: ${apiUrl}"
        }
        boolean shardFailed = false
        try {
          parallel(
              'Shard 1/2': {
                stage('Playwright 1/2') {
                  executeShard('1', environment, portalUrl, apiUrl, runner)
                }
              },
              'Shard 2/2': {
                stage('Playwright 2/2') {
                  node(secondAgentLabel) {
                    withEnv(["BUILD_AGENT_TYPE=${agentLabel}", "DEPLOYMENT_ENVIRONMENT=${environment}",
                             'BUILD_AGENT_CONTAINER=', 'IS_DOCKER_BUILD_AGENT=false']) {
                      dir('build/playwright-e2e-shard-2') {
                        deleteDir()
                        try {
                          checkoutPlaywright(playwrightSha)
                          executeShard('2', environment, portalUrl, apiUrl, runner)
                        } finally {
                          try {
                            stash name: 'playwright-e2e-shard-2', includes: 'shard-2/blob-report/*.zip', allowEmpty: true
                            archiveArtifacts artifacts: 'shard-2/blob-report/*.zip,shard-2/test-results/**', allowEmptyArchive: true
                          } finally {
                            deleteDir()
                          }
                        }
                      }
                    }
                  }
                }
              }
          )
        } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException interrupted) {
          throw interrupted
        } catch (Exception failure) {
          shardFailed = true
          echo 'Playwright execution failed; collecting available reports before failing the build'
        }
        stage('Merge Playwright reports') {
          // Allocation can fail before shard 2 creates its stash; merge still checks for both reports.
          try {
            unstash 'playwright-e2e-shard-2'
          } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException interrupted) {
            throw interrupted
          } catch (Exception missingStash) {
            echo 'Could not collect the second shard report; the merge will fail if it is missing'
          }
          writeFile file: '.jenkins-run-playwright.sh', text: runner
          withEnv(['PW_SHARD_TOTAL=2']) {
            sh 'bash .jenkins-run-playwright.sh merge'
          }
          def results = readJSON(file: 'report-results.json', returnPojo: true)
          def stats = results.stats
          if (stats.expected + stats.unexpected + stats.flaky == 0 || stats.unexpected > 0 || results.errors) {
            shardFailed = true
          }
        }
        if (shardFailed) {
          error('Playwright E2E failed; inspect the shard logs and combined report')
        }
      }
    } finally {
      try {
        archiveArtifacts artifacts: 'playwright-report/**,report-results.json,shard-*/blob-report/*.zip,shard-*/test-results/**',
                         allowEmptyArchive: true
        publishHTML target: [allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true,
                             reportDir: 'playwright-report', reportFiles: 'index.html', reportName: 'Playwright E2E']
      } finally {
        deleteDir()
      }
    }
  }
}

return this
