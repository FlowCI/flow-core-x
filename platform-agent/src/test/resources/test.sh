#!/usr/bin/env bash

echo "test shell stdout"
echo "test shell stderr" >& 2

export FLOW_UT_OUTPUT_1=11
export FLOW_UT_OUTPUT_2=2

sleep 2
echo "done"