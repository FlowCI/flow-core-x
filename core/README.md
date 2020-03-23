## Status List

#### Status for job

* PENDING: Initial status

* QUEUED: Been put to job queue

* RUNNING: Agent received the job and start to execute

* SUCCESS: Job been executed 

* FAILURE: Job been executed but failure

* CANCELLED: Job been stopped by user

* TIMEOUT: Job execution time been over the expiredAt

#### Status for step

* PENDING

* RUNNING

* SUCCESS

* EXCEPTION

* KILLED

* TIMEOUT

## Websocket API

Data:

```
{
  "event": "NEW_CREATED" | "STATUS_CHANGE"
  "body": {} | []
}
```

Init connection: `{host}/ws`

- Example:
  ```javascript
  const socket = new SockJS('http://127.0.0.1/ws')
  stompClient = Stomp.over(socket);
  stompClient.connect({}, function (frame) {
    console.log('Connected: ' + frame);
  });
  ```

Cmd logging: `/topic/logs/{cmd id}`

- Example:
  ```javascript
  const path = '/topic/logs/' + step.id;
  stompClient.subscribe(path, function (data) {
    console.log(data.body + " =======");
    // STDOUT#10#hello world
  });
  ```
  
Job Event: `/topic/jobs`

- Example:
  ```
  const path = '/topic/jobs
  stompClient.subscribe(path, function (data) {
    
  });
  ```
  
Step Event: `/topic/steps/{job id}`

- Example:
  ```
  const path = '/topic/steps/xxxxxxxx
  stompClient.subscribe(path, function (data) {
    
  });
  ```

