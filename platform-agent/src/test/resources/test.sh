#!/usr/bin/env bash

echo "test shell stdout"
echo "test shell stderr" >& 2

export FLOW_AGENT_1=11
export FLOW_AGENT_2=2

sleep 2
echo "done"