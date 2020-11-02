package com.flowci.core.job.controller;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobAction;
import com.flowci.core.job.domain.JobArtifact;
import com.flowci.core.job.service.ArtifactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class ArtifactController extends BaseController {

    @Autowired
    private ArtifactService artifactService;

    @GetMapping("/{flow}/{buildNumber}/artifacts")
    @Action(JobAction.LIST_ARTIFACTS)
    public List<JobArtifact> listArtifact(@PathVariable String flow, @PathVariable String buildNumber) {
        Job job = getJob(flow, buildNumber);
        return artifactService.list(job);
    }

    @GetMapping(value = "/{flow}/{buildNumber}/artifacts/{artifactId}")
    @Action(JobAction.DOWNLOAD_ARTIFACT)
    public ResponseEntity<Resource> downloadArtifact(@PathVariable String flow,
                                                     @PathVariable String buildNumber,
                                                     @PathVariable String artifactId) {
        Job job = getJob(flow, buildNumber);
        JobArtifact artifact = artifactService.fetch(job, artifactId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + artifact.getFileName() + "\"")
                .body(new InputStreamResource(artifact.getSrc()));
    }
}
