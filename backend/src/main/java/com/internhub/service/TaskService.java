package com.internhub.service;

import com.internhub.dto.TaskRequest;
import com.internhub.dto.TaskResponse;
import com.internhub.entity.TaskItem;
import com.internhub.entity.UserAccount;
import com.internhub.enums.Priority;
import com.internhub.enums.Role;
import com.internhub.enums.TaskStatus;
import com.internhub.exception.ForbiddenException;
import com.internhub.exception.NotFoundException;
import com.internhub.repository.TaskItemRepository;
import com.internhub.repository.UserAccountRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TaskService {

    private final TaskItemRepository taskRepository;
    private final UserAccountRepository userRepository;
    private final CurrentUserService currentUserService;

    public TaskService(TaskItemRepository taskRepository, UserAccountRepository userRepository,
            CurrentUserService currentUserService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(Long currentUserId, TaskStatus status, Long assigneeId,
            String keyword, LocalDate dueBefore) {
        UserAccount current = currentUserService.resolve(currentUserId);
        List<TaskItem> source = current.getRole() == Role.MENTOR
                ? taskRepository.findAll()
                : taskRepository.findByAssigneeId(current.getId());

        String normalizedKeyword = normalize(keyword);
        return source.stream()
                .filter(task -> status == null || task.getStatus() == status)
                .filter(task -> assigneeId == null || Objects.equals(task.getAssignee().getId(), assigneeId))
                .filter(task -> dueBefore == null || (task.getDueDate() != null && !task.getDueDate().isAfter(dueBefore)))
                .filter(task -> !StringUtils.hasText(normalizedKeyword) || containsKeyword(task, normalizedKeyword))
                .sorted(taskComparator())
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long currentUserId, Long taskId) {
        UserAccount current = currentUserService.resolve(currentUserId);
        TaskItem task = loadTask(taskId);
        assertCanAccess(current, task);
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse createTask(Long currentUserId, TaskRequest request) {
        UserAccount current = currentUserService.resolve(currentUserId);
        UserAccount assignee = resolveAssignee(current, request.assigneeId());
        TaskItem task = new TaskItem(
                request.title().trim(),
                request.description(),
                defaultStatus(request.status()),
                defaultPriority(request.priority()),
                assignee,
                current,
                request.dueDate());
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTask(Long currentUserId, Long taskId, TaskRequest request) {
        UserAccount current = currentUserService.resolve(currentUserId);
        TaskItem task = loadTask(taskId);
        assertCanAccess(current, task);

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setStatus(defaultStatus(request.status()));
        task.setPriority(defaultPriority(request.priority()));
        task.setDueDate(request.dueDate());
        if (current.getRole() == Role.MENTOR) {
            task.setAssignee(resolveAssignee(current, request.assigneeId()));
        }
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateStatus(Long currentUserId, Long taskId, TaskStatus status) {
        UserAccount current = currentUserService.resolve(currentUserId);
        TaskItem task = loadTask(taskId);
        assertCanAccess(current, task);
        task.setStatus(status);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long currentUserId, Long taskId) {
        UserAccount current = currentUserService.resolve(currentUserId);
        TaskItem task = loadTask(taskId);
        assertCanAccess(current, task);
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public List<TaskItem> visibleTaskEntities(Long currentUserId) {
        UserAccount current = currentUserService.resolve(currentUserId);
        return current.getRole() == Role.MENTOR
                ? taskRepository.findAll()
                : taskRepository.findByAssigneeId(current.getId());
    }

    private TaskItem loadTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }

    private UserAccount resolveAssignee(UserAccount current, Long assigneeId) {
        if (current.getRole() == Role.INTERN) {
            return current;
        }
        Long targetId = assigneeId == null ? current.getId() : assigneeId;
        return userRepository.findById(targetId)
                .orElseThrow(() -> new NotFoundException("Assignee not found"));
    }

    private void assertCanAccess(UserAccount current, TaskItem task) {
        if (current.getRole() == Role.MENTOR) {
            return;
        }
        if (!Objects.equals(task.getAssignee().getId(), current.getId())) {
            throw new ForbiddenException("Interns can only access their own tasks");
        }
    }

    private TaskStatus defaultStatus(TaskStatus status) {
        return status == null ? TaskStatus.TODO : status;
    }

    private Priority defaultPriority(Priority priority) {
        return priority == null ? Priority.MEDIUM : priority;
    }

    private boolean containsKeyword(TaskItem task, String keyword) {
        return normalize(task.getTitle()).contains(keyword)
                || normalize(task.getDescription()).contains(keyword)
                || normalize(task.getAssignee().getDisplayName()).contains(keyword);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private Comparator<TaskItem> taskComparator() {
        return Comparator
                .comparing((TaskItem task) -> task.getDueDate(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing((TaskItem task) -> task.getUpdatedAt(), Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
