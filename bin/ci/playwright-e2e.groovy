import uk.gov.hmcts.contino.AppPipelineConfig
import uk.gov.hmcts.contino.GithubAPI
import uk.gov.hmcts.pipeline.AgentSelector

def call(String environment, String portalUrl) {
  if (env.CHANGE_FORK) {
    echo 'Skipping Playwright E2E for a fork pull request'
    return
  }
  def githubApi = new GithubAPI(this)
  if (!githubApi.checkForLabel(env.BRANCH_NAME, 'pw_e2e')
      && !githubApi.checkForLabel(env.BRANCH_NAME, 'pw_e2e_branch')) {
    echo 'No Playwright E2E label: skipping checkout and tests'
    return
  }
  if (!(portalUrl ==~ /https:\/\/(darts\.staging\.apps\.hmcts\.net|darts-portal-api-pr-[0-9]+\.dev\.platform\.hmcts\.net)/)) {
    error('Playwright requires the staging portal or a DARTS API PR portal HTTPS origin')
  }

  // Use the requested environment's agents for network/Key Vault access.
  withEnvironmentAgent(environment, 'darts') {
    runTests(environment, portalUrl)
  }
}

def checkoutPlaywright(String revision) {
  checkout(changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: revision]],
            userRemoteConfigs: [[url: 'https://github.com/hmcts/darts-automation-playwright.git']]])
}

def executeShard(String index, String environment, String portalUrl, String runner) {
  echo "Playwright shard ${index}/2 on agent ${env.NODE_NAME}"
  writeFile file: '.jenkins-run-playwright.sh', text: runner
  withEnv(["DARTS_PORTAL=${portalUrl}", 'CI=true', 'PW_SHARD_TOTAL=2', "PW_SHARD_INDEX=${index}"]) {
    sh 'bash .jenkins-run-playwright.sh setup'
    def config = new AppPipelineConfig()
    config.vaultSecrets = ['darts-${env}': [
        [secretType: 'Secret', name: 'AutomationTestUserName', version: '', envVariable: 'AUTOMATION_USERNAME'],
        [secretType: 'Secret', name: 'AutomationTestPassword', version: '', envVariable: 'AUTOMATION_PASSWORD']
    ]]
    withTeamSecrets(config, environment, 'darts') {
      sh 'bash .jenkins-run-playwright.sh test'
    }
  }
}

def runTests(String environment, String portalUrl) {
  // Temporary spike override for both labels; restore mainline/ticket matching afterwards.
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
          checkoutPlaywright("*/${playwrightBranch}")
          playwrightSha = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
          if (!(playwrightSha ==~ /[0-9a-f]{40}/)) {
            error('Could not resolve a Playwright commit for both shards')
          }
          echo "Playwright branch: ${playwrightBranch}; commit: ${playwrightSha}; portal: ${portalUrl}"
        }
        boolean shardFailed = false
        try {
          parallel(
              'Shard 1/2': {
                stage('Playwright 1/2') {
                  executeShard('1', environment, portalUrl, runner)
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
                          executeShard('2', environment, portalUrl, runner)
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
