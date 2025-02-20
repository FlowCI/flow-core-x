package com.flowci.agent;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/agents")
@Tag(name = "agent")
@AllArgsConstructor
public class AgentController {
}
