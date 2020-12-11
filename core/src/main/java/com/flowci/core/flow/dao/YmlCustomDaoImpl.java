package com.flowci.core.flow.dao;

import com.flowci.core.flow.domain.Yml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static com.flowci.core.common.domain.Mongoable.SortByCreatedAtASC;

public class YmlCustomDaoImpl implements YmlCustomDao {

    @Autowired
    private MongoOperations operations;

    @Override
    public List<Yml> findAllWithoutRawByFlowId(String flowId) {
        Query query = Query.query(Criteria.where("flowId").is(flowId)).with(SortByCreatedAtASC);
        query.fields()
                .include("id")
                .include("name")
                .include("flowId")
                .include("conditionInB64")
                .include("createdBy")
                .include("createdAt")
                .include("updatedAt")
                .include("updatedBy");

        return operations.find(query, Yml.class);
    }
}
