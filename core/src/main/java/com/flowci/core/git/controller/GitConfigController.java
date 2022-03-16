package com.flowci.core.git.controller;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.git.domain.GitConfig;
import com.flowci.core.git.service.GitService;
import com.flowci.exception.ArgumentException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RestController
@RequestMapping("/gitconfig")
public class GitConfigController {

    @Autowired
    private GitService gitService;

    @Action(GitActions.LIST)
    @GetMapping
    public List<GitConfig> list() {
        return gitService.list();
    }

    @Action(GitActions.GET)
    @GetMapping("/{source}")
    public GitConfig get(@PathVariable String source) {
        return gitService.get(GitSource.valueOf(source.toUpperCase()));
    }

    @Action(GitActions.SAVE)
    @PostMapping()
    public GitConfig save(@RequestBody Request.SaveOptions options) {
        if (options.getSource() == GitSource.GERRIT && options.getHost() == null) {
            throw new ArgumentException("Host address is required for Gerrit");
        }

        return gitService.save(options.toGitConfig());
    }

    @Action(GitActions.DELETE)
    @DeleteMapping("/{source}")
    public void delete(@PathVariable String source) {
        gitService.delete(GitSource.valueOf(source.toUpperCase()));
    }
}
