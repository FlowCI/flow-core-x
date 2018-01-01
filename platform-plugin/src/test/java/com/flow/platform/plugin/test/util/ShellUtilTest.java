package com.flow.platform.plugin.test.util;

import com.flow.platform.plugin.test.TestBase;
import com.flow.platform.plugin.util.ShellUtil;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class ShellUtilTest extends TestBase{

    @Test
    public void should_exec_cmd_success() {
        ShellUtil.exeCmd("echo helloworld");
        ShellUtil.exeCmd("pwd");
        ShellUtil.exeCmd("mvn");
    }
}