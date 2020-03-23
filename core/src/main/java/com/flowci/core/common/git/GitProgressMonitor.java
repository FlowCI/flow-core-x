package com.flowci.core.common.git;

import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.File;

@Log4j2
public class GitProgressMonitor implements ProgressMonitor {

    private final String source;

    private final File target;

    public GitProgressMonitor(String source, File target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public void start(int totalTasks) {
        log.debug("Git - {} start: {}", source, totalTasks);
    }

    @Override
    public void beginTask(String title, int totalWork) {
        log.debug("Git - {} beginTask: {} {}", source, title, totalWork);
    }

    @Override
    public void update(int completed) {
        log.debug("Git - {} update: {}", source, completed);
    }

    @Override
    public void endTask() {
        log.debug("Git - {} endTask on {}", source, target);
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
