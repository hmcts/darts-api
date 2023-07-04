UPDATE event_type
SET handler = 'DefaultEventHandler'
WHERE handler = 'com.synapps.moj.dfs.handler.DARTSEventHandler';
