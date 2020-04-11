

MVN_CLEAN    	:= mvn clean
MVN_BUILD    	:= mvn compile -T 4C
MVN_PKG	    	:= mvn package -T 4C -Dmaven.test.skip=true
MVN_TEST		:= mvn test -T 4C


CURRENT_DIR 	:= $(shell pwd)

DOCKER_VOLUME	:= -v $(HOME)/.m2:/root/.m2
DOCKER_IMG		:= flowci/javasdk:1.0
DOCKER_RUN 		:= docker run -it --rm -v $(CURRENT_DIR):/ws -w /ws $(DOCKER_VOLUME) --network host $(DOCKER_IMG)

DOCKER_BUILD 	:= ./build.sh

.PHONY: build test clean package

build:
	$(DOCKER_RUN) $(MVN_BUILD)

package:
	$(DOCKER_RUN) $(MVN_PKG)

test:
	$(DOCKER_RUN) $(MVN_TEST)

clean:
	$(DOCKER_RUN) $(MVN_CLEAN)