# flow.ci.backend docker includes war of flow.ci api and control center
# VERSION beta 0.1

# The image provides default settings for flow.ci
# if you want to customize flow.ci settings file,
# please mount to /etc/flow.ci to customize  app-api.properties,
# and app-cc.properties in config folder

FROM flow.ci.tomcat:latest

ENV MAVEN_VERSION 3.3.9

# setup flow.ci default environments
ENV FLOW_PLATFORM_DIR=/etc/flow.ci
ENV FLOW_PLATFORM_CONFIG_DIR=/etc/flow.ci/config
ENV FLOW_PLATFORM_SOURCE_CODE=/flow-platform

RUN mkdir -p $FLOW_PLATFORM_DIR \
	mkdir -p $FLOW_PLATFORM_CONFIG_DIR \
	mkdir -p $FLOW_PLATFORM_DIR/migration \
	mkdir -p $FLOW_PLATFORM_SOURCE_CODE

# install maven
RUN curl -fsSL http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xzf - -C /usr/share \
    && mv /usr/share/apache-maven-$MAVEN_VERSION /usr/share/maven \
    && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn


# install git
RUN apt-get update \
	&& apt-get -y install git \
	&& git config --global user.email "flowci@flow.ci" \
	&& git config --global user.name "flowci"

# install mysql
RUN apt-get install -y mysql-server-5.6 mysql-client-5.6

# copy code
COPY . $FLOW_PLATFORM_SOURCE_CODE

# mvn build
RUN	cd $FLOW_PLATFORM_SOURCE_CODE \
    rm -rf $FLOW_PLATFORM_SOURCE_CODE/dist \
    mvn clean install -DskipTests=true

# setup flow.ci default configuration
COPY ./docker/app-cc.properties $FLOW_PLATFORM_CONFIG_DIR
COPY ./docker/app-api.properties $FLOW_PLATFORM_CONFIG_DIR

# config tomcat
COPY ./docker/tomcat-users.xml $CATALINA_HOME/conf

# wait for mysql
COPY ./docker/flow.ci.backend.cmd.sh $FLOW_PLATFORM_DIR
COPY ./schema/migration $FLOW_PLATFORM_DIR/migration

# set wars to tomcat
# COPY ./target/flow-control-center.war $CATALINA_HOME/webapps
# COPY ./target/flow-api.war $CATALINA_HOME/webapps

RUN   cd  $FLOW_PLATFORM_SOURCE_CODE \
      mv ./dist/flow-control-center-*.war $CATALINA_HOME/webapps/flow-control-center.war \
      mv ./dist/flow-api-*.war $CATALINA_HOME/webapps/flow-api.war

WORKDIR $FLOW_PLATFORM_DIR

CMD bash ./flow.ci.backend.cmd.sh catalina.sh run