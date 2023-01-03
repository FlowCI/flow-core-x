FROM amazoncorretto:11-alpine3.15

RUN apk add bash

ENV WORKER=/flow.ci
ENV JAR=flow-ci-core.jar

WORKDIR $WORKER

COPY target/$JAR $WORKER
COPY wait_for_it.sh $WORKER

RUN mkdir -p $HOME/.ssh
RUN echo "StrictHostKeyChecking=no" >> $HOME/.ssh/config

CMD java $JVM_OPS -jar $JAR