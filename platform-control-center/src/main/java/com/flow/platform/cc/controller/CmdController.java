package com.flow.platform.cc.controller;

import com.flow.platform.cc.service.CmdService;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
@RestController
@RequestMapping("/cmd")
public class CmdController {

    @Autowired
    private CmdService cmdService;

    @RequestMapping(path = "/send", method = RequestMethod.POST, consumes = "application/json")
    public Cmd sendCommand(@RequestBody CmdBase cmd) {
        return cmdService.send(cmd);
    }
}
