# DARTS Skills

This folder holds reusable Codex skill guides for `darts-api`. Add future skills as separate subfolders so each guide can
keep its instructions, examples, and README together.

## Available Skills

- [Automated Task Generator](automated-task-generator/README.md): creates the standard DARTS automated-task wiring,
  including Flyway data, task configuration, runner classes, and a domain service stub.

## Folder Convention

Use one folder per skill:

```text
skills/
  <skill-name>/
    README.md
    <skill-name>.md
```

Keep each skill focused on one repeatable workflow. Put detailed examples in the skill folder rather than the repository
root.
