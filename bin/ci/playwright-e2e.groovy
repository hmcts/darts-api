import groovy.json.JsonSlurperClassic
import uk.gov.hmcts.contino.AppPipelineConfig
import uk.gov.hmcts.contino.GithubAPI

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

  // Checkout runs on the general build agent; use an environment agent for network/Key Vault access.
  withEnvironmentAgent(environment, 'darts') {
    runTests(environment, portalUrl)
  }
}

def runTests(String environment, String portalUrl) {
  // Temporary spike override for both labels; restore mainline/ticket matching afterwards.
  String playwrightBranch = 'dma-264-spike-pr-e2e-testing'
  String runner = "${pwd()}/bin/ci/run-playwright.sh"
  dir('build/playwright-e2e') {
    deleteDir()
    try {
      timeout(time: 60, unit: 'MINUTES') {
        stage('Playwright setup') {
          checkout(changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: "*/${playwrightBranch}"]],
                    userRemoteConfigs: [[url: 'https://github.com/hmcts/darts-automation-playwright.git']]])
          echo "Playwright branch: ${playwrightBranch}; portal: ${portalUrl}"
          withEnv(["PLAYWRIGHT_RUNNER=${runner}"]) {
            sh 'bash "$PLAYWRIGHT_RUNNER" setup'
          }
        }
        withEnv(["PLAYWRIGHT_RUNNER=${runner}", "DARTS_PORTAL=${portalUrl}", 'CI=true', 'PW_SHARD_TOTAL=2']) {
          boolean shardFailed = false
          try {
            def config = new AppPipelineConfig()
            config.vaultSecrets = ['darts-${env}': [
                [secretType: 'Secret', name: 'AutomationTestUserName', version: '', envVariable: 'AUTOMATION_USERNAME'],
                [secretType: 'Secret', name: 'AutomationTestPassword', version: '', envVariable: 'AUTOMATION_PASSWORD']
            ]]
            withTeamSecrets(config, environment, 'darts') {
              def shards = [:]
              for (int index = 1; index <= 2; index++) {
                String shardIndex = index.toString()
                shards["Shard ${shardIndex}/2"] = {
                  stage("Playwright ${shardIndex}/2") {
                    withEnv(["PW_SHARD_INDEX=${shardIndex}"]) {
                      sh 'bash "$PLAYWRIGHT_RUNNER" test'
                    }
                  }
                }
              }
              parallel shards
            }
          } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException interrupted) {
            throw interrupted
          } catch (Exception failure) {
            shardFailed = true
            echo 'Playwright execution failed; collecting available reports before failing the build'
          }
          stage('Merge Playwright reports') {
            // A missing shard report also fails the build, even when a partial report can be published.
            sh 'bash "$PLAYWRIGHT_RUNNER" merge'
            def results = new JsonSlurperClassic().parseText(readFile('report-results.json'))
            def stats = results.stats
            if (stats.expected + stats.unexpected + stats.flaky == 0 || stats.unexpected > 0 || results.errors) {
              shardFailed = true
            }
          }
          if (shardFailed) {
            error('Playwright E2E failed; inspect the shard logs and combined report')
          }
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
