UPDATE event_handler ev
SET handler = 'SentencingRemarksAndRetentionPolicyHandler'
WHERE ev.handler = 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler';
