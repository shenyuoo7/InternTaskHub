package com.internhub.config;

import com.internhub.entity.TaskItem;
import com.internhub.entity.UserAccount;
import com.internhub.enums.Priority;
import com.internhub.enums.Role;
import com.internhub.enums.TaskStatus;
import com.internhub.repository.TaskItemRepository;
import com.internhub.repository.UserAccountRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(UserAccountRepository userRepository,
            TaskItemRepository taskRepository) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            UserAccount daoshiA = userRepository.save(new UserAccount("daoshiA", "导师A", "123456", Role.MENTOR, "#2563eb"));
            UserAccount daoshiB = userRepository.save(new UserAccount("daoshiB", "导师B", "123456", Role.MENTOR, "#7c3aed"));
            UserAccount xiaozhao = userRepository.save(new UserAccount("xiaozhao", "实习生小赵", "123456", Role.INTERN, "#0f766e"));
            UserAccount xiaoli = userRepository.save(new UserAccount("xiaoli", "实习生小李", "123456", Role.INTERN, "#d97706"));
            UserAccount xiaowang = userRepository.save(new UserAccount("xiaowang", "实习生小王", "123456", Role.INTERN, "#dc2626"));
            UserAccount xiaoliu = userRepository.save(new UserAccount("xiaoliu", "实习生小刘", "123456", Role.INTERN, "#0891b2"));

            taskRepository.saveAll(List.of(
                    new TaskItem("完成登录页面中文化",
                            "将登录、注册、错误提示等页面文案改为中文，并优化登录页布局，方便面试演示。",
                            TaskStatus.TODO, Priority.HIGH, xiaozhao, daoshiA, LocalDate.parse("2026-07-02")),
                    new TaskItem("接入任务列表查询接口",
                            "完成前端任务列表与后端接口对接，支持按状态、负责人和截止日期筛选。",
                            TaskStatus.IN_PROGRESS, Priority.HIGH, xiaoli, daoshiA, LocalDate.parse("2026-07-03")),
                    new TaskItem("编写项目 README 和启动说明",
                            "补充项目背景、功能介绍、技术栈、启动命令、演示账号和接口说明。",
                            TaskStatus.TODO, Priority.MEDIUM, xiaowang, daoshiB, LocalDate.parse("2026-07-04")),
                    new TaskItem("修复任务状态切换后不刷新的问题",
                            "修复任务从“进行中”切换到“已完成”后，页面没有及时刷新或状态显示不一致的问题。",
                            TaskStatus.IN_PROGRESS, Priority.HIGH, xiaoli, daoshiA, LocalDate.parse("2026-07-01")),
                    new TaskItem("设计任务详情页展示区域",
                            "在任务详情页展示任务标题、负责人、创建人、状态、优先级、截止日期、描述和关联资讯。",
                            TaskStatus.TODO, Priority.MEDIUM, xiaozhao, daoshiB, LocalDate.parse("2026-07-05")),
                    new TaskItem("测试任务创建、编辑、删除流程",
                            "验证任务 CRUD 全流程是否正常，包括创建任务、编辑任务、删除任务和异常输入提示。",
                            TaskStatus.DONE, Priority.MEDIUM, xiaowang, daoshiB, LocalDate.parse("2026-06-30")),
                    new TaskItem("接入实时资讯 RSS 数据源",
                            "调用公开 RSS 或第三方 API 获取技术资讯，展示资讯标题、来源、发布时间和原文链接。",
                            TaskStatus.DONE, Priority.HIGH, xiaoliu, daoshiA, LocalDate.parse("2026-06-29")),
                    new TaskItem("优化任务看板三列布局",
                            "优化待办、进行中、已完成三列看板布局，让任务状态更直观，适合现场演示。",
                            TaskStatus.IN_PROGRESS, Priority.LOW, xiaozhao, daoshiA, LocalDate.parse("2026-07-06")),
                    new TaskItem("准备项目面试演示脚本",
                            "准备 3 分钟演示流程，包括登录、创建任务、筛选任务、状态切换、资讯关联和项目亮点介绍。",
                            TaskStatus.TODO, Priority.HIGH, xiaowang, daoshiB, LocalDate.parse("2026-07-02")),
                    new TaskItem("增加任务关键词搜索功能",
                            "支持按任务标题和描述关键词搜索任务，提高任务定位效率。",
                            TaskStatus.TODO, Priority.MEDIUM, xiaoli, daoshiA, LocalDate.parse("2026-07-07")),
                    new TaskItem("实现资讯与任务的关联展示",
                            "在任务表单或任务详情页中支持选择相关资讯，并展示该任务关联的资讯内容。",
                            TaskStatus.IN_PROGRESS, Priority.HIGH, xiaoliu, daoshiB, LocalDate.parse("2026-07-03")),
                    new TaskItem("整理测试用例和验收清单",
                            "整理系统验收清单，覆盖登录注册、任务 CRUD、任务筛选、状态流转和资讯模块。",
                            TaskStatus.DONE, Priority.MEDIUM, xiaowang, daoshiB, LocalDate.parse("2026-06-30"))));

        };
    }
}
