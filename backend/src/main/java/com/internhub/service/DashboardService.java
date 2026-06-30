package com.internhub.service;

import com.internhub.dto.DashboardSummary;
import com.internhub.entity.TaskItem;
import com.internhub.enums.Priority;
import com.internhub.enums.TaskStatus;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final TaskService taskService;

    public DashboardService(TaskService taskService) {
        this.taskService = taskService;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(Long currentUserId) {
        List<TaskItem> tasks = taskService.visibleTaskEntities(currentUserId);
        LocalDate today = LocalDate.now();
        long total = tasks.size();
        long todo = countStatus(tasks, TaskStatus.TODO);
        long inProgress = countStatus(tasks, TaskStatus.IN_PROGRESS);
        long done = countStatus(tasks, TaskStatus.DONE);
        long overdue = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(today))
                .count();
        long dueSoon = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .filter(task -> task.getDueDate() != null)
                .filter(task -> !task.getDueDate().isBefore(today) && !task.getDueDate().isAfter(today.plusDays(3)))
                .count();
        double completionRate = total == 0 ? 0 : Math.round(done * 10000.0 / total) / 100.0;

        Map<String, Long> byPriority = Arrays.stream(Priority.values())
                .collect(Collectors.toMap(Priority::name, priority -> tasks.stream()
                        .map(TaskItem::getPriority)
                        .filter(priority::equals)
                        .count()));

        return new DashboardSummary(total, todo, inProgress, done, overdue, dueSoon, completionRate, byPriority);
    }

    private long countStatus(List<TaskItem> tasks, TaskStatus status) {
        return tasks.stream()
                .map(TaskItem::getStatus)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .getOrDefault(status, 0L);
    }
}
