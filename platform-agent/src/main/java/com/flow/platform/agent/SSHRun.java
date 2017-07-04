package com.flow.platform.agent;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by gy@fir.im on 04/07/2017.
 * Copyright fir.im
 */

@Deprecated // mark to deprecated only for test ssh
public class SSHRun {

    public static void main(String[] args) throws Throwable {
        byte[] bytes = Files.readAllBytes(Paths.get("/Users/yang/.ssh/id_rsa"));
        String key = new String(bytes);

        SSH ssh = new SSH("52.80.53.164", 22, "ubuntu", key);
        Shell.Plain shell = new Shell.Plain(ssh);
        System.out.println(shell.exec("whoami"));

        new Shell.Safe(ssh).exec("whoami", null, null, null);
    }
}
