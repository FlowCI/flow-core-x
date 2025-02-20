package com.flowci.flow.repo;

import com.flowci.flow.model.Flow;
import com.flowci.flow.model.FlowWithBuildStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlowRepo extends JpaRepository<Flow, Long> {

    Optional<Flow> findByName(String name);

    @Query("select f from Flow f " +
            "where f.id in (select fu.flowId from FlowUser fu where fu.userId = ?2) " +
            "and f.parentId = ?1 " +
            "order by f.createdAt")
    List<Flow> findAllByParentIdAndUserIdOrderByCreatedAt(Long parentId, Long userId);

    @Query("select new FlowWithBuildStatus(f.id, f.name, b.status) " +
            "from Flow f " +
            "inner join Build b on f.id = b.flowId " +
            "where f.id = ?1")
    FlowWithBuildStatus findFlowWithBuildStatusByUserId(Long flowId);
}
