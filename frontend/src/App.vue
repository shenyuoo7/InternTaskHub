<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authApi, dashboardApi, newsApi, readStoredUser, taskApi, userApi } from './api/client'

const currentUser = ref(readStoredUser())
const activeView = ref('dashboard')
const loginLoading = ref(false)
const pageLoading = ref(false)
const taskSaving = ref(false)
const newsLoading = ref(false)
const users = ref([])
const tasks = ref([])
const news = ref([])
const relatedNews = ref([])
const dashboard = ref({
  total: 0,
  todo: 0,
  inProgress: 0,
  done: 0,
  overdue: 0,
  dueSoon: 0,
  completionRate: 0,
  byPriority: { HIGH: 0, MEDIUM: 0, LOW: 0 },
})

const viewMode = ref('card')
const taskDialogVisible = ref(false)
const taskFormRef = ref()
const chartEl = ref()
let chartInstance

const filters = reactive({
  keyword: '',
  status: '',
  assigneeId: '',
  dueBefore: '',
})

const taskForm = reactive({
  id: null,
  title: '',
  description: '',
  status: 'TODO',
  priority: 'MEDIUM',
  assigneeId: '',
  dueDate: '',
})

const newsKeyword = ref('Spring Boot')

const statusOptions = [
  { label: '待办', value: 'TODO', type: 'info' },
  { label: '进行中', value: 'IN_PROGRESS', type: 'warning' },
  { label: '已完成', value: 'DONE', type: 'success' },
]

const priorityOptions = [
  { label: '高', value: 'HIGH', type: 'danger' },
  { label: '中', value: 'MEDIUM', type: 'warning' },
  { label: '低', value: 'LOW', type: 'success' },
]

const quickUsers = [
  { username: 'mentor', title: '导师登录', note: '可查看和分配所有任务' },
  { username: 'intern', title: '实习生登录', note: '仅查看自己的任务' },
]

const menuItems = [
  { key: 'dashboard', label: '个人仪表盘', icon: 'DataAnalysis' },
  { key: 'tasks', label: '任务管理', icon: 'List' },
  { key: 'news', label: '实施资讯', icon: 'Connection' },
]

const isMentor = computed(() => currentUser.value?.role === 'MENTOR')
const activeTitle = computed(() => menuItems.find((item) => item.key === activeView.value)?.label || '')
const currentRoleText = computed(() => (isMentor.value ? '导师' : '实习生'))

const urgentTasks = computed(() =>
  tasks.value
    .filter((task) => task.status !== 'DONE' && task.dueDate)
    .map((task) => ({ ...task, deadline: deadlineState(task) }))
    .filter((task) => task.deadline.level === 'danger' || task.deadline.level === 'warning')
    .slice(0, 6),
)

const taskRules = {
  title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  assigneeId: [{ required: true, message: '请选择负责人', trigger: 'change' }],
}

onMounted(async () => {
  if (currentUser.value) {
    await loadWorkspace()
  }
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
})

watch([dashboard, activeView], () => {
  nextTick(renderChart)
}, { deep: true })

async function quickLogin(username) {
  loginLoading.value = true
  try {
    const response = await authApi.login(username)
    currentUser.value = response.user
    localStorage.setItem('ith-user', JSON.stringify(response.user))
    localStorage.setItem('ith-token', response.token)
    ElMessage.success(`已进入 ${response.user.displayName} 工作台`)
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loginLoading.value = false
  }
}

function logout() {
  localStorage.removeItem('ith-user')
  localStorage.removeItem('ith-token')
  currentUser.value = null
  tasks.value = []
  activeView.value = 'dashboard'
}

async function loadWorkspace() {
  pageLoading.value = true
  try {
    await Promise.all([loadUsers(), loadTasks(), loadDashboard(), loadNews()])
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    pageLoading.value = false
  }
}

async function loadUsers() {
  users.value = await userApi.list()
}

async function loadTasks() {
  const params = cleanParams({
    keyword: filters.keyword,
    status: filters.status,
    assigneeId: filters.assigneeId,
    dueBefore: filters.dueBefore,
  })
  tasks.value = await taskApi.list(params)
}

async function loadDashboard() {
  dashboard.value = await dashboardApi.summary()
}

async function loadNews() {
  newsLoading.value = true
  try {
    news.value = await newsApi.list(newsKeyword.value)
  } finally {
    newsLoading.value = false
  }
}

