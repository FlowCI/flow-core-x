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

### GET agents
    ```
        Method: Get
        Route: /agents
        Response:
           [
             {
               "path": {
                 "zone": "default",
                 "name": "testd"
               },
               "flowName": "flow6",
               "agentStatus": "BUSY",
               "number": 10,
               "branch": "master"
             }
           ]
    ```
