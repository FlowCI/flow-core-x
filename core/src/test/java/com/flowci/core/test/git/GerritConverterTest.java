package com.flowci.core.test.git;

import com.flowci.core.common.domain.Variables;
import com.flowci.core.git.converter.GerritConverter;
import com.flowci.core.git.domain.GitPatchSetTrigger;
import com.flowci.core.git.domain.GitTrigger;
import com.flowci.core.test.SpringScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GerritConverterTest extends SpringScenario {

    @Autowired
    private GerritConverter gerritConverter;

    @Test
    void should_parse_patchset_create_event() {
        InputStream stream = load("gerrit/patchset_created.json");

        Optional<GitTrigger> optional = gerritConverter.convert(GerritConverter.AllEvent, stream);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof GitPatchSetTrigger);

        GitPatchSetTrigger t = (GitPatchSetTrigger) optional.get();
        assertEquals("task 1 try", t.getSubject());
        assertEquals("task 1 try\n\nChange-Id: I508fde11a59c36f9ab8b086d13d41b5d9c597db6\n", t.getMessage());
        assertEquals("gerrit_test", t.getProject());
        assertEquals("I508fde11a59c36f9ab8b086d13d41b5d9c597db6", t.getChangeId());
        assertEquals(1, t.getChangeNumber().intValue());
        assertEquals("http://192.168.31.173:8088/c/gerrit_test/+/1", t.getChangeUrl());

        assertEquals(2, t.getPatchNumber().intValue());
        assertEquals("http://192.168.31.173:8088/c/gerrit_test/+/1/2", t.getPatchUrl());
        assertEquals("34ebc1c63ef2173b3704663c32bcd14471b68a9b", t.getPatchRevision());
        assertEquals("refs/changes/01/1/2", t.getPatchRef());

        assertEquals("master", t.getBranch());
        assertEquals(GitPatchSetTrigger.Status.NEW, t.getStatus());
        assertEquals("1642965660", t.getCreatedOn());
        assertEquals(3, t.getSizeInsertions().intValue());
        assertEquals(0, t.getSizeDeletions().intValue());

        var vars = t.toVariableMap();
        assertEquals("refs/changes/01/1/2", vars.get(Variables.Git.BRANCH));
    }

    @Test
    void should_parse_patchset_merged_event() {
        InputStream stream = load("gerrit/change_merged.json");

        Optional<GitTrigger> optional = gerritConverter.convert(GerritConverter.AllEvent, stream);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof GitPatchSetTrigger);

        GitPatchSetTrigger t = (GitPatchSetTrigger) optional.get();
        assertEquals(GitPatchSetTrigger.Status.MERGED, t.getStatus());

        var vars = t.toVariableMap();
        assertEquals("master", vars.get(Variables.Git.BRANCH));
    }
}
