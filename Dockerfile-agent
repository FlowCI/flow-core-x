#
# Flow Agent Dockerfile
# Image Name: flowci/flow-agent
#
# https://github.com/FlowCI/flow-platform
#
FROM flowci/flow-platform-base:latest

# setup flow.ci default environments
ENV FLOW_PLATFORM_SOURCE_CODE=/flow-platform
ENV MVN_CACHE=/root/.m2
ENV FLOW_AGENT_DIR=$HOME/agent


# copy code
COPY . $FLOW_PLATFORM_SOURCE_CODE

# Build Agent and delete no use thing
RUN cd $FLOW_PLATFORM_SOURCE_CODE \
    && rm -rf $FLOW_PLATFORM_SOURCE_CODE/dist \
    && mvn clean install -DskipTests=true \
    && cd  $FLOW_PLATFORM_SOURCE_CODE \
    && mkdir -p $FLOW_AGENT_DIR \
    && mv ./dist/flow-agent-*.jar $FLOW_AGENT_DIR/flow-agent.jar \
    && rm -rf $FLOW_PLATFORM_SOURCE_CODE \
    && rm -rf $MVN_CACHE


WORKDIR $FLOW_AGENT_DIR

CMD java -jar ./flow-agent.jar $FLOW_BASE_URL $FLOW_TOKEN
