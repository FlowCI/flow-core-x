package com.flowci.core.git.controller;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.git.domain.GitConfig;
import com.flowci.core.git.service.GitConfigService;
import com.flowci.common.exception.ArgumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/gitconfig")
public class GitConfigController {

    @Autowired
    private GitConfigService gitConfigService;

    @Action(GitActions.LIST)
    @GetMapping
    public List<GitConfig> list() {
        return gitConfigService.list();
    }

    @Action(GitActions.GET)
    @GetMapping("/{source}")
    public GitConfig get(@PathVariable String source) {
        return gitConfigService.get(GitSource.valueOf(source.toUpperCase()));
    }

    @Action(GitActions.SAVE)
    @PostMapping()
    public GitConfig save(@RequestBody Request.SaveOptions options) {
        if (options.getSource() == GitSource.GERRIT && options.getHost() == null) {
            throw new ArgumentException("Host address is required for Gerrit");
        }

        return gitConfigService.save(options.toGitConfig());
    }

    @Action(GitActions.DELETE)
    @DeleteMapping("/{source}")
    public void delete(@PathVariable String source) {
        gitConfigService.delete(GitSource.valueOf(source.toUpperCase()));
    }
}
