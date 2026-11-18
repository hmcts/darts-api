// Standalone pipeline contract checks: run with groovy bin/ci/tests/playwright-e2e-test.groovy.
// Jenkins and GitHub are stubbed; these checks do not deploy, retrieve secrets or start browsers.
def loader = new GroovyClassLoader(getClass().classLoader)
loader.parseClass('''
package uk.gov.hmcts.contino
class AppPipelineConfig { def vaultSecrets }
class GithubAPI {
  def steps
  GithubAPI(steps) { this.steps = steps }
  def getLabelsbyPattern(branch, pattern) { steps.labels.findAll { it.startsWith(pattern) } }
  boolean checkForLabel(branch, label) { steps.labels.contains(label) }
}
''')
loader.parseClass('''
package uk.gov.hmcts.pipeline
class AgentSelector { static String labelForEnvironment(environment, env, product) { 'stg' } }
''')
loader.parseClass('''
package org.jenkinsci.plugins.workflow.steps
class FlowInterruptedException extends Exception {}
''')

String sha = 'a' * 40
String otherSha = 'b' * 40
String portal = 'https://darts-portal-api-pr-3503.dev.platform.hmcts.net'
String api = 'https://darts-api-pr-3503.dev.platform.hmcts.net'
int checks = 0
def pipeline = { List labels = [] ->
  def binding = new Binding([
      labels: labels,
      env: [BRANCH_NAME: 'PR-3503', CHANGE_ID: '3503', NODE_NAME: 'agent-1'],
      echo: { message -> },
      error: { message -> throw new IllegalArgumentException(message.toString()) },
      withEnvironmentAgent: { environment, product, body -> body() }
  ])
  new GroovyShell(loader, binding).parse(new File('bin/ci/playwright-e2e.groovy'))
}
def rejects = { String message, Closure operation ->
  try {
    operation()
  } catch (IllegalArgumentException failure) {
    assert failure.message.contains(message): failure.message
    checks++
    return
  }
  throw new AssertionError("Expected failure containing: ${message}")
}

def script = pipeline()
assert script.selectPlaywrightTicket([]) == null
assert script.selectPlaywrightTicket(['pw_e2e', 'pw_e2e_branch']) == null
assert script.selectPlaywrightTicket(['pw_e2e:264']) == '264'
assert script.selectPlaywrightTicket(['pw_e2e_branch:546']) == '546'
checks += 4
['pw_e2e:', 'pw_e2e:0', 'pw_e2e:0264', 'pw_e2e:264;echo unsafe',
 'pw_e2e_branch:abc', 'pw_e2e:264:extra'].each { label ->
  rejects('positive number') { script.selectPlaywrightTicket([label]) }
}
rejects('only one') { script.selectPlaywrightTicket(['pw_e2e:264', 'pw_e2e_branch:546']) }
rejects('only one') { script.selectPlaywrightTicket(['pw_e2e:264', 'pw_e2e_branch:264']) }

assert script.selectPlaywrightRevision('264', "${sha}\trefs/heads/dma-264-spike\n") ==
    [branch: 'dma-264-spike', sha: sha]
assert script.selectPlaywrightRevision('264', "${sha}\trefs/heads/DMA-264\n") ==
    [branch: 'DMA-264', sha: sha]
assert script.selectPlaywrightRevision('264', "${sha}\trefs/heads/dma-264-topic/child\n").sha == sha
checks += 3
rejects('found 0') { script.selectPlaywrightRevision('264', '') }
rejects('found 0') {
  script.selectPlaywrightRevision('264', "${sha}\trefs/heads/dma-2640-spike\n${sha}\trefs/tags/dma-264\n")
}
rejects('found 2') {
  script.selectPlaywrightRevision('264', "${sha}\trefs/heads/dma-264-first\n${otherSha}\trefs/heads/dma-264-second\n")
}
rejects('valid Playwright commit') { script.selectPlaywrightRevision('264', 'invalid refs/heads/dma-264') }

