## Flow API Reference ##

### GET Flows
  ```
      Method: Get
      Route: /flows
      Response:
        [
          {
            name: "flow", 
            path: "/flow",
            createdAt: 12121212,
            updatedAt: 12121212,
          },
          {
            name: "flow", 
            path: "/flow",
            createdAt: 12121212,
            updatedAt: 12121212,
          }
        ]
  ```

### Get Check flow name is existed
  ```
      Method: GET:
      Route: /flows/{flowname}/exist
      Response: true or false
  ```

### POST Create flow by name
  ```
      Method: POST:
      Route: /flows/{flowname}
      Response: {
        name: "xxx",
        path: "/xxx"
        createdAt: 12121212,
        updatedAt: 12121212,
      }

  ```

### POST Set flow environment variable
  ```
      Method: POST:
      Route: /flows/{flowname}/env
      Body: {
        FLOW_XXX_1: xxx,
        FLOW_XXX_2: xxx
      }
  ```
  
### GET Load flow yml content from repo
  ```
      Description: Async method, and then call Get flow yml content periodically 
      Method: GET:
      Route: /flows/{flowname}/yml/load
  ```

### GET Get flow yml content
  ```
      Method: GET:
      Route: /flows/{flowname}/yml
      Response:
      flow:
        - name: xx
        - steps:
          - name : xxx
  ```

### POST Verify yml content
    ```
        Method: POST:
        Route: /flows/{flowname}/yml/verify
        Response: if yml been verified, response 200, otherwise response 4xx with error message {message: xxx}
    ```

### GET Get flow webhook
  ```
      Method: GET:
      Route: /flows/{flowname}/webhook
      Response: {
        path: /xxx
        hook: http://xxxxxxxx 
      }
  ```

### GET latest job status
  ```
      Method: Post
      Route: /job/status/lastest
      Body: ['flow-1', 'flow-2']
      Response:
          [
          {
             path: "/flow",
             status: xxx
          }]
  ```

### Get Job 

  ``` 
        Method: Get
        Route: /:flowName/jobs/:buildNumber
        Response:
            {
                "id": 17080916210140717520056,
                "nodePath": "/flow5",
                "status": "SUCCESS",
                "outputs": {
                  "FLOW_GIT_COMMIT_ID": {
                    desc: "提交的Id"
                    value: "1234"
                  },
                  "FLOW_WORKSPACE": {
                     value: "~/flow-platform/test/id/1/1/3",
                     desc: ""
                  },
                  "FLOW_GIT_CHANGELOG": {
                      value: "test",
                      desc: "描述的信息"
                  },
                  "FLOW_GIT_COMPARE_ID": {
                    desc: "变更对比"
                    value: "1234..12121",
                  },
                  "FLOW_GIT_BRANCH": {
                      value: "master",
                      desc: "分支"
                   },
                  "FLOW_GIT_COMMITER":{
                      value: "WILL",
                      desc: "提交者"
                   }
                },
                "nodeName": "flow5",
                "startedAt": 1502266861,
                "finishedAt": 1502266861,
                "createdAt": 1502266861,
                "updatedAt": 1502266878
            }
  
  ```
  
### Job List
``` 
        Method: Get
        Route: /jobs?flowName=
        Response:
            [{
                "id": 17080916210140717520056,
                "nodePath": "/flow5",
                "status": "SUCCESS",
                "outputs": {
                  "FLOW_GIT_COMMIT_ID": {
                    desc: "提交的Id"
                    value: "1234"
                  },
                  "FLOW_WORKSPACE": {
                     value: "~/flow-platform/test/id/1/1/3",
                     desc: ""
                  },
                  "FLOW_GIT_CHANGELOG": {
                      value: "test",
                      desc: "描述的信息"
                  },
                  "FLOW_GIT_COMPARE_ID": {
                    desc: "变更对比"
                    value: "1234..12121",
                  },
                  "FLOW_GIT_BRANCH": {
                      value: "master",
                      desc: "分支"
                   },
                  "FLOW_GIT_COMMITER":{
                      value: "WILL",
                      desc: "提交者"
                   }
                },
                "nodeName": "flow5",
                "startedAt": 1502266861,
                "finishedAt": 1502266861,
                "createdAt": 1502266861,
                "updatedAt": 1502266878
            }]
 
  ```

### GET agents
    ```
        Method: Get
        Route: /agents
        Response:
           [
             {
               name: "xxxx",
               zone: "xxxx",
               "flowName": "flow6",
               "agentStatus": "BUSY",
               "number": 10,
               "branch": "master"
             }
           ]
    ```

### POST emailSetting
    ```
        Method: Post
        Route: /message/email/settings
        Param:
          - smtpUrl
          - smtpPort
          - sender
          - username
          - password
          - isAuthenticated
        Response:
             {
               smtpUrl: "xxxx",
               smtpPort: "xxxx",
               "sender": "flow6",
               "username": "BUSY",
               "password": xxxx,
               "isAuthenticated": false
             }
    ```

### UPDATE emailSetting
    ```
        Method: Patch
        Route: /message/email/settings
        Param:
          - smtpUrl
          - smtpPort
          - sender
          - username
          - password
          - isAuthenticated
        Response:
             {
               smtpUrl: "xxxx",
               smtpPort: "xxxx",
               "sender": "flow6",
               "username": "BUSY",
               "password": xxxx,
               "isAuthenticated": false
             }
    ```    

### auth emailSetting
    ```
        Method: Post
        Route: /message/email/settings/auth
        Param:
          - smtpUrl
          - smtpPort
          - sender
          - username
          - password
          - isAuthenticated
        Response:
             {
               auth: true
             }
    ```      

### detail flows 
    ```
        Method: Get
        Route: /flows/detail
        Response:
             [
               {
                  flowName: "test",
                  creator: "will",
                  createdAt: 1212121,
                  webhookUrl: "http://xxx.git",
                  deployKey: xxxx 
               }
             ]
    ```          
    
## Environments Description

### Flow

**FLOW_STATUS**: READY | PENDING

### Git 
        
**FLOW_GIT_SOURCE**: UNDEFINED_SSH | UNDEFINED_HTTP | GITLAB | GITHUB| CODING| OSCHINA | BITBUCKET 

**FLOW_GIT_URL**: Git repo url

**FLOW_GIT_BRANCH**: Git repo branch to check

**FLOW_GIT_WEBHOOK**: readonly, output webhook url of flow

**FLOW_GIT_CHANGELOG**: readonly

**FLOW_GIT_EVENT_TYPE**: readonly

**FLOW_GIT_SSH_PRIVATE_KEY**: readonly

**FLOW_GIT_SSH_PUBLIC_KEY**: readonly