package com.flow.platform.cc.controller;

import com.flow.platform.cc.service.CmdService;
import com.flow.platform.domain.CmdResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Set;

/**
 * Created by gy@fir.im on 28/06/2017.
 * Copyright fir.im
 */
@RestController
@RequestMapping("/cmd/result")
public class CmdResultController {

    @Autowired
    private CmdService cmdService;

    /**
     * List cmd result by cmd list
     *
     * @param cmdIdList
     * @return
     */
    @PostMapping(path = "/list", consumes = "application/json")
    public Collection<CmdResult> list(@RequestBody Set<String> cmdIdList) {
        return cmdService.listResult(cmdIdList);
    }
}