async function refreshNews() {
  newsLoading.value = true
  try {
    news.value = await newsApi.refresh(newsKeyword.value)
    ElMessage.success('资讯已刷新')
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    newsLoading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  filters.assigneeId = ''
  filters.dueBefore = ''
  loadTasks()
}

function openCreateTask() {
  Object.assign(taskForm, {
    id: null,
    title: '',
    description: '',
    status: 'TODO',
    priority: 'MEDIUM',
    assigneeId: isMentor.value ? users.value.find((user) => user.role === 'INTERN')?.id || '' : currentUser.value.id,
    dueDate: '',
  })
  relatedNews.value = []
  taskDialogVisible.value = true
}

async function openEditTask(task) {
  Object.assign(taskForm, {
    id: task.id,
    title: task.title,
    description: task.description,
    status: task.status,
    priority: task.priority,
    assigneeId: task.assignee?.id || '',
    dueDate: task.dueDate || '',
  })
  taskDialogVisible.value = true
  await loadRelatedNews(task.title)
}

async function saveTask() {
  await taskFormRef.value?.validate()
  taskSaving.value = true
  try {
    const payload = {
      title: taskForm.title,
      description: taskForm.description,
      status: taskForm.status,
      priority: taskForm.priority,
      assigneeId: taskForm.assigneeId,
      dueDate: taskForm.dueDate || null,
    }
    if (taskForm.id) {
      await taskApi.update(taskForm.id, payload)
      ElMessage.success('任务已更新')
    } else {
      await taskApi.create(payload)
      ElMessage.success('任务已创建')
    }
    taskDialogVisible.value = false
    await Promise.all([loadTasks(), loadDashboard()])
  } catch (error) {
    if (error?.fields) {
      return
    }
    ElMessage.error(errorMessage(error))
  } finally {
    taskSaving.value = false
  }
}

async function updateStatus(task, status) {
  try {
    await taskApi.updateStatus(task.id, status)
    await Promise.all([loadTasks(), loadDashboard()])
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

function nextStatus(status) {
  if (status === 'TODO') return 'IN_PROGRESS'
  if (status === 'IN_PROGRESS') return 'DONE'
  return 'TODO'
}

async function removeTask(task) {
  try {
    await ElMessageBox.confirm(`确定删除任务「${task.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await taskApi.remove(task.id)
    ElMessage.success('任务已删除')
    await Promise.all([loadTasks(), loadDashboard()])
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error(errorMessage(error))
  }
}

async function loadRelatedNews(keyword = taskForm.title) {
  if (!keyword?.trim()) {
    relatedNews.value = []
    return
  }
  try {
    relatedNews.value = await newsApi.related(keyword.trim())
  } catch (error) {
    ElMessage.warning(`相关资讯获取失败：${errorMessage(error)}`)
  }
}

function exportCsv() {
  const rows = [
    ['标题', '状态', '优先级', '负责人', '截止日期', '描述'],
    ...tasks.value.map((task) => [
      task.title,
      statusText(task.status),
      priorityText(task.priority),
      task.assignee?.displayName || '',
      task.dueDate || '',
      task.description || '',
    ]),
  ]
  const csv = `\uFEFF${rows.map((row) => row.map(csvCell).join(',')).join('\n')}`
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `tasks-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

function renderChart() {
  if (activeView.value !== 'dashboard' || !chartEl.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartEl.value)
  }
  chartInstance.setOption({
    color: ['#64748b', '#f59e0b', '#10b981'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, left: 'center' },
    series: [
      {
        name: '任务状态',
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        label: { formatter: '{b}: {c}' },
        data: [
          { name: '待办', value: dashboard.value.todo },
          { name: '进行中', value: dashboard.value.inProgress },
          { name: '已完成', value: dashboard.value.done },
        ],
      },
    ],
  })
}

function resizeChart() {
  chartInstance?.resize()
}

function cleanParams(params) {
  return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined))
}

function statusText(status) {
  return statusOptions.find((item) => item.value === status)?.label || status
}

function statusType(status) {
  return statusOptions.find((item) => item.value === status)?.type || 'info'
}

function priorityText(priority) {
  return priorityOptions.find((item) => item.value === priority)?.label || priority
}

function priorityType(priority) {
  return priorityOptions.find((item) => item.value === priority)?.type || 'info'
}

function deadlineState(task) {
  if (!task.dueDate) return { text: '未设置截止日', level: 'info' }
  const diff = dayDiff(task.dueDate)
  if (task.status === 'DONE') return { text: '已完成', level: 'success' }
  if (diff < 0) return { text: `已逾期 ${Math.abs(diff)} 天`, level: 'danger' }
  if (diff === 0) return { text: '今天截止', level: 'warning' }
  if (diff <= 3) return { text: `${diff} 天后截止`, level: 'warning' }
  return { text: `${diff} 天后截止`, level: 'info' }
}

function dayDiff(dateValue) {
  const today = new Date()
  const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const target = new Date(`${dateValue}T00:00:00`)
  return Math.ceil((target - todayStart) / 86400000)
}

function formatDateTime(value) {
  if (!value) return '暂无时间'
  return value.replace('T', ' ').slice(0, 16)
}

function csvCell(value) {
  return `"${String(value ?? '').replaceAll('"', '""')}"`
}

function errorMessage(error) {
  return error?.response?.data?.message || error?.message || '操作失败'
}
</script>

<template>
  <section v-if="!currentUser" class="login-screen">
    <div class="login-panel">
      <p class="eyebrow">Intern Task Hub</p>
      <h1>实习任务协作台</h1>
      <p class="login-copy">面向导师与实习生的任务分配、进度跟踪和技术资讯关联工作台。</p>
      <div class="quick-login">
        <button
          v-for="user in quickUsers"
          :key="user.username"
          class="login-option"
          :disabled="loginLoading"
          @click="quickLogin(user.username)"
        >
          <span>{{ user.title }}</span>
          <small>{{ user.note }}</small>
        </button>
      </div>
    </div>
  </section>

  <section v-else class="app-shell" v-loading="pageLoading">
    <aside class="sidebar">
      <div>
        <p class="eyebrow">Intern Task Hub</p>
        <h1>任务协作台</h1>
      </div>

      <div class="user-chip">
        <span class="avatar" :style="{ background: currentUser.avatarColor }">
          {{ currentUser.displayName.slice(-1) }}
        </span>
        <div>
          <strong>{{ currentUser.displayName }}</strong>
          <span>{{ currentRoleText }}</span>
        </div>
      </div>

      <nav class="side-nav" aria-label="主导航">
        <button
          v-for="item in menuItems"
          :key="item.key"
          :class="{ active: activeView === item.key }"
          @click="activeView = item.key"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <el-button class="logout-button" plain @click="logout">
        <el-icon><SwitchButton /></el-icon>
        退出
      </el-button>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">{{ currentRoleText }}视角</p>
          <h2>{{ activeTitle }}</h2>
        </div>
        <div class="topbar-actions">
          <el-button @click="loadWorkspace">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="primary" @click="openCreateTask">
            <el-icon><Plus /></el-icon>
            新建任务
          </el-button>
        </div>
      </header>

      <section v-show="activeView === 'dashboard'" class="view-section">
        <div class="metric-grid">
          <article class="metric-card">
            <span>全部任务</span>
            <strong>{{ dashboard.total }}</strong>
          </article>
          <article class="metric-card">
            <span>我的待办</span>
            <strong>{{ dashboard.todo }}</strong>
          </article>
          <article class="metric-card">
            <span>进行中</span>
            <strong>{{ dashboard.inProgress }}</strong>
          </article>
          <article class="metric-card accent">
            <span>完成率</span>
            <strong>{{ dashboard.completionRate }}%</strong>
          </article>
        </div>

        <div class="dashboard-grid">
          <section class="panel chart-panel">
            <div class="panel-header">
              <h3>任务状态分布</h3>
              <el-tag type="success">ECharts</el-tag>
            </div>
            <div ref="chartEl" class="chart"></div>
          </section>

          <section class="panel">
            <div class="panel-header">
              <h3>临期提醒</h3>
              <el-tag :type="dashboard.overdue > 0 ? 'danger' : 'info'">
                逾期 {{ dashboard.overdue }} / 临期 {{ dashboard.dueSoon }}
              </el-tag>
            </div>
            <div v-if="urgentTasks.length" class="urgent-list">
              <button v-for="task in urgentTasks" :key="task.id" class="urgent-row" @click="openEditTask(task)">
                <span>{{ task.title }}</span>
                <el-tag :type="task.deadline.level">{{ task.deadline.text }}</el-tag>
              </button>
            </div>
            <el-empty v-else description="暂无临期任务" :image-size="90" />
          </section>
        </div>
      </section>

      <section v-show="activeView === 'tasks'" class="view-section">
        <div class="toolbar">
          <el-input v-model="filters.keyword" clearable placeholder="搜索标题、描述、负责人" @keyup.enter="loadTasks">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="filters.status" clearable placeholder="状态">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="filters.assigneeId" clearable placeholder="负责人">
            <el-option v-for="user in users" :key="user.id" :label="user.displayName" :value="user.id" />
          </el-select>
          <el-date-picker v-model="filters.dueBefore" value-format="YYYY-MM-DD" type="date" placeholder="截止日前" />
          <el-button type="primary" @click="loadTasks">
            <el-icon><Search /></el-icon>
            筛选
          </el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>

        <div class="sub-toolbar">
          <el-segmented
            v-model="viewMode"
            :options="[
              { label: '卡片', value: 'card' },
              { label: '表格', value: 'table' },
            ]"
          />
          <el-button plain @click="exportCsv">
            <el-icon><Download /></el-icon>
            导出 CSV
          </el-button>
        </div>

        <div v-if="viewMode === 'card'" class="task-grid">
          <article v-for="task in tasks" :key="task.id" class="task-card">
            <div class="task-card-head">
              <el-tag :type="priorityType(task.priority)" effect="dark">{{ priorityText(task.priority) }}</el-tag>
              <el-tag :type="statusType(task.status)">{{ statusText(task.status) }}</el-tag>
            </div>
            <h3>{{ task.title }}</h3>
            <p>{{ task.description || '暂无描述' }}</p>
            <div class="task-meta">
              <span>{{ task.assignee?.displayName }}</span>
              <el-tag :type="deadlineState(task).level">{{ deadlineState(task).text }}</el-tag>
            </div>
            <div class="task-actions">
              <el-button text type="primary" @click="openEditTask(task)">
                <el-icon><Edit /></el-icon>
                详情
              </el-button>
              <el-button text @click="updateStatus(task, nextStatus(task.status))">
                <el-icon><DArrowRight /></el-icon>
                流转
              </el-button>
              <el-button text type="danger" @click="removeTask(task)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </article>
        </div>

        <el-table v-else :data="tasks" class="task-table" stripe>
          <el-table-column prop="title" label="任务" min-width="190" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="100">
            <template #default="{ row }">
              <el-tag :type="priorityType(row.priority)">{{ priorityText(row.priority) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="负责人" min-width="120">
            <template #default="{ row }">{{ row.assignee?.displayName }}</template>
          </el-table-column>
          <el-table-column label="截止提醒" min-width="150">
            <template #default="{ row }">
              <el-tag :type="deadlineState(row).level">{{ deadlineState(row).text }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openEditTask(row)">编辑</el-button>
              <el-button text @click="updateStatus(row, nextStatus(row.status))">流转</el-button>
              <el-button text type="danger" @click="removeTask(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-show="activeView === 'news'" class="view-section">
        <div class="toolbar news-toolbar">
          <el-input v-model="newsKeyword" clearable placeholder="搜索 Java、Spring Boot、Vue 等关键词" @keyup.enter="loadNews">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" :loading="newsLoading" @click="loadNews">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button :loading="newsLoading" @click="refreshNews">
            <el-icon><Refresh /></el-icon>
            刷新 RSS
          </el-button>
        </div>

        <div class="news-list" v-loading="newsLoading">
          <article v-for="item in news" :key="item.id || item.link" class="news-row">
            <div>
              <a :href="item.link" target="_blank" rel="noreferrer">{{ item.title }}</a>
              <p>{{ item.summary || '暂无摘要' }}</p>
              <span>{{ item.source }} · {{ formatDateTime(item.publishedAt || item.fetchedAt) }}</span>
            </div>
            <el-tag>{{ item.keyword || 'RSS' }}</el-tag>
          </article>
          <el-empty v-if="!news.length" description="暂无资讯，可点击刷新 RSS" />
        </div>
      </section>
    </main>

    <el-dialog v-model="taskDialogVisible" :title="taskForm.id ? '任务详情 / 编辑' : '新建任务'" width="860px">
      <div class="dialog-layout">
        <el-form ref="taskFormRef" :model="taskForm" :rules="taskRules" label-position="top">
          <el-form-item label="任务标题" prop="title">
            <el-input v-model="taskForm.title" maxlength="120" show-word-limit @blur="loadRelatedNews()" />
          </el-form-item>
          <el-form-item label="任务描述">
            <el-input v-model="taskForm.description" type="textarea" :rows="4" maxlength="1000" show-word-limit />
          </el-form-item>
          <div class="form-grid">
            <el-form-item label="状态">
              <el-select v-model="taskForm.status">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="优先级">
              <el-select v-model="taskForm.priority">
                <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="负责人" prop="assigneeId">
              <el-select v-model="taskForm.assigneeId" :disabled="!isMentor">
                <el-option v-for="user in users" :key="user.id" :label="user.displayName" :value="user.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="截止日期">
              <el-date-picker v-model="taskForm.dueDate" value-format="YYYY-MM-DD" type="date" placeholder="选择日期" />
            </el-form-item>
          </div>
        </el-form>

        <aside class="related-panel">
          <div class="panel-header">
            <h3>相关资讯</h3>
            <el-button text type="primary" @click="loadRelatedNews()">
              <el-icon><Refresh /></el-icon>
              获取
            </el-button>
          </div>
          <div v-if="relatedNews.length" class="related-list">
            <a v-for="item in relatedNews" :key="item.id || item.link" :href="item.link" target="_blank" rel="noreferrer">
              <strong>{{ item.title }}</strong>
              <span>{{ item.source }}</span>
            </a>
          </div>
          <el-empty v-else description="输入标题后获取相关资讯" :image-size="80" />
        </aside>
      </div>

      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="taskSaving" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
