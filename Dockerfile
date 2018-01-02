#
# Flow Platform Dockerfile
# Image Name: flowci/flow-platform
#
# https://github.com/FlowCI/flow-platform
#
FROM flowci/flow-platform-base:latest

# setup flow.ci default environments
ENV FLOW_PLATFORM_DIR=/etc/flow.ci
ENV FLOW_PLATFORM_CONFIG_DIR=/etc/flow.ci/config
ENV FLOW_PLATFORM_SOURCE_CODE=/flow-platform
ENV MVN_CACHE=/root/.m2

# setup mysql config
ADD ./docker/mysqld.cnf /etc/mysql/conf.d/mysqld.cnf
VOLUME /var/lib/mysql

# mount docker
# docker in docker volume map happens some error
# VOLUME /var/lib/docker
COPY ./docker/daemon.json /etc/docker/daemon.json

# config tomcat
COPY ./docker/tomcat-users.xml $CATALINA_HOME/conf

# copy code
COPY . $FLOW_PLATFORM_SOURCE_CODE

# mvn build and set wars to tomcat and delete no use thing
RUN cd $FLOW_PLATFORM_SOURCE_CODE \
    && rm -rf $FLOW_PLATFORM_SOURCE_CODE/dist \
    && mvn clean install -DskipTests=true \
    && mkdir -p $FLOW_PLATFORM_CONFIG_DIR \
    && cd  $FLOW_PLATFORM_SOURCE_CODE \
    && mv ./dist/flow-control-center-*.war $CATALINA_HOME/webapps/flow-control-center.war \
    && mv ./dist/flow-api-*.war $CATALINA_HOME/webapps/flow-api.war \
    && rm -rf $FLOW_PLATFORM_SOURCE_CODE \
    && rm -rf $MVN_CACHE


# add aliyun proxy
ADD ./docker/settings.xml /root/.m2/

# cache mvn package
VOLUME /root/.m2/repository

# setup flow.ci default configuration
COPY ./docker/app-cc.properties $FLOW_PLATFORM_CONFIG_DIR
COPY ./docker/app-api.properties $FLOW_PLATFORM_CONFIG_DIR

# wait for mysql
COPY ./docker/flow.ci.backend.cmd.sh $FLOW_PLATFORM_DIR
COPY ./schema/migration $FLOW_PLATFORM_DIR/migration

WORKDIR $FLOW_PLATFORM_DIR

CMD bash ./flow.ci.backend.cmd.sh catalina.sh run
