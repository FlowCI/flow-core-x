

MVN_CLEAN    	:= mvn clean
MVN_BUILD    	:= mvn clean compile
MVN_PKG	    	:= mvn clean package -Dmaven.test.skip=true
MVN_TEST		:= mvn clean test


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