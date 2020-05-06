package com.flowci.core.config;

import com.flowci.core.config.domain.Config;
import com.flowci.core.config.domain.CreateSmtp;
import com.flowci.core.config.domain.SmtpConfig;
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
    public List<Config> list() {
        return configService.list();
    }

    @PostMapping
    public Config create(@Validated @RequestBody CreateSmtp body) {
        SmtpConfig config = body.toConfig();
        return configService.create(config);
    }

    @GetMapping("/{name}")
    public Config get(@PathVariable String name) {
        return configService.get(name);
    }
}
