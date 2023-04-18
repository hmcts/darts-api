locals {
  env = (var.env == "aat") ? "stg" : (var.env == "sandbox") ? "sbox" : "${(var.env == "perftest") ? "test" : "${var.env}"}"

  env_subdomain = local.env_long_name == "prod" ? "" : "${local.env_long_name}."
  base_url      = "${var.product}-${var.component}.${local.env_subdomain}platform.hmcts.net"

  apim_name     = "sds-api-mgmt-${local.env}"
  apim_rg       = "ss-${local.env}-network-rg"
  env_long_name = var.env == "sbox" ? "sandbox" : var.env == "stg" ? "staging" : var.env

  deploy_apim = local.env == "stg" || local.env == "sbox" || local.env == "prod" ? 1 : 0

  prefix            = "${var.product}-ss"
  prefix_no_special = replace(local.prefix, "-", "")
}
