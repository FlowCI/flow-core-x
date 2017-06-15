#!/usr/bin/env bash

echo "test shell stdout"
echo "test shell stderr" >& 2
sleep 2
echo "done"