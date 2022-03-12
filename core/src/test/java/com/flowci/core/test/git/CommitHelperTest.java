package com.flowci.core.test.git;

import com.flowci.core.git.domain.GitCommit;
import com.flowci.core.git.util.CommitHelper;
import org.junit.Test;
import org.junit.Assert;;

public class CommitHelperTest {

    @Test
    public void should_get_change_id_from_commit_message() {
        var msg = "try task 1 patchset\n" +
                "\n" +
                "Change-Id: I9a55b4d3fe33d2efae107a38fc2744bfc95bfea9" +
                "\n";

        var commit = new GitCommit();
        commit.setMessage(msg);

        var changeId = CommitHelper.getChangeId(commit);
        Assert.assertTrue(changeId.isPresent());
        Assert.assertEquals("I9a55b4d3fe33d2efae107a38fc2744bfc95bfea9", changeId.get());
    }
}
