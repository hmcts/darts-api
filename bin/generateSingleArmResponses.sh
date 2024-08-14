#!/bin/bash
echo "This script generates the success path ARM pull response files. These can be used for testing where there is a need for an object such as media, transcription document, annotation document or case document to be stored in the ARM storage"

uuid1=$(uuidgen)
uuid2=$(uuidgen)
uuid3=$(uuidgen)
uuid4=$(uuidgen)
echo "uuid 1: $uuid1 uuid 2 $uuid2"
echo "uuid 3: $uuid3 uuid 4 $uuid4"

read -p 'enter EOD ID: ' eodid
if [ -z "$eodid" ]
then
  exit 1
fi

read -p 'enter object ID (media, transcription document, annotation document or case document): ' objectid
if [ -z "$objectid" ]
then
  exit 1
fi

read -p 'enter transfer attempt: ' transferattempt
if [ -z "$transferattempt" ]
then
  exit 1
fi

read -p 'enter checksum: ' checksum
if [ -z "$checksum" ]
then
  exit 1
fi

iuFilename="EODID_OBJECTID_TRANSFERATTEMPTS_UUID1_1_ui.rsp"
iuFilename=${iuFilename//EODID/$eodid}
iuFilename=${iuFilename//OBJECTID/$objectid}
iuFilename=${iuFilename//TRANSFERATTEMPTS/$transferattempt}
iuFilename=${iuFilename//UUID1/$uuid1}
echo "IU filename: $iuFilename"

crFilename="UUID1_UUID2_1_cr.rsp"
crFilename=${crFilename//UUID1/$uuid1}
crFilename=${crFilename//UUID2/$uuid2}
echo "CR filename: $crFilename"

ufFilename="UUID1_UUID3_1_uf.rsp"
ufFilename=${ufFilename//UUID1/$uuid1}
ufFilename=${ufFilename//UUID3/$uuid3}
echo "UF filename: $ufFilename"

#${parameter//pattern/string}
uiFileContentsParam="{\"operation\": \"upload_file\", \"transaction_id\": \"2d1c7f6f-224e-768e-a274-41af570e6502\", \"relation_id\": \"EODID\", \"a360_record_id\": \"1cf976c7-cedd-703f-ab70-01588bd56d50\", \"process_time\": \"2023-07-11T11:39:26.790000\", \"status\": 1, \"input\": \"{\\\"operation\\\": \\\"create_record\\\",\\\"relation_id\\\": \\\"EODID\",\\\"record_metadata\\\": {\\\"record_class\\\": \\\"A360TEST\",\\\"publisher\\\": \\\"A360\\\",\\\"recordDate\\\": \\\"2016-11-22T11:39:30Z\\\",\\\"region\\\": \\\"GBR\",\\\"title\\\": \\\"A360230711_TestIngestion_2\\\"}}\", \"exception_description\": null, \"error_status\": null}"
uiFileContents=${uiFileContentsParam//EODID/$eodid}
echo "$uiFileContents"

crFileContentsParam="{\"operation\": \"create_record\", \"transaction_id\": \"2d1c7f6f-224e-768e-a274-41af570e6502\", \"relation_id\": \"EODID\", \"a360_record_id\": \"1cf976c7-cedd-703f-ab70-01588bd56d50\", \"process_time\": \"2023-07-11T11:39:26.790000\", \"status\": 1, \"input\": \"{\\\"operation\\\": \\\"create_record\\\",\\\"relation_id\\\": \\\"EODID\",\\\"record_metadata\\\": {\\\"record_class\\\": \\\"A360TEST\",\\\"publisher\\\": \\\"A360\\\",\\\"recordDate\\\": \\\"2016-11-22T11:39:30Z\\\",\\\"region\\\": \\\"GBR\",\\\"title\\\": \\\"A360230711_TestIngestion_2\\\"}}\", \"exception_description\": null, \"error_status\": null}"
crFileContents=${crFileContentsParam//EODID/$eodid}
echo "$crFileContents"

ufFileContentsParam="{\"operation\": \"upload_new_file\", \"transaction_id\": \"f11e1453-27ef-75ec-9322-41af570e6502\", \"relation_id\": \"EODID\", \"a360_record_id\": \"1cf976c7-cedd-703f-ab70-01588bd56d50\", \"process_time\": \"2023-07-11T11:41:27.873000\", \"status\": 1, \"input\": \"{\\\"operation\\\": \\\"upload_new_file\\\",\\\"relation_id\\\": \\\"EODID\\\",\\\"file_metadata\\\":{\\\"publisher\\\": \\\"A360\\\",\\\"dz_file_name\\\": \\\"A360230516_TestIngestion_1.docx\\\",\\\"file_tag\\\": \\\"docx\\\"}}\", \"exception_description\": null, \"error_status\": null, \"a360_file_id\": \"e7cde7c6-15d7-4c7e-a85d-a468c7ea72b9\", \"file_size\": 11997, \"s_md5\": "CHECKSUM", \"s_sha256\": \"33054BD335175AE9CAFEBA794E468F2EC1C3F999CD8E0B314432A2C893EE4775\"}"
ufFileContents=${ufFileContentsParam//EODID/$eodid}
ufFileContents=${ufFileContents//CHECKSUM/$checksum}
echo "$ufFileContents"

mkdir -p arm_responses
pushd arm_responses
echo $uiFileContents > $iuFilename
echo $crFileContents > $crFilename
echo $ufFileContents > $ufFilename

popd
