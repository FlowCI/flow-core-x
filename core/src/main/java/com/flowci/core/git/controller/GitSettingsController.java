package com.flowci.core.git.controller;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.git.domain.GitSettings;
import com.flowci.core.git.service.GitService;
import com.flowci.exception.UnsupportedException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/gitsettings")
public class GitSettingsController {

    @Autowired
    private GitService gitService;

    @PostMapping("/save")
    public GitSettings save(@RequestBody Request.SaveOptions options) {
        if (options.getSource() == GitSource.GITHUB) {
            return gitService.saveGithubSecret(options.getSecret());
        }

        throw new UnsupportedException("Unsupported git source");
    }
}
