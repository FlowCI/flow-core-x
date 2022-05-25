<h3 align="center">
  <a href="https://flowci.github.io">
    <img src="https://github.com/FlowCI/docs/raw/master/_images/logo.png" alt="Logo" width="100">
  </a>
</h3>

<h3 align="center">A Simple & Powerful CI/CD Server</h3>

<p align="center">
    <a href="https://github.com/FlowCI/docs/blob/master/LICENSE"><img src="https://img.shields.io/github/license/flowci/flow-core-x"></a>
    <a href="https://github.com/FlowCI/flow-core-x/releases/"><img src="https://img.shields.io/github/v/release/flowci/flow-core-x"></a>
    <a href="https://github.com/FlowCI"><img alt="GitHub Org's stars" src="https://img.shields.io/github/stars/flowci"></a>
    <a href="https://hub.docker.com/u/flowci"><img alt="Docker Pulls" src="https://img.shields.io/docker/pulls/flowci/core"></a>
</p>

<div align="center">

</div>

## What is flow.ci?

flow.ci is an open-source CI/CD automation server that designed for setting up a self-hosted CI/CD service with the easiest, fastest and most painless way. It supports high availability, multiple building environment, and scalability with dynamic agents.

- __High Availability__

  flow.ci is designed to work in the cloud -- public, private, or hybrid, it could be deployed with multiple instances, the configuration/jobs data on the node may not be lost when the instance fails.


- __High Performance__

  - __scaling__: auto scaling agents either on K8s cluster or Linux host
  - __parallel__: job steps can be executed in parallel on multiple agents
  - __cache__: cache anything to speed up the build


- __Zero Configuration__

  flow.ci tries to minimize the complexity of any configuration, the server could be started with three command lines. It also provides build templates of many programming languages, a job could be started just using it.


- __Online Debugging__

  flow.ci supports the online TTY terminal so that you could find out the problems in the running job from runtime terminal.


- __Flexible Plugins__

  Using plugins on flow.ci is quite simple, you just need type the plugin name in the step. Developing a plugin is also quite easy, you could use any language on your own plugin development.


- __Flexible Runtime__

  Each step or step group can be run either on any docker images or native os.

## Quick start

> [Docker](https://docs.docker.com/install/) & [Docker-Compose](https://docs.docker.com/compose/install/) are required

```bash
git clone https://github.com/FlowCI/docker.git flow-docker
cd flow-docker
./server.sh start
```

## Documentation

+ [English](https://flowci.github.io/#/en/)
+ [中文文档](https://flowci.github.io/#/cn/)

Need Help? submit issue from [here](https://github.com/FlowCI/docs/issues) or send email to `flowci@foxmail.com`


## Templates

[maven, npm, golang, ruby, android and more](https://github.com/FlowCI/templates)


## Architecture

![architecture](https://github.com/FlowCI/docs/raw/master/_images/architecture.png)

## Preview

![demo](https://github.com/FlowCI/docs/raw/master/_images/demo.gif)
