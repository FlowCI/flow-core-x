package com.flowci.core.job.service;

import com.flowci.core.api.adviser.ApiAuth;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.dao.ExecutedLocalTaskDao;
import com.flowci.core.job.domain.Executed;
import com.flowci.core.job.domain.ExecutedLocalTask;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.TaskUpdateEvent;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.event.GetPluginAndVerifySetContext;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.docker.ContainerManager;
import com.flowci.docker.DockerManager;
import com.flowci.docker.ImageManager;
import com.flowci.docker.domain.DockerStartOption;
import com.flowci.domain.LocalTask;
import com.flowci.exception.StatusException;
import com.flowci.tree.NodeTree;
import com.flowci.util.ObjectsHelper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class LocalTaskServiceImpl implements LocalTaskService {

    private static final String DefaultImage = "flowci/plugin-runtime";

    private static final int DefaultTimeout = 300; // seconds

    @Autowired
    private String serverUrl;

    @Autowired
    private ExecutedLocalTaskDao executedLocalTaskDao;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private DockerManager dockerManager;

    @Autowired
    private TaskExecutor appTaskExecutor;

    @Autowired
    private YmlManager ymlManager;

    @Override
    public void init(Job job) {
        NodeTree tree = ymlManager.getTree(job);
        List<LocalTask> list = tree.getRoot().getNotifications();
        if (list.isEmpty()) {
            return;
        }

        List<ExecutedLocalTask> tasks = new ArrayList<>(list.size());
        for (LocalTask n : list) {
            ExecutedLocalTask t = new ExecutedLocalTask();
            t.setName(n.getPlugin()); // name is plugin name
            t.setJobId(job.getId());
            t.setFlowId(job.getFlowId());
            tasks.add(t);
        }

        executedLocalTaskDao.insert(tasks);
        eventManager.publish(new TaskUpdateEvent(this, job.getId(), tasks, true));
    }

    @Override
    public List<ExecutedLocalTask> list(Job job) {
        return executedLocalTaskDao.findAllByJobId(job.getId());
    }

    @Override
    public Long delete(Job job) {
        return executedLocalTaskDao.deleteAllByJobId(job.getId());
    }

    @Override
    public Long delete(Flow flow) {
        return executedLocalTaskDao.deleteAllByFlowId(flow.getId());
    }

    @Override
    public void executeAsync(Job job) {
        appTaskExecutor.execute(() -> {
            NodeTree tree = ymlManager.getTree(job);
            for (LocalTask t : tree.getRoot().getNotifications()) {
                execute(job, t);
            }
        });
    }

    @Override
    public ExecutedLocalTask execute(Job job, LocalTask task) {
        Optional<ExecutedLocalTask> optional =
                executedLocalTaskDao.findByJobIdAndAndName(job.getId(), task.getPlugin());

        if (!optional.isPresent()) {
            throw new StatusException("Executed local task should be init() before execute");
        }

        ExecutedLocalTask exec = optional.get();
        updateStatusTimeAndSave(exec, Executed.Status.RUNNING, null);

        DockerStartOption option = new DockerStartOption();
        option.setImage(DefaultImage);
        option.addEntryPoint("/bin/bash");
        option.addEntryPoint("-c");
        option.getEnv()
                .putAndReturn(Variables.App.Url, serverUrl)
                .putAndReturn(Variables.Agent.Token, ApiAuth.LocalTaskToken)
                .putAndReturn(Variables.Agent.Workspace, "/ws/")
                .putAndReturn(Variables.Agent.PluginDir, "/ws/.plugins")
                .merge(job.getContext())
                .merge(task.getEnvs());

        if (task.hasPlugin()) {
            String name = task.getPlugin();
            GetPluginEvent event = eventManager.publish(new GetPluginAndVerifySetContext(this, name, option.getEnv()));

            if (event.hasError()) {
                String message = event.getError().getMessage();
                log.warn(message);
                updateStatusTimeAndSave(exec, Executed.Status.EXCEPTION, message);
                return exec;
            }

            Plugin plugin = event.getFetched();
            option.addEntryPoint(plugin.getScript());
            option.addBind(event.getDir().toString(), "/ws/.plugins/" + plugin.getName());
            ObjectsHelper.ifNotNull(plugin.getDocker(), (docker) -> option.setImage(docker.getImage()));
        }

        try {
            log.info("Start local task {} image = {} for job {}", task.getPlugin(), option.getImage(), job.getId());
            runDockerTask(option, exec);
            updateStatusTimeAndSave(exec, Executed.Status.SUCCESS, null);
        } catch (Exception e) {
            log.warn(e.getMessage());
            updateStatusTimeAndSave(exec, Executed.Status.EXCEPTION, e.getMessage());
        }

        return exec;
    }

    private void updateStatusTimeAndSave(ExecutedLocalTask t, Executed.Status status, String error) {
        if (t.getStatus() == status) {
            return;
        }

        if (Executed.Status.RUNNING == status) {
            t.setStartAt(new Date());
        }

        if (Executed.FinishStatus.contains(status)) {
            t.setFinishAt(new Date());
        }

        t.setStatus(status);
        t.setError(error);
        executedLocalTaskDao.save(t);

        String jobId = t.getJobId();
        List<ExecutedLocalTask> list = executedLocalTaskDao.findAllByJobId(jobId);
        eventManager.publish(new TaskUpdateEvent(this, t.getJobId(), list, false));
    }

    private void runDockerTask(DockerStartOption option, ExecutedLocalTask r) throws Exception {
        ContainerManager cm = dockerManager.getContainerManager();
        ImageManager im = dockerManager.getImageManager();
        String image = option.getImage();

        try {
            im.pull(image, DefaultTimeout, log::debug);
            String cid = cm.start(option);
            r.setContainerId(cid);

            cm.wait(cid, DefaultTimeout, (frame -> log.debug(new String(frame.getPayload()))));
            Long exitCode = cm.inspect(cid).getState().getExitCodeLong();
            r.setCode(exitCode.intValue());
        } finally {
            if (r.hasContainerId()) {
                cm.delete(r.getContainerId());
            }
        }
    }
}
