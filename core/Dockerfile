FROM amazoncorretto:17-alpine3.18

RUN apk add bash

ENV WORKER=/flow.ci
ENV JAR=flowci-core.jar

WORKDIR $WORKER

COPY target/$JAR $WORKER
COPY wait_for_it.sh $WORKER

RUN mkdir -p $HOME/.ssh
RUN echo "StrictHostKeyChecking=no" >> $HOME/.ssh/config

CMD java $JVM_OPS -jar $JAR