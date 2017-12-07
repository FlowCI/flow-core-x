flow-platform
============

'flow-platform' is a backend project for flow.ci, it consist three main sub projects:  

- api: 
- control-center: to handle agent status and dispatch task to agent
- agent: running in the any where to receive task from control center


## Getting Start

### Start with docker

首先 Docker pull flowci/flow-platform ， 把对应的镜像拉下来
 
> 环境变量的设置:
> 
> - `FLOW_API_DOMAIN`：部署的后端 API 域名地址
> - `FLOW_WEB_DOMAIN`：部署的前端 Web 页面的域名地址
> - `FLOW_SYS_EMAIL`：flowci 系统管理员账号
> - `FLOW_SYS_USERNAME`：flowci 系统管理员的用户名
> - `FLOW_SYS_PASSWORD`：flowci 系统管理员密码
> - `MYSQL_PASSWORD`：flowci MYSQL 数据库的密码
> - `MYSQL_HOST`：flowci MYSQL 数据库的 `HOST`
> - `MYSQL 的存储路径`: `~/flow-ci/db`

```
 docker run -it \
   -e MYSQL_HOST=127.0.0.1 \
   -e MYSQL_PASSWORD=123456 \
   -e FLOW_SYS_EMAIL=admin@fir.im \
   -e FLOW_SYS_USERNAME=admin \
   -e FLOW_SYS_PASSWORD=123456  \
   -e FLOW_API_DOMAIN=127.0.0.1 \
   -e FLOW_WEB_DOMAIN=127.0.0.1 \
   -p 8080:8080 \
   -v ~/flow-ci/data:/var/lib/mysql \
   flowci/flow-platform

``` 

### Build by maven

Using standard maven `mvn clean install -DskipTests=true` to generate packages

####Installation####

In build phase will generate three packages:

| project | package | container |
|---------|---------|-----------|
| api | flow-api.war | tomcat 8.5 |
| control-center | flow-control-center.war | tomcat 8.5 |
| agent | flow-agent-{version}.jar |  |

**flow api**

Dependencies: 

- Mysql 5.6

**flow control center**

Dependencies:

- Apache zookeeper 3.4.10
- Mysql 5.6
- RabbitMQ 3.6.10

**Configuration**

Configuration file can be loaded by sequence:
- System envrionment variable: `FLOW_CONFIG_PATH`
- System property: `-Dconfig.path`
- Default directory: `/etc/flow.ci/config/app.properties`

The sample properites file in : 

`platform-control-center/app-example.properties`

**Agent**

Start agent by following command:

`java -jar flow-platform-agent-{version}.jar {zookeeper address} {zone name} $HOSTNAME`


### Build by docker

Run `./build-docker.sh` will generate required docker images `flow.ci.backend` for back-end and `flow.ci.agent` for agent.

**To start flow.ci backend services**

`docker-compose up`

**To start flow.ci agent** 

`docker run --network=host -e FLOW_ZOOKEEPER_HOST=127.0.0.1:2181 -e FLOW_AGENT_ZONE=default -e FLOW_AGENT_NAME={agent name you want} flow.ci.agent`



## License
flow-platform is an open source project, sponsored by [fir.im](https://www.fir.im) 
under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