// Exercise the actual entry point: legacy labels, new labels, skip gates and deployment guards.
[[], ['unrelated'], ['pw_e2e'], ['pw_e2e_branch'], ['pw_e2e:264'],
 ['pw_e2e_branch:264'], ['pw_e2e', 'pw_e2e:264']].each { labels ->
  def entry = pipeline(labels)
  def calls = []
  entry.metaClass.runTests = { String environment, String portalUrl, String apiUrl, String ticket ->
    calls << [environment, portalUrl, apiUrl, ticket]
  }
  entry.call('stg', portal, api + '/')
  if (!labels || labels == ['unrelated']) {
    assert calls.empty
  } else {
    assert calls == [['stg', portal, api, labels.any { it.contains(':') } ? '264' : null]]
  }
  checks++
}
def fork = pipeline(['pw_e2e:invalid'])
fork.env.CHANGE_FORK = 'external-contributor'
fork.call('stg', portal, api) // Fork gate must run before label resolution.
checks++
def entry = pipeline(['pw_e2e:264'])
rejects('requires an API pull request number') {
  entry.env.CHANGE_ID = null
  entry.call('stg', portal, api)
}
entry.env.CHANGE_ID = '3503'
rejects('requires the portal and API for this PR') { entry.call('stg', portal, api.replace('3503', '3504')) }
rejects('requires the portal and API for this PR') { entry.call('stg', 'https://darts.staging.apps.hmcts.net', api) }

// Execute the orchestration with fake Jenkins steps, including both shard checkouts and report merge.
[null, '264', 'missing', 'ambiguous', 'mismatch'].each { scenario ->
  String ticket = scenario ? '264' : null
  def runner = pipeline()
  def checkouts = []
  def shards = []
  def commands = []
  runner.metaClass.checkoutPlaywright = { String revision -> checkouts << revision }
  runner.metaClass.executeShard = { String index, String environment, String portalUrl, String apiUrl, String shellRunner ->
    shards << [index, portalUrl, apiUrl]
  }
  [dir: { name, body -> body() }, timeout: { options, body -> body() },
   stage: { name, body -> body() }, node: { label, body -> body() },
   withEnv: { variables, body -> body() }, parallel: { branches -> branches.values().each { it() } },
   deleteDir: { -> }, readFile: { name -> 'existing shell runner' }, writeFile: { options -> },
   stash: { options -> }, unstash: { name -> }, archiveArtifacts: { options -> },
   publishHTML: { options -> },
   readJSON: { options -> [stats: [expected: 2, unexpected: 0, flaky: 0], errors: []] },
   sh: { options ->
     if (options instanceof Map) {
       commands << options.script
       if (options.script.contains('ls-remote')) {
         if (scenario == 'missing') {
           return ''
         }
         if (scenario == 'ambiguous') {
           return "${sha}\trefs/heads/dma-264-first\n${otherSha}\trefs/heads/dma-264-second\n"
         }
         return "${sha}\trefs/heads/dma-264-spike\n"
       }
       return scenario == 'mismatch' ? otherSha : sha
     }
     commands << options
   }].each { name, implementation -> runner.binding.setVariable(name, implementation) }
  if (scenario in ['missing', 'ambiguous', 'mismatch']) {
    String message = scenario == 'mismatch' ? 'differs from the selected' : 'Expected exactly one'
    rejects(message) { runner.runTests('stg', portal, api, ticket) }
    assert shards.empty
    assert !commands.contains('bash .jenkins-run-playwright.sh merge')
    assert checkouts == (scenario == 'mismatch' ? [sha] : [])
    return
  }
  runner.runTests('stg', portal, api, ticket)
  assert checkouts == [ticket ? sha : '*/dma-264-spike-pr-e2e-testing', sha]
  assert shards == [['1', portal, api], ['2', portal, api]]
  assert commands.count { it.contains('ls-remote') } == (ticket ? 1 : 0)
  assert commands.contains('bash .jenkins-run-playwright.sh merge')
  checks++
}
println "Passed ${checks} Playwright pipeline contract checks"
