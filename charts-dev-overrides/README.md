# Dev chart overrides

This directory contains overrides for the files in the `charts` directory.

These overrides are used to replace the files in the development environment, when specific labels are set on the PR.

This happens at the start of the AKS deploy dev stage in the pipeline.

_Note: the `enable_keep_helm` label must also be set on the PR for these overrides to be used._

## Usage

Set a label on a PR, corresponding to one of the directories in here. For example, set the `enable_darts_portal` label and the files in the `enable_darts_portal` directory will be used to replace the files in the `charts` directory. This changes the dev deployment and adds a DARTS portal instance.

## Supported labels

| Label                  | Usages                                                                                           |
|------------------------|--------------------------------------------------------------------------------------------------|
| enable_darts_portal    | Deploys a DARTS portal instance alongside the API in the dev environment                         |
| enable_darts_fullstack | Not yet supported, but will deploy the full DARTS stack alongside the API in the dev environment |