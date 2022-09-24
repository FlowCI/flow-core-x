package com.flowci.core.job.dao;

import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

public class CustomJobDaoImpl implements CustomJobDao {

    @Autowired
    private MongoOperations operations;

    @Override
    public void increaseNumOfArtifact(String jobId) {
        operations.findAndModify(
                query(where("_id").is(jobId)),
                new Update().inc("numOfArtifact", 1),
                Job.class);
    }

    @Override
    public List<Job> list(Collection<JobNumber> numbers) {
        List<String> flowIdList = new ArrayList<>(numbers.size());
        List<Long> buildNumList = new ArrayList<>(numbers.size());

        for (JobNumber num : numbers) {
            flowIdList.add(num.getFlowId());
            buildNumList.add(num.getNumber());
        }

        Criteria criteria = where("flowId").in(flowIdList)
                .andOperator(where("buildNumber").in(buildNumList));

        return operations.find(query(criteria), Job.class);
    }
}
