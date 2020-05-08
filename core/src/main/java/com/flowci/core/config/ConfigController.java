package com.flowci.core.config;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.config.domain.Config;
import com.flowci.core.config.domain.ConfigAction;
import com.flowci.core.config.domain.SmtpOption;
import com.flowci.core.config.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/configs")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @GetMapping
    @Action(ConfigAction.LIST)
    public List<Config> list() {
        return configService.list();
    }

    @PostMapping("/{name}/smtp")
    @Action(ConfigAction.SAVE)
    public Config save(@PathVariable String name, @Validated @RequestBody SmtpOption option) {
        return configService.save(name, option);
    }

    @GetMapping("/{name}")
    @Action(ConfigAction.GET)
    public Config get(@PathVariable String name) {
        return configService.get(name);
    }

    @DeleteMapping("/{name}")
    @Action(ConfigAction.DELETE)
    public Config delete(@PathVariable String name) {
        return configService.delete(name);
    }
}
