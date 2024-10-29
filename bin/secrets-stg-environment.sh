#!/bin/bash

# To set the secrets in your shell, source this file ie. source ./bin/secrets-stg.sh

echo "Exporting secrets from Azure keyvault (darts-stg), please ensure you have \"az\" installed and you have logged in, using \"az login\"."

echo "GOVUK_NOTIFY_API_KEY=$(az keyvault secret show --vault-name darts-stg --name GovukNotifyTestApiKey | jq .value -r)"
echo "FUNC_TEST_ROPC_USERNAME=$(az keyvault secret show --vault-name darts-stg --name AzureAdB2CFuncTestROPCUsername | jq .value -r)"
echo "FUNC_TEST_ROPC_PASSWORD=$(az keyvault secret show --vault-name darts-stg --name AzureAdB2CFuncTestROPCPassword | jq .value -r)"
echo "AAD_B2C_TENANT_ID=$(az keyvault secret show --vault-name darts-stg --name AzureAdB2CTenantId | jq .value -r)"
echo "AAD_B2C_CLIENT_ID=$(az keyvault secret show --vault-name darts-stg --name AzureAdB2CClientId | jq .value -r)"
echo "AAD_B2C_CLIENT_SECRET=$(az keyvault secret show --vault-name darts-stg --name AzureAdB2CClientSecret | jq .value -r)"
echo "AAD_B2C_ROPC_CLIENT_ID=$(az keyvault secret show --vault-name darts-stg --name AzureAdB2CFuncTestROPCClientId | jq .value -r)"
echo "AAD_B2C_ROPC_CLIENT_SECRET=$(az keyvault secret show --vault-name darts-stg --name AzureAdB2CFuncTestROPCClientSecret | jq .value -r)"
echo "AZURE_STORAGE_CONNECTION_STRING=$(az keyvault secret show --vault-name darts-stg --name AzureStorageConnectionString | jq .value -r)"
echo "AAD_TENANT_ID=$(az keyvault secret show --vault-name darts-stg --name AzureADTenantId | jq .value -r)"
echo "AAD_CLIENT_ID=$(az keyvault secret show --vault-name darts-stg --name AzureADClientId | jq .value -r)"
echo "AAD_CLIENT_SECRET=$(az keyvault secret show --vault-name darts-stg --name AzureADClientSecret | jq .value -r)"
echo "XHIBIT_USER_NAME=$(az keyvault secret show --vault-name darts-stg --name XhibitUserName | jq .value -r)"
echo "XHIBIT_PASSWORD=$(az keyvault secret show --vault-name darts-stg --name XhibitPassword | jq .value -r)"
echo "CPP_USER_NAME=$(az keyvault secret show --vault-name darts-stg --name CppUserName | jq .value -r)"
echo "CPP_PASSWORD=$(az keyvault secret show --vault-name darts-stg --name CppPassword | jq .value -r)"
echo "DARPC_USER_NAME=$(az keyvault secret show --vault-name darts-stg --name DarPcUserName | jq .value -r)"
echo "DARPC_PASSWORD=$(az keyvault secret show --vault-name darts-stg --name DarPcPassword | jq .value -r)"
echo "SYSTEM_USER_EMAIL=$(az keyvault secret show --vault-name darts-stg --name DartsSystemUserEmail | jq .value -r)"
echo "DAR_MIDTIER_USER_NAME=$(az keyvault secret show --vault-name darts-stg --name DarMidTierUserName | jq .value -r)"
echo "DAR_MIDTIER_PASSWORD=$(az keyvault secret show --vault-name darts-stg --name DarMidTierPassword | jq .value -r)"
echo "AZURE_AD_FUNCTIONAL_TEST_GLOBAL_USERNAME=$(az keyvault secret show --vault-name darts-stg --name AzureAdB2CFuncTestROPCGlobalUsername | jq .value -r)"
echo "AZURE_AD_FUNCTIONAL_TEST_GLOBAL_PASSWORD=$(az keyvault secret show --vault-name darts-stg --name AzureAdB2CFuncTestROPCGlobalPassword | jq .value -r)"
echo "AZURE_AD_FUNCTIONAL_TEST_USERNAME=$(az keyvault secret show --vault-name darts-stg --name AzureADFunctionalTestUsername | jq .value -r)"
echo "AZURE_AD_FUNCTIONAL_TEST_PASSWORD=$(az keyvault secret show --vault-name darts-stg --name AzureADFunctionalTestPassword | jq .value -r)"
echo "ARM_SAS_ENDPOINT=$(az keyvault secret show --vault-name darts-stg --name ARMSasEndpoint | jq .value -r)"
echo "ARM_USERNAME=$(az keyvault secret show --vault-name darts-stg --name ArmUsername | jq .value -r)"
echo "ARM_PASSWORD=$(az keyvault secret show --vault-name darts-stg --name ArmPassword | jq .value -r)"
echo "ARM_URL=$(az keyvault secret show --vault-name darts-stg --name ArmUrl | jq .value -r)"
echo "DETS_SAS_URL_ENDPOINT=$(az keyvault secret show --vault-name darts-stg --name DETSSasURLEndpoint | jq .value -r)"
echo "AAD_TENANT_ID_JUSTICE=$(az keyvault secret show --vault-name darts-stg --name AzureADTenantIdJustice | jq .value -r)"
echo "AAD_CLIENT_ID_JUSTICE=$(az keyvault secret show --vault-name darts-stg --name AzureADClientIdJustice | jq .value -r)"
echo "AAD_CLIENT_SECRET_JUSTICE=$(az keyvault secret show --vault-name darts-stg --name AzureADClientSecretJustice | jq .value -r)"
echo "MAX_FILE_UPLOAD_SIZE_MEGABYTES=$(az keyvault secret show --vault-name darts-stg --name MaxFileUploadSizeInMegabytes | jq .value -r)"
echo "MAX_FILE_UPLOAD_REQUEST_SIZE_MEGABYTES=$(az keyvault secret show --vault-name darts-stg --name MaxFileUploadRequestSizeInMegabytes | jq .value -r)"
echo "DARTS_INBOUND_STORAGE_SAS_URL=$(az keyvault secret show --vault-name darts-stg --name DartsInboundStorageSasUrl | jq .value -r)"
echo "DARTS_UNSTRUCTURED_STORAGE_SAS_URL=$(az keyvault secret show --vault-name darts-stg --name DartsUnstructuredStorageSasUrl | jq .value -r)"
echo "ARM_SERVICE_PROFILE=$(az keyvault secret show --vault-name darts-stg --name ArmServiceProfile | jq .value -r)"
echo "ACTIVE_DIRECTORY_B2C_AUTH_URI=https://hmctsstgextid.b2clogin.com/hmctsstgextid.onmicrosoft.com"
echo "DARTS_PORTAL_URL=http://localhost:3000"
echo "ARM_SERVICE_ENTITLEMENT=$(az keyvault secret show --vault-name darts-stg --name ArmServiceEntitlement | jq .value -r)"
echo "ARM_STORAGE_ACCOUNT_NAME=$(az keyvault secret show --vault-name darts-stg --name ArmStorageAccountName | jq .value -r)"

