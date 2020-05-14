package com.flowci.core.task;

import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.domain.ScriptBody;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Log4j2
@Service
public class LocalTaskServiceImpl implements LocalTaskService {

    @Autowired
    private SpringEventManager eventManager;

    @Override
    public void execute(LocalTask task) {
        if (task.hasPlugin()) {
            Plugin plugin = getPlugin(task);

            Optional<String> validate = plugin.verifyInput(task.getInputs());
            if (validate.isPresent()) {
                throw new ArgumentException("The illegal input {0} for plugin {1}", validate.get(), plugin.getName());
            }

            ScriptBody body = (ScriptBody) plugin.getBody();
            task.setScript(body.getScript());
        }



    }

    private Plugin getPlugin(LocalTask task) {
        String name = task.getPlugin();
        GetPluginEvent event = eventManager.publish(new GetPluginEvent(this, name));
        if (Objects.isNull(event.getPlugin())) {
            throw new NotFoundException("The plugin {0} defined in local task not found", name);
        }
        return event.getPlugin();
    }
}
