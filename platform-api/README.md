### 创建 flows
```
   Method: Post
   Routes: /flows
   params: 
        - name # uniq
        - language
        - prev # allow null
        - next # allow null
```

### 删除 flows
```
   Method: Delete
   Routes: /flows/:id
```

### Get flow
```
   Method: Get
   Routes: /flows/:id
   Routes: /flow_name
   Response:
       {
          name: "flow",
          language: "ruby"
       }
```

### 创建 steps
```
   Method: Post
   Routes: /steps
   params: 
        - name # uniq
        - plugin
        - flowName
        - action # before or after
        - prev # allow null
        - next # allow null
```

### Get steps
```
   Method: Get
   Routes: /steps/:id
   Routes: /:flowName/:stepName
   Response:
        { 
          name: "a",
          plugin: "next"
        }
```


### Delete steps
```
   Method: Delete
   Routes: /steps/:id
```


### 跑任务
```
   Method: Post
   Routes: /flows/:name/trigger
   params: 
        - envs
        - name
```

### 创建session
```
  Json:
    {
      callbackUrl: "/jobs/:id/createSessionCb",
      jobId:
    }
```

### createSessionCallback
``` 
  params:
    idCreated: success
```

### 发送命令
```
  Method: Post
  Routes: /cmd/send
  params:
      {
        "callbackUrl": "/jobSteps/:id/callback",
      	"agentPath": {
      		"zone": "test-zone",
      		"name": "test-001"
      	},
      	"cmd": "ls",
      	"type": "RUN_SHELL",
      	"inputs": Map<String, String>
      }
```

### 返回的数据格式
``` 
  params:
     status: pending running success fail timeout
     logPaths:,
     outputs:,
     exitCode
```