#!/usr/bin/env bash

## For Dev Env

export FLOW_AGENT_ZK=54.222.129.38:2181
export FLOW_AGENT_ZONE=test-mos-mac
export FLOW_AGENT_NAME=$HOSTNAME

export JAVA_HOME=/Library/Java/JavaVirtualMachines/jre1.8.0_131.jre/Contents/Home

$JAVA_HOME/bin/java -jar flow-platform-client-0.0.1.jar $FLOW_AGENT_ZK $FLOW_AGENT_ZONE $FLOW_AGENT_NAME