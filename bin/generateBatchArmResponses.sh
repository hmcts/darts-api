#!/bin/bash
echo "This script generates the ARM pull response files. These can be used for testing where ARM returns a response for an object such as media, transcription document, annotation document or case document to be stored in the ARM storage"

uuid1=$(uuidgen | tr -d '-')

echo "uuid 1: $uuid1"
echo " "
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

  echo "Options: 1, 2, 3, 4"
  echo "1: CR and UF"
  echo "2: IL and CR"
  echo "3: IL and UF"
  echo "4: IL and IL"
  read -p 'enter option: ' option
  if [ -z "$option" ]
  then
    exit 1
  fi

  read -p "enter IU timestamp or return to use default - 2024-01-11T12:46:21.215310: " iuTimestamp
  if [ -z "$iuTimestamp" ]
  then
    iuTimestamp="2024-01-11T12:46:21.215310"
  fi
  #${parameter//pattern/string}
  uiFileContentsParam="{\"operation\": \"input_upload\", \"timestamp\": \"IU_TIMESTAMP\", \"status\": 1, \"exception_description\": null, \"error_status\": null, \"filename\": \"CGITestFilesMalformedManifest_1\", \"submission_folder\": \"/dropzone/A360/submission\", \"file_hash\": \"fbfec54925d62146aeced724ff9f3c8e\"}"
  uiFileContents=${uiFileContentsParam//IU_TIMESTAMP/$iuTimestamp}
  echo "$uiFileContents"
  echo $uiFileContents >> $iuFilename

  errorStatus1="bad_input"
  errorDescription1="PS.20023:INVALID_PARAMETERS:Invalid line: invalid json"
  errorStatus2="bad_input"
  errorDescription2="PS.20023:INVALID_PARAMETERS:Invalid line: invalid json"

  if [ "$option" == "2" ] || [ "$option" == "3" ] || [ "$option" == "4" ]
  then
    echo " "
    read -p 'Do you want to enter error status and description (y/n): ' continue
    if [ "$continue" == "y" ]
    then
      read -p 'enter IL error status 1: ' errorStatus1
      if [ -z "$errorStatus1" ]
      then
        exit 1
      fi

      read -p 'enter IL error description 1: ' errorDescription1
      if [ -z "$errorDescription1" ]
      then
        exit 1
      fi

      if [ "$option" == "4"]
      then
        read -p 'enter IL error status 2: ' errorStatus2
        if [ -z "$errorStatus2" ]
        then
          exit 1
        fi

        read -p 'enter IL error description 2: ' errorDescription2
        if [ -z "$errorDescription2" ]
        then
          exit 1
        fi
      fi
    fi
  fi

  if [ "$option" == "1" ] || [ "$option" == "2" ]
  then
    statusCode=1

    crFilename="UUID1_UUID2_STATUSCODE_cr.rsp"
    crFilename=${crFilename//STATUSCODE/$statusCode}
    crFilename=${crFilename//UUID1/$uuid1}
    crFilename=${crFilename//UUID2/$uuid2}
    echo "CR filename: $crFilename"

    crFileContentsParam="{\"operation\": \"create_record\", \"transaction_id\": \"2d1c7f6f-224e-768e-a274-41af570e6502\", \"relation_id\": \"EODID\", \"a360_record_id\": \"1cf976c7-cedd-703f-ab70-01588bd56d50\", \"process_time\": \"2023-07-11T11:39:26.790000\", \"status\": 1, \"input\": \"{\\\"operation\\\": \\\"create_record\\\",\\\"relation_id\\\": \\\"EODID\\\",\\\"record_metadata\\\": {\\\"record_class\\\": \\\"A360TEST\\\",\\\"publisher\\\": \\\"A360\\\",\\\"recordDate\\\": \\\"2016-11-22T11:39:30Z\\\",\\\"region\\\": \\\"GBR\\\",\\\"title\\\": \\\"A360230711_TestIngestion_2\\\"}}\", \"exception_description\": null, \"errorStatus1\": null}"
    crFileContents=${crFileContentsParam//EODID/$eodid}
    echo "$crFileContents"
    echo $crFileContents > $crFilename
  fi

  if [ "$option" == "2" ] || [ "$option" == "3" ] || [ "$option" == "4" ]
  then
    ilFilename="UUID1_UUID3_0_il.rsp"
    ilFilename=${ilFilename//UUID1/$uuid1}
    ilFilename=${ilFilename//UUID3/$uuid3}
    echo "IL filename 1: $ilFilename"

    ilFileContentsParam="{\"operation\": \"invalid_line\", \"transaction_id\": \"f11e1453-27ef-75ec-9322-41af570e6502\", \"relation_id\": \"EODID\", \"a360_record_id\": \"1cf976c7-cedd-703f-ab70-01588bd56d50\", \"process_time\": \"2023-07-11T11:41:27.873000\", \"status\": 1, \"input\": \"{\\\"operation\\\": \\\"upload_new_file\\\",\\\"relation_id\\\": \\\"EODID\\\",\\\"file_metadata\\\":{\\\"publisher\\\": \\\"A360\\\",\\\"dz_file_name\\\": \\\"A360230516_TestIngestion_1.docx\\\",\\\"file_tag\\\": \\\"docx\\\"}}\", \"exception_description\": \"ERROR_DESCRIPTION\", \"error_status\": \"ERROR_STATUS\"}"
    ilFileContents=${ilFileContentsParam//EODID/$eodid}
    ilFileContents=${ilFileContents//ERROR_STATUS/$errorStatus1}
    ilFileContents=${ilFileContents//ERROR_DESCRIPTION/$errorDescription1}
    echo "$ilFileContents"
    echo $ilFileContents > $ilFilename

    if [ "$option" == "4" ]
    then
      ilFilename2="UUID1_UUID5_0_il.rsp"
      ilFilename2=${ilFilename2//UUID1/$uuid1}
      ilFilename2=${ilFilename2//UUID5/$uuid5}
      echo "IL filename 2: $ilFilename2"

      ilFileContentsParam2="{\"operation\": \"invalid_line\", \"transaction_id\": \"f11e1453-27ef-75ec-9322-41af570e6502\", \"relation_id\": \"EODID\", \"a360_record_id\": \"1cf976c7-cedd-703f-ab70-01588bd56d50\", \"process_time\": \"2023-07-11T11:41:27.873000\", \"status\": 1, \"input\": \"{\\\"operation\\\": \\\"create_record\\\",\\\"relation_id\\\": \\\"EODID\\\",\\\"file_metadata\\\":{\\\"publisher\\\": \\\"A360\\\",\\\"dz_file_name\\\": \\\"A360230516_TestIngestion_1.docx\\\",\\\"file_tag\\\": \\\"docx\\\"}}\", \"exception_description\": \"ERROR_DESCRIPTION\", \"error_status\": \"ERROR_STATUS\"}"
      ilFileContents2=${ilFileContentsParam2//EODID/$eodid}
      ilFileContents2=${ilFileContents2//ERROR_STATUS/$errorStatus1}
      ilFileContents2=${ilFileContents2//ERROR_DESCRIPTION/$errorDescription1}
      echo "$ilFileContents2"
      echo $ilFileContents2 > $ilFilename2
    fi
  fi

  if [ "$option" == "1" ] || [ "$option" == "3" ]
  then

    read -p 'enter checksum: ' checksum
    if [ -z "$checksum" ]
    then
      exit 1
    fi

    statusCode=1
    if [ "$option" == "3" ]
    then
      statusCode=0
    fi
    ufFilename="UUID1_UUID5_STATUSCODE_uf.rsp"
    ufFilename=${ufFilename//STATUSCODE/$statusCode}
    ufFilename=${ufFilename//UUID1/$uuid1}
    ufFilename=${ufFilename//UUID5/$uuid5}
    echo "UF filename : $ilFilename"

    ufFileContentsParam="{\"operation\": \"upload_new_file\", \"transaction_id\": \"f11e1453-27ef-75ec-9322-41af570e6502\", \"relation_id\": \"EODID\", \"a360_record_id\": \"1cf976c7-cedd-703f-ab70-01588bd56d50\", \"process_time\": \"2023-07-11T11:41:27.873000\", \"status\": 1, \"input\": \"{\\\"operation\\\": \\\"upload_new_file\\\",\\\"relation_id\\\": \\\"EODID\\\",\\\"file_metadata\\\":{\\\"publisher\\\": \\\"A360\\\",\\\"dz_file_name\\\": \\\"A360230516_TestIngestion_1.docx\\\",\\\"file_tag\\\": \\\"docx\\\"}}\", \"exception_description\": null, \"error_status\": null, \"a360_file_id\": \"e7cde7c6-15d7-4c7e-a85d-a468c7ea72b9\", \"file_size\": 11997, \"s_md5\": \"CHECKSUM\", \"s_sha256\": \"33054BD335175AE9CAFEBA794E468F2EC1C3F999CD8E0B314432A2C893EE4775\"}"
    ufFileContents=${ufFileContentsParam//EODID/$eodid}
    ufFileContents=${ufFileContents//CHECKSUM/$checksum}
    echo "$ufFileContents"

    echo $ufFileContents > $ufFilename
  fi

  echo " "
  read -p 'Do you want to add new responses: (y/n): ' shouldAddNewResponses
  if [ "$shouldAddNewResponses" == "y" ]
  then
    addNewResponses
  fi
}

addNewResponses

popd

