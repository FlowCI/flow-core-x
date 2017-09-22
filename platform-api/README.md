## Node Status Transmission

- CmdStatus and Exit Value --> transfer to --> NodeStatus
- NodeStatus --> bottom up transfer to --> Parent Node Status --> Root Node Status
- Root Node Status --> transfer to --> Job Status

## Environment Variable Description

### Flow

**FLOW_STATUS**: {READY | PENDING}

**FLOW_YML_STATUS**: {NOT_FOUND | GIT_CONNECTING | GIT_LOADING | GIT_LOADED | FOUND | ERROR}

**FLOW_YML_ERROR_MSG**: error message if FLOW_YML_STATUS = ERROR

**FLOW_DEPLOY_KEY_NAME**: the credential name for RSA deploy key pair

### Job

**FLOW_JOB_BUILD_NUMBER**: readonly, the job build number

**FLOW_JOB_BUILD_CATEGORY**: readonly, the job trigger type, same as FLOW_GIT_EVENT_TYPE 

**FLOW_JOB_AGENT_INFO**:  readonly, the agent path which job running on as 'zone#name' format

### Git 
        
**FLOW_GIT_SOURCE**: {UNDEFINED_SSH | UNDEFINED_HTTP | GITLAB | GITHUB| CODING| OSCHINA | BITBUCKET} 

**FLOW_GIT_URL**: Git repo url

**FLOW_GIT_BRANCH**: Git repo branch to check

**FLOW_GIT_WEBHOOK**: readonly, output webhook url of flow

**FLOW_GIT_CHANGELOG**: readonly and load from git

**FLOW_GIT_EVENT_SOURCE**: readonly and from git event

**FLOW_GIT_EVENT_TYPE**: {MANUAL | PUSH | PR | TAG} readonly and from git event 

**FLOW_GIT_SSH_PRIVATE_KEY**: readonly

**FLOW_GIT_SSH_PUBLIC_KEY**: readonly

#### Git Push or Tag Event

**FLOW_GIT_AUTHOR**: readonly and git username who create push or tag event

**FLOW_GIT_COMMIT_ID**: readonly, latest git commit id

**FLOW_GIT_COMPARE_ID**: readonly, xxxx...xxxx

**FLOW_GIT_COMPARE_URL**: readonly, html git compare url

#### Git Pull Request Event

**FLOW_GIT_BRANCH**: readonly, the source branch when open pr, target branch when close pr

**FLOW_GIT_AUTHOR**: readonly, the submitter when open pr, merged by user when close pr
 
**FLOW_GIT_PR_URL**: readonly, the html pr url of git repo
