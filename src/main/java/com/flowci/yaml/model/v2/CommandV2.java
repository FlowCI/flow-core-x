package com.flowci.yaml.model.v2;

import com.flowci.yaml.model.Command;
import lombok.Getter;
import lombok.Setter;

import static org.springframework.util.StringUtils.hasText;

@Getter
@Setter
public class CommandV2 implements Command {

    private String name; // optional

    private String bash; // bash script

    private String pwsh; // powershell script

    public String getBash() {
        return hasText(bash) ? bash.trim() : null;
    }

    public String getPwsh() {
        return hasText(pwsh) ? pwsh.trim() : null;
    }
}
