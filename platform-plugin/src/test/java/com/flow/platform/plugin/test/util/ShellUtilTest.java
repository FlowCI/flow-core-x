package com.flow.platform.plugin.test.util;

import com.flow.platform.plugin.test.TestBase;
import com.flow.platform.plugin.util.CmdUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author yh@firim
 */
public class ShellUtilTest extends TestBase{

    @Value("${api.run.indocker}")
    private boolean runInDocker;


    @Test
    public void should_exec_cmd_success() {
        CmdUtil.exeCmd("echo helloworld");
        CmdUtil.exeCmd("pwd");
        CmdUtil.exeCmd("mvn");
    }
}