package com.internhub.repository;

import com.internhub.entity.TaskItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {

    List<TaskItem> findByAssigneeId(Long assigneeId);
}
