<h3 align="center">
  <a href="https://flowci.github.io">
    <img src="https://github.com/FlowCI/docs/raw/master/src/logo.png" alt="Logo" width="100">
  </a>
</h3>

<h3 align="center">A powerful and user-friendly CI/CD server</h3>

<p align="center">
    <a href="https://github.com/FlowCI/docs/blob/master/LICENSE"><img src="https://img.shields.io/github/license/flowci/flow-core-x"></a>
    <a href="https://github.com/FlowCI/flow-core-x/releases/"><img src="https://img.shields.io/github/v/release/flowci/flow-core-x"></a>
</p>

## What is flow.ci?

flow.ci is an open-source CI/CD automation server that designed for reducing the complexity and increasing user experience. It supports high availability, multiple building environment, and scalability with dynamic agents.

- __High Availability__

    flow.ci is designed to work in the cloud -- public, private, or hybrid, it could be deployed with multiple instances, the configuration/jobs data on the node may not be lost when the instance fails.

- __Zero Configuration__

    flow.ci tries to minimize the complexity of any configuration, the server could be started with three command lines. It also provides build templates of many programming languages, a job could be started just using it.

- __Dynamic Agents__

    flow.ci could scale agent automatically either on K8s cluster or Linux host.

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

+ [English](https://github.com/FlowCI/docs/tree/master/en/index.md) 
+ [简体中文](https://github.com/FlowCI/docs/tree/master/cn/index.md)

Need Help? submit issue from [here](https://github.com/FlowCI/docs/issues)


## Templates

[maven, npm, golang, ruby, android and more](https://github.com/FlowCI/templates)

## Architecture

![architecture](https://github.com/FlowCI/docs/raw/master/src/architecture.png)

## Preview

![demo](https://github.com/FlowCI/docs/raw/master/src/demo.gif)

![tty](https://github.com/FlowCI/docs/raw/master/src/step_tty.gif)
