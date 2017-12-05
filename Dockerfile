FROM flowci/flow.ci.tomcat:latest

# setup flow.ci default environments
ENV MAVEN_VERSION 3.3.9
ENV FLOW_PLATFORM_DIR=/etc/flow.ci
ENV FLOW_PLATFORM_CONFIG_DIR=/etc/flow.ci/config
ENV FLOW_PLATFORM_SOURCE_CODE=/flow-platform

RUN mkdir -p $FLOW_PLATFORM_DIR \
	&& mkdir -p $FLOW_PLATFORM_CONFIG_DIR \
	&& mkdir -p $FLOW_PLATFORM_DIR/migration \
	&& mkdir -p $FLOW_PLATFORM_SOURCE_CODE

# install git
RUN apt-get update \
    && apt-get install -y --no-install-recommends apt-utils \
	&& apt-get -y install git \
	&& git config --global user.email "flowci@flow.ci" \
	&& git config --global user.name "flowci"

# intall open jdk
RUN apt-get -y install openjdk-8-jdk

# install maven
RUN curl -fsSL http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xzf - -C /usr/share \
    && mv /usr/share/apache-maven-$MAVEN_VERSION /usr/share/maven \
    && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

# install mysql
ENV MYSQL_MAJOR 5.6
ENV MYSQL_VERSION 5.6.38-1debian8

RUN echo "deb http://repo.mysql.com/apt/debian/ jessie mysql-${MYSQL_MAJOR}" > /etc/apt/sources.list.d/mysql.list

RUN { \
		echo mysql-community-server mysql-community-server/data-dir select ''; \
		echo mysql-community-server mysql-community-server/root-pass password ''; \
		echo mysql-community-server mysql-community-server/re-root-pass password ''; \
		echo mysql-community-server mysql-community-server/remove-test-db select false; \
	} | debconf-set-selections \
	&& apt-get update && apt-get install -y mysql-server="${MYSQL_VERSION}" && rm -rf /var/lib/apt/lists/* \
	&& rm -rf /var/lib/mysql && mkdir -p /var/lib/mysql /var/run/mysqld \
	&& chown -R mysql:mysql /var/lib/mysql /var/run/mysqld \
# ensure that /var/run/mysqld (used for socket and lock files) is writable regardless of the UID our mysqld instance ends up having at runtime
	&& chmod 777 /var/run/mysqld \
# comment out a few problematic configuration values
	&& find /etc/mysql/ -name '*.cnf' -print0 \
		| xargs -0 grep -lZE '^(bind-address|log)' \
		| xargs -rt -0 sed -Ei 's/^(bind-address|log)/#&/' \
# don't reverse lookup hostnames, they are usually another container
	&& echo '[mysqld]\nskip-host-cache\nskip-name-resolve' > /etc/mysql/conf.d/docker.cnf


ADD ./docker/mysqld.cnf /etc/mysql/conf.d/mysqld.cnf

# copy code
COPY . $FLOW_PLATFORM_SOURCE_CODE

# mvn build
RUN	cd $FLOW_PLATFORM_SOURCE_CODE \
    && rm -rf $FLOW_PLATFORM_SOURCE_CODE/dist \
    && mvn clean install -DskipTests=true

# setup flow.ci default configuration
COPY ./docker/app-cc.properties $FLOW_PLATFORM_CONFIG_DIR
COPY ./docker/app-api.properties $FLOW_PLATFORM_CONFIG_DIR

# config tomcat
COPY ./docker/tomcat-users.xml $CATALINA_HOME/conf

# wait for mysql
COPY ./docker/flow.ci.backend.cmd.sh $FLOW_PLATFORM_DIR
COPY ./schema/migration $FLOW_PLATFORM_DIR/migration

# set wars to tomcat
RUN   cd  $FLOW_PLATFORM_SOURCE_CODE \
      && mv ./dist/flow-control-center-*.war $CATALINA_HOME/webapps/flow-control-center.war \
      && mv ./dist/flow-api-*.war $CATALINA_HOME/webapps/flow-api.war

WORKDIR $FLOW_PLATFORM_DIR

CMD bash ./flow.ci.backend.cmd.sh catalina.sh run