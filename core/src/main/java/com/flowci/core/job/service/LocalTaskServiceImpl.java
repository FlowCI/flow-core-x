package com.flowci.core.job.service;

import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.job.dao.ExecutedLocalTaskDao;
import com.flowci.core.job.domain.Executed;
import com.flowci.core.job.domain.ExecutedLocalTask;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.LocalDockerTask;
import com.flowci.core.job.manager.DockerManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.event.GetPluginAndVerifySetContext;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.domain.Notification;
import com.flowci.exception.NotAvailableException;
import com.flowci.exception.StatusException;
import com.flowci.tree.NodeTree;
import com.flowci.util.ObjectsHelper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class LocalTaskServiceImpl implements LocalTaskService {

    @Autowired
    private ExecutedLocalTaskDao executedLocalTaskDao;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private DockerManager dockerManager;

    @Autowired
    private ThreadPoolTaskExecutor localTaskExecutor;

    @Autowired
    private YmlManager ymlManager;

    @Override
    public void init(Job job) {
        NodeTree tree = ymlManager.getTree(job);
        List<Notification> list = tree.getRoot().getNotifications();
        if (list.isEmpty()) {
            return;
        }

        List<ExecutedLocalTask> tasks = new ArrayList<>(list.size());
        for (Notification n : list) {
            if (!n.isEnabled()) {
                continue;
            }

            ExecutedLocalTask t = new ExecutedLocalTask();
            t.setName(n.getPlugin());
            t.setJobId(job.getId());
            tasks.add(t);
        }
        executedLocalTaskDao.insert(tasks);
    }

    @Override
    public List<ExecutedLocalTask> list(Job job) {
        return executedLocalTaskDao.findAllByJobId(job.getId());
    }

    @Override
    public void executeAsync(LocalDockerTask task) {
        localTaskExecutor.execute(() -> execute(task));
    }

    @Override
    public ExecutedLocalTask execute(LocalDockerTask task) {
        Optional<ExecutedLocalTask> optional =
                executedLocalTaskDao.findByJobIdAndAndName(task.getJobId(), task.getName());

        if (!optional.isPresent()) {
            throw new StatusException("Executed local task should be init() before execute");
        }

        ExecutedLocalTask exec = optional.get();
        exec.setStartAt(new Date());
        exec.setStatus(Executed.Status.RUNNING);
        executedLocalTaskDao.save(exec);

        if (task.hasPlugin()) {
            String name = task.getPlugin();
            GetPluginEvent event = eventManager.publish(new GetPluginAndVerifySetContext(this, name, task.getInputs()));

            if (event.hasError()) {
                exec.setError(event.getError().getMessage());
                exec.setStatus(Executed.Status.EXCEPTION);
                exec.setFinishAt(new Date());
                executedLocalTaskDao.save(exec);
                return exec;
            }

            Plugin plugin = event.getFetched();
            task.setScript(plugin.getScript());
            task.setPluginDir(event.getDir());

            // apply docker image only from plugin if it's specified
            ObjectsHelper.ifNotNull(plugin.getDocker(), (docker) -> {
                task.setImage(plugin.getDocker().getImage());
            });
        }

        log.info("Start local task {} image = {} for job {}", task.getName(), task.getImage(), task.getJobId());
        executedLocalTaskDao.save(runDockerTask(task, exec));
        return exec;
    }

    private ExecutedLocalTask runDockerTask(LocalDockerTask task, ExecutedLocalTask r) {
        try {
            String image = task.getImage();

            boolean isSuccess = dockerManager.pullImage(image);
            if (!isSuccess) {
                throw new NotAvailableException("Docker image {0} not available", image);
            }

            String cid = dockerManager.createAndStartContainer(task);
            r.setContainerId(cid);
            dockerManager.printContainerLog(cid);

            if (!dockerManager.waitContainer(cid, task.getTimeoutInSecond())) {
                dockerManager.killContainer(cid);
            }

            r.setCode(dockerManager.getContainerExitCode(cid));
        } catch (InterruptedException | RuntimeException e) {
            log.warn(e.getMessage());
            r.setError(e.getMessage());
            r.setStatus(Executed.Status.EXCEPTION);
        } finally {
            r.setStatus(Executed.Status.SUCCESS);
            r.setFinishAt(new Date());

            if (r.hasContainerId()) {
                dockerManager.removeContainer(r.getContainerId());
            }
        }

        return r;
    }
}
