package com.internhub.config;

import com.internhub.entity.NewsItem;
import com.internhub.entity.TaskItem;
import com.internhub.entity.UserAccount;
import com.internhub.enums.Priority;
import com.internhub.enums.Role;
import com.internhub.enums.TaskStatus;
import com.internhub.repository.NewsItemRepository;
import com.internhub.repository.TaskItemRepository;
import com.internhub.repository.UserAccountRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(UserAccountRepository userRepository,
            TaskItemRepository taskRepository,
            NewsItemRepository newsRepository) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            UserAccount mentor = userRepository.save(new UserAccount("mentor", "导师-倪老师", Role.MENTOR, "#2563eb"));
            UserAccount internA = userRepository.save(new UserAccount("intern", "实习生-小汇", Role.INTERN, "#0f766e"));
            UserAccount internB = userRepository.save(new UserAccount("intern2", "实习生-小信", Role.INTERN, "#d97706"));

            LocalDate today = LocalDate.now();
            taskRepository.saveAll(List.of(
                    new TaskItem("搭建 Spring Boot 任务 API", "完成任务 CRUD、状态流转、异常处理和 H2 初始化数据。",
                            TaskStatus.IN_PROGRESS, Priority.HIGH, internA, mentor, today.plusDays(1)),
                    new TaskItem("设计 Vue 任务列表交互", "实现卡片/表格视图切换，支持按状态、负责人和截止日期筛选。",
                            TaskStatus.TODO, Priority.HIGH, internA, mentor, today.plusDays(2)),
                    new TaskItem("整理 AI 辅助开发说明", "记录 Codex/ChatGPT 的使用边界、验证方式和遇到的问题。",
                            TaskStatus.TODO, Priority.MEDIUM, internB, mentor, today.plusDays(4)),
                    new TaskItem("补充 ECharts 个人仪表盘", "展示我的待办、已完成统计、完成率和临期提醒。",
                            TaskStatus.DONE, Priority.MEDIUM, internA, mentor, today.minusDays(1)),
                    new TaskItem("验证 RSS 实时资讯模块", "从公开 RSS 获取技术资讯，并在任务详情中展示相关内容。",
                            TaskStatus.IN_PROGRESS, Priority.LOW, internB, mentor, today.plusDays(3))));

            LocalDateTime now = LocalDateTime.now();
            newsRepository.saveAll(List.of(
                    new NewsItem("Spring Boot reference documentation",
                            "Official Spring Boot documentation is useful for validating configuration and runtime behavior.",
                            "https://docs.spring.io/spring-boot/index.html",
                            "Spring Docs", "Spring Boot", now.minusDays(2), now),
                    new NewsItem("Vue 3 guide",
                            "The Vue 3 guide covers Composition API patterns used by the frontend implementation.",
                            "https://vuejs.org/guide/introduction.html",
                            "Vue Docs", "Vue", now.minusDays(1), now),
                    new NewsItem("Element Plus component overview",
                            "Element Plus provides form, table, dialog and layout components for the task management UI.",
                            "https://element-plus.org/en-US/component/overview.html",
                            "Element Plus Docs", "Vue Element Plus", now.minusHours(12), now)));
        };
    }
}
