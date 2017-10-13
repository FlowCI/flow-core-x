#!/usr/bin/env bash

TOMCAT_PATH=/home/deploy/apache-tomcat-8.5.14

HOST_IP=flow-lyon
HOST_USER=deploy
DEPLOY_PATH=${TOMCAT_PATH}/webapps

mvn clean package -DskipTests=true

# stop tomcat
ssh ${HOST_USER}@${HOST_IP} "${TOMCAT_PATH}/bin/shutdown.sh"

# cleanup current deploy package
ssh ${HOST_USER}@${HOST_IP} "rm -r ${DEPLOY_PATH}/flow-*"

# Find flow-control-center.war and
array=$(find ./platform-control-center -name flow-control-center.war 2>&1)
for file in ${array[@]}
do
 echo "=== flow control center war been found: $file"
 scp ${file} ${HOST_USER}@${HOST_IP}:${DEPLOY_PATH}
done

# Find flow-api.war
array=$(find ./platform-api/ -name flow-api.war 2>&1)
for file in ${array[@]}
do
 echo "=== flow api war been found: $file"
 scp ${file} ${HOST_USER}@${HOST_IP}:${DEPLOY_PATH}
done


# start tomcat
ssh ${HOST_USER}@${HOST_IP} "${TOMCAT_PATH}/bin/startup.sh"