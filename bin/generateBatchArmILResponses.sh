#!/bin/bash
echo "This script generates the failure path of ARM pull response files. These can be used for testing where ARM reports a failure for an object such as media, transcription document, annotation document or case document to be stored in the ARM storage"

uuid1=$(uuidgen)

echo "uuid 1: $uuid1"

read -p 'enter manifestName without .a360: ' manifestName
if [ -z "$manifestName" ]
then
  exit 1
fi

mkdir -p arm_responses
pushd arm_responses

iuFilename="RESPONSE_FILENAME_UUID1_1_iu.rsp"
iuFilename=${iuFilename//RESPONSE_FILENAME/$manifestName}
iuFilename=${iuFilename//UUID1/$uuid1}
echo "IU filename: $iuFilename"


addNewResponses () {
  uuid2=$(uuidgen)
  uuid3=$(uuidgen)
  uuid4=$(uuidgen)
  uuid5=$(uuidgen)
  uuid6=$(uuidgen)

  read -p 'enter EOD ID: ' eodid
  if [ -z "$eodid" ]
  then
    exit 1
  fi

  read -p 'enter IL error status: ' errorStatus
  if [ -z "$errorStatus" ]
  then
    exit 1
  fi

  read -p 'enter IL error description: ' errorDescription
  if [ -z "$errorDescription" ]
  then
    exit 1
  fi


  crFilename="UUID1_UUID2_1_cr.rsp"
  crFilename=${crFilename//UUID1/$uuid1}
  crFilename=${crFilename//UUID2/$uuid2}
  echo "CR filename: $crFilename"

  ilFilename="UUID1_UUID3_0_il.rsp"
  ilFilename=${ilFilename//UUID1/$uuid1}
  ilFilename=${ilFilename//UUID3/$uuid3}
  echo "IL filename: $ilFilename"

  #${parameter//pattern/string}
  uiFileContentsParam="{\"operation\": \"upload_file\", \"transaction_id\": \"2d1c7f6f-224e-768e-a274-41af570e6502\", \"relation_id\": \"EODID\", \"a360_record_id\": \"1cf976c7-cedd-703f-ab70-01588bd56d50\", \"process_time\": \"2023-07-11T11:39:26.790000\", \"status\": 1, \"input\": \"{\\\"operation\\\": \\\"create_record\\\",\\\"relation_id\\\": \\\"EODID\",\\\"record_metadata\\\": {\\\"record_class\\\": \\\"A360TEST\",\\\"publisher\\\": \\\"A360\\\",\\\"recordDate\\\": \\\"2016-11-22T11:39:30Z\\\",\\\"region\\\": \\\"GBR\",\\\"title\\\": \\\"A360230711_TestIngestion_2\\\"}}\", \"exception_description\": null, \"errorStatus\": null}"
  uiFileContents=${uiFileContentsParam//EODID/$eodid}
  echo "$uiFileContents"

  crFileContentsParam="{\"operation\": \"create_record\", \"transaction_id\": \"2d1c7f6f-224e-768e-a274-41af570e6502\", \"relation_id\": \"EODID\", \"a360_record_id\": \"1cf976c7-cedd-703f-ab70-01588bd56d50\", \"process_time\": \"2023-07-11T11:39:26.790000\", \"status\": 1, \"input\": \"{\\\"operation\\\": \\\"create_record\\\",\\\"relation_id\\\": \\\"EODID\",\\\"record_metadata\\\": {\\\"record_class\\\": \\\"A360TEST\",\\\"publisher\\\": \\\"A360\\\",\\\"recordDate\\\": \\\"2016-11-22T11:39:30Z\\\",\\\"region\\\": \\\"GBR\",\\\"title\\\": \\\"A360230711_TestIngestion_2\\\"}}\", \"exception_description\": null, \"errorStatus\": null}"
  crFileContents=${crFileContentsParam//EODID/$eodid}
  echo "$crFileContents"

  ilFileContentsParam="{\"operation\": \"invalid_line\", \"transaction_id\": \"f11e1453-27ef-75ec-9322-41af570e6502\", \"relation_id\": \"EODID\", \"a360_record_id\": \"1cf976c7-cedd-703f-ab70-01588bd56d50\", \"process_time\": \"2023-07-11T11:41:27.873000\", \"status\": 1, \"input\": \"{\\\"operation\\\": \\\"upload_new_file\\\",\\\"relation_id\\\": \\\"EODID\\\",\\\"file_metadata\\\":{\\\"publisher\\\": \\\"A360\\\",\\\"dz_file_name\\\": \\\"A360230516_TestIngestion_1.docx\\\",\\\"file_tag\\\": \\\"docx\\\"}}\", \"exception_description\": \"ERROR_DESCRIPTION\", \"error_status\": \"ERROR_STATUS\"}"
  ilFileContents=${ilFileContentsParam//EODID/$eodid}
  ilFileContents=${ilFileContents//ERROR_STATUS/$errorStatus}
  ilFileContents=${ilFileContents//ERROR_DESCRIPTION/$errorDescription}
  echo "$ilFileContents"

  echo $uiFileContents >> $iuFilename
  echo $crFileContents > $crFilename
  echo $ilFileContents > $ilFilename

  read -p 'Do you want to add new responses: (y/n): ' shouldAddNewResponses
  if [ "$shouldAddNewResponses" == "y" ]
  then
    addNewResponses
  fi
}

addNewResponses

popd

