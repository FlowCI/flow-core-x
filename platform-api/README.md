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
  
  
  ### GET latest job status
    ```
        Method: Post
        Route: /job/status/lastest
        Body: ['/flow-1', '/flow-2']
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
        Route: /job/:id
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
