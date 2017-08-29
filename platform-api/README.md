## Node Status Transmission

- CmdStatus and Exit Value --> transfer to --> NodeStatus
- NodeStatus --> bottom up transfer to --> Parent Node Status --> Root Node Status
- Root Node Status --> transfer to --> Job Status


## Environments Description

### Flow

**FLOW_STATUS**: READY | PENDING

**FLOW_YML_STATUS**: NOT_FOUND | GIT_CONNECTING | GIT_LOADING | GIT_LOADED | FOUND | ERROR

**FLOW_YML_ERROR_MSG**: error message if FLOW_YML_STATUS = ERROR

### Git 
        
**FLOW_GIT_SOURCE**: UNDEFINED_SSH | UNDEFINED_HTTP | GITLAB | GITHUB| CODING| OSCHINA | BITBUCKET 

**FLOW_GIT_URL**: Git repo url

**FLOW_GIT_BRANCH**: Git repo branch to check

**FLOW_GIT_WEBHOOK**: readonly, output webhook url of flow

**FLOW_GIT_CHANGELOG**: readonly

**FLOW_GIT_EVENT_TYPE**: readonly

**FLOW_GIT_SSH_PRIVATE_KEY**: readonly

**FLOW_GIT_SSH_PUBLIC_KEY**: readonly