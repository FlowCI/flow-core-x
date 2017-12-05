FROM flowci/flow.ci.tomcat:latest

# setup flow.ci default environments
ENV MAVEN_VERSION 3.3.9
ENV FLOW_PLATFORM_DIR=/etc/flow.ci
ENV FLOW_PLATFORM_CONFIG_DIR=/etc/flow.ci/config
ENV FLOW_PLATFORM_SOURCE_CODE=/flow-platform

# install mysql
# add our user and group first to make sure their IDs get assigned consistently, regardless of whatever dependencies get added
RUN groupadd -r mysql && useradd -r -g mysql mysql

# add gosu for easy step-down from root
ENV GOSU_VERSION 1.7
RUN set -x \
	&& apt-get update && apt-get install -y --no-install-recommends ca-certificates wget && rm -rf /var/lib/apt/lists/* \
	&& wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$(dpkg --print-architecture)" \
	&& wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$(dpkg --print-architecture).asc" \
	&& export GNUPGHOME="$(mktemp -d)" \
	&& gpg --keyserver ha.pool.sks-keyservers.net --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 \
	&& gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu \
	&& rm -r "$GNUPGHOME" /usr/local/bin/gosu.asc \
	&& chmod +x /usr/local/bin/gosu \
	&& gosu nobody true \
	&& apt-get purge -y --auto-remove ca-certificates wget

RUN mkdir /docker-entrypoint-initdb.d

# FATAL ERROR: please install the following Perl modules before executing /usr/local/mysql/scripts/mysql_install_db:
# File::Basename
# File::Copy
# Sys::Hostname
# Data::Dumper
RUN apt-get update && apt-get install -y perl pwgen --no-install-recommends && rm -rf /var/lib/apt/lists/*

RUN set -ex; \
# gpg: key 5072E1F5: public key "MySQL Release Engineering <mysql-build@oss.oracle.com>" imported
	key='A4A9406876FCBD3C456770C88C718D3B5072E1F5'; \
	export GNUPGHOME="$(mktemp -d)"; \
	gpg --keyserver ha.pool.sks-keyservers.net --recv-keys "$key"; \
	gpg --export "$key" > /etc/apt/trusted.gpg.d/mysql.gpg; \
	#rm -r "$GNUPGHOME"; \
	apt-key list > /dev/null

ENV MYSQL_MAJOR 5.6
ENV MYSQL_VERSION 5.6.38-1debian8

RUN echo "deb http://repo.mysql.com/apt/debian/ jessie mysql-${MYSQL_MAJOR}" > /etc/apt/sources.list.d/mysql.list

# the "/var/lib/mysql" stuff here is because the mysql-server postinst doesn't have an explicit way to disable the mysql_install_db codepath besides having a database already "configured" (ie, stuff in /var/lib/mysql/mysql)
# also, we set debconf keys to make APT a little quieter
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

COPY ./docker/mysqld.cnf /etc/mysql/conf.d/mysqld.cnf

VOLUME /var/lib/mysql
COPY ./docker/docker-entrypoint.sh /usr/local/bin/
RUN ln -s usr/local/bin/docker-entrypoint.sh /entrypoint.sh # backwards compat

# copy code
COPY . $FLOW_PLATFORM_SOURCE_CODE

RUN mkdir -p $FLOW_PLATFORM_DIR \
	&& mkdir -p $FLOW_PLATFORM_CONFIG_DIR \
	&& mkdir -p $FLOW_PLATFORM_DIR/migration \
	&& mkdir -p $FLOW_PLATFORM_SOURCE_CODE

# install git and jdk
RUN apt-get update \
    && apt-get install -y --no-install-recommends apt-utils \
	&& apt-get -y install git \
	&& git config --global user.email "flowci@flow.ci" \
	&& git config --global user.name "flowci" \
	&& apt-get -y install openjdk-8-jdk


# install maven
RUN curl -fsSL http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xzf - -C /usr/share \
    && mv /usr/share/apache-maven-$MAVEN_VERSION /usr/share/maven \
    && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

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