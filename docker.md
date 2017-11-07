## FlowCi docker 使用说明
### 安装 docker 环境
>安装步骤请查看官方文档[install docker](https://github.com/docker/docker.github.io/edit/master/docker-for-mac/install.md)

### 使用 FlowCi

* **克隆代码**：
  - 选择通过 Git 的形式 Clone 代码，确保机器已经安装Git，之后执行命令： \
  	git clone git@github.com:FlowCI/docker.git
  - 选择直接通过下载的形式 Clone 代码，之后执行命令： \
  	curl -L  -o docker.zip  https://github.com/FlowCI/docker/archive/master.zip \
  	之后再解压 docker 目录下
 
* **Start Docker**：
  - 进入到上步下载的 docker 目录，执行命令: ./start-services.sh，之后可以访问地址 http://localhost:3000 进入
  - 启动时可以添加各种环境变量，例如\
    FLOW_API_DOMAIN： 部署的Api域名地址， 默认：[http://localhost:8080]()   \
    FLOW_WEB_DOMAIN： 部署的Web的域名地址，默认：[http://localhost:3000]()   \
    FLOW_WS_URL：部署的Api的 web socket 地址，默认：ws://localhost:8080 \
    FLOW_SYS_EMAIL：Flow 的系统账号，默认是 admin@flow.ci \
    FLOW_SYS_USERNAME：Flow 的用户名，默认是 admin \
    FLOW_SYS_PASSWORD: Flow 的系统密码，默认是 123456 \
    Demo: 
  	```
  	mkdir flowci 
  	cd flowci 
  	git clone git@github.com:FlowCI/docker.git 
  	cd docker 
    FLOW_API_DOMAIN=http://api.xxxx.ci FLOW_WEB_DOMAIN=http://xxxx.ci ./start-services.sh 
    浏览器输入 xxxx.ci 即可
    ```
    
* **Build Docker**
  - 确保 flow-platform、flow-web、docker三个项目在同一目录下 \
    Demo：
    ```
    mkdir flowci 
    cd flowci 
    git clone git@github.com:FlowCI/flow-platform.git 
    git clone git@github.com:FlowCI/flow-web.git 
    git clone git@github.com:FlowCI/docker.git 
    cd docker 
    ./build-docker.sh
    ```
  - build docker 的环境变量描述: \
    DOCKER_NAME_FLOWCI: FlowApi build 的 image 名称，默认 [flowci/flow.ci.backend](https://hub.docker.com/r/flowci/flow.ci.backend/) \
    DOCKER_NAME_FLOW_WEB: FlowWeb build 的 image 名称，默认 [flowci/flow.web](https://hub.docker.com/r/flowci/flow.web/) \
    DOCKER_NAME_FLOWCI_AGENT: FlowApi build 的 image 名称，默认 [flowci/flow.ci.agent](https://hub.docker.com/r/flowci/flow.ci.agent/) \
    Demo: 
    ```
    DOCKER_NAME_FLOWCI=AAAAAA DOCKER_NAME_FLOW_WEB=BBBBB ./build-docker.sh  
    build 了新的镜像 AAAAAA,BBBBB 
    修改 docker-compose.yml 
    把flow.ci的 image 改为 AAAAAA
    把flow.web的 image 改为 BBBBB
    ```
    
* **Start Agent**
  - 以 Docker 形式启动 Agent \
    运行命令： ./start-agent.sh
    >建议不要使用 docker 启动 Agent, 因为不太灵活
   
  - 以 Jar 包形式启动 Agent \
    首先先准备好 java 环境，之后运行脚本：\
    java -jar flow-agent.jar([下载地址](https://github.com/FlowCI/docker/raw/feature/change_docker_latest/agent/flow-agent-0.1.0.jar))  FLOW_API_URL FLOW_TOKEN \