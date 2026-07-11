<script setup lang="ts">
import { Bell, EditPen, Plus, Promotion, RefreshLeft, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  createNotice,
  fetchEnrollmentCourseOptions,
  fetchNoticePage,
  publishNotice,
  revokeNotice,
  updateNotice,
  type NoticePayload,
} from '../api/training'
import { getErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import type { CourseNoticeRecord, CourseOptionRecord } from '../types/api'

const authStore = useAuthStore()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const courseOptions = ref<CourseOptionRecord[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  courseId: undefined as number | undefined,
})

const pageData = reactive({
  list: [] as CourseNoticeRecord[],
  total: 0,
})

const form = reactive<NoticePayload>({
  courseId: 0,
  title: '',
  content: '',
  registrationStartAt: '',
  registrationEndAt: '',
  externalPublishFlag: false,
})

const rules: FormRules<NoticePayload> = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  title: [{ required: true, message: '请输入通知标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入通知内容', trigger: 'blur' }],
}

const isStudentView = computed(() => authStore.hasRole('STUDENT'))
const publishedCount = computed(() => pageData.list.filter((item) => item.status === 'PUBLISHED').length)
const pageTag = computed(() => (isStudentView.value ? '培训通知' : '通知维护'))
const pageTitle = computed(() => (isStudentView.value ? '培训通知' : '通知发布'))
const pageDescription = computed(() =>
  isStudentView.value
    ? '学员可查看已发布通知，了解课程安排、报名窗口与培训地点，并直接进入报名流程。'
    : '执行人负责编辑通知内容、设置报名时间窗口，并将已发布课程的通知推送给学员。',
)

async function loadCourseOptions() {
  if (isStudentView.value) {
    courseOptions.value = []
    return
  }
  const response = await fetchEnrollmentCourseOptions()
  courseOptions.value = response.data
}

async function loadData() {
  loading.value = true
  try {
    const response = await fetchNoticePage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      courseId: query.courseId,
    })
    pageData.list = response.data.list
    pageData.total = response.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  void loadData()
}

function resetForm() {
  editingId.value = null
  form.courseId = 0
  form.title = ''
  form.content = ''
  form.registrationStartAt = ''
  form.registrationEndAt = ''
  form.externalPublishFlag = false
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: CourseNoticeRecord) {
  editingId.value = row.id
  form.courseId = row.courseId
  form.title = row.title
  form.content = row.content
  form.registrationStartAt = row.registrationStartAt ?? ''
  form.registrationEndAt = row.registrationEndAt ?? ''
  form.externalPublishFlag = row.externalPublishFlag
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    const payload = {
      ...form,
      registrationStartAt: form.registrationStartAt || undefined,
      registrationEndAt: form.registrationEndAt || undefined,
    }
    if (editingId.value) {
      await updateNotice(editingId.value, payload)
      ElMessage.success('通知已更新')
    } else {
      await createNotice(payload)
      ElMessage.success('通知已创建')
    }
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, editingId.value ? '通知更新失败，请稍后重试' : '通知创建失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

async function handlePublish(row: CourseNoticeRecord) {
  try {
    await publishNotice(row.id)
    ElMessage.success('通知已发布')
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '通知发布失败，请稍后重试'))
  }
}

async function handleRevoke(row: CourseNoticeRecord) {
  try {
    await revokeNotice(row.id)
    ElMessage.success('通知已撤回')
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '通知撤回失败，请稍后重试'))
  }
}

function goEnroll(row: CourseNoticeRecord) {
  void router.push({
    path: '/enrollments',
    query: { courseId: String(row.courseId) },
  })
}

function formatTime(value: string | null) {
  return value ? value.replace('T', ' ') : '-'
}

function statusText(value: string) {
  return value === 'PUBLISHED' ? '已发布' : '草稿'
}

function statusTagType(value: string) {
  return value === 'PUBLISHED' ? 'success' : 'warning'
}

function handleDialogClosed() {
  formRef.value?.resetFields()
  resetForm()
}

onMounted(async () => {
  await Promise.all([loadCourseOptions(), loadData()])
})
</script>

<template>
  <div class="module-page">
    <section class="page-card summary-card">
      <div>
        <div class="module-tag">{{ pageTag }}</div>
        <h2>{{ pageTitle }}</h2>
        <p>{{ pageDescription }}</p>
      </div>
      <div class="summary-metrics">
        <div class="metric-item">
          <span>通知总数</span>
          <strong>{{ pageData.total }}</strong>
        </div>
        <div class="metric-item">
          <span>已发布</span>
          <strong>{{ publishedCount }}</strong>
        </div>
      </div>
    </section>

    <section class="page-card toolbar-card">
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索课程编号 / 课程名称 / 通知标题"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="query.status" clearable placeholder="通知状态">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
        </el-select>
        <el-select v-if="!isStudentView" v-model="query.courseId" clearable placeholder="课程筛选">
          <el-option
            v-for="course in courseOptions"
            :key="course.id"
            :label="`${course.courseName} / ${course.courseNo}`"
            :value="course.id"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
      <el-button v-if="!isStudentView" type="primary" :icon="Plus" @click="openCreateDialog">新建通知</el-button>
    </section>

    <section class="page-card table-card">
      <el-table v-loading="loading" :data="pageData.list" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="notice-detail">
              <div><strong>通知内容：</strong>{{ row.content }}</div>
              <div><strong>报名窗口：</strong>{{ formatTime(row.registrationStartAt) }} 至 {{ formatTime(row.registrationEndAt) }}</div>
              <div><strong>发布时间：</strong>{{ formatTime(row.publishedAt) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="courseNo" label="课程编号" min-width="150" />
        <el-table-column prop="courseName" label="课程名称" min-width="220" />
        <el-table-column prop="title" label="通知标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="报名窗口" min-width="240">
          <template #default="{ row }">
            {{ formatTime(row.registrationStartAt) }} 至 {{ formatTime(row.registrationEndAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="通知状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdByName" label="发布人" width="120" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button
                v-if="isStudentView"
                link
                type="primary"
                :icon="Bell"
                @click="goEnroll(row)"
              >
                去报名
              </el-button>
              <template v-else>
                <el-button v-if="row.status === 'DRAFT'" link type="primary" :icon="EditPen" @click="openEditDialog(row)">
                  编辑
                </el-button>
                <el-button v-if="row.status === 'DRAFT'" link type="success" :icon="Promotion" @click="handlePublish(row)">
                  发布
                </el-button>
                <el-button v-if="row.status === 'PUBLISHED'" link type="warning" :icon="RefreshLeft" @click="handleRevoke(row)">
                  撤回
                </el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="[5, 10, 20]"
          :total="pageData.total"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑培训通知' : '新建培训通知'" width="820px" @closed="handleDialogClosed">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="培训课程" prop="courseId">
          <el-select v-model="form.courseId" placeholder="请选择已发布课程" style="width: 100%">
            <el-option
              v-for="course in courseOptions"
              :key="course.id"
              :label="`${course.courseName} / ${course.courseNo} / ${formatTime(course.startTime)}`"
              :value="course.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="通知标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入通知标题" />
        </el-form-item>
        <el-form-item label="通知内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            maxlength="4000"
            show-word-limit
            placeholder="请填写课程安排、对象范围、培训地点、报名要求等内容"
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="报名开始">
              <el-date-picker
                v-model="form.registrationStartAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="请选择报名开始时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报名截止">
              <el-date-picker
                v-model="form.registrationEndAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="请选择报名截止时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="发布渠道">
          <el-switch v-model="form.externalPublishFlag" inline-prompt active-text="外部" inactive-text="站内" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.module-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.summary-card,
.toolbar-card,
.table-card {
  padding: 22px 24px;
}

.summary-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.module-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-weight: 600;
}

h2 {
  margin: 14px 0 8px;
  font-size: 28px;
  color: #0f172a;
}

p {
  margin: 0;
  color: #64748b;
}

.summary-metrics {
  display: flex;
  gap: 16px;
  min-width: 260px;
}

.metric-item {
  flex: 1;
  padding: 18px;
  border-radius: 18px;
  background: #f8fafc;
}

.metric-item span {
  color: #64748b;
}

.metric-item strong {
  display: block;
  margin-top: 10px;
  font-size: 28px;
  color: #0f172a;
}

.toolbar-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.filter-row {
  display: flex;
  gap: 12px;
  flex: 1;
  flex-wrap: wrap;
}

.filter-row .el-input,
.filter-row .el-select {
  max-width: 280px;
}

.notice-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 14px;
  color: #475569;
  line-height: 1.8;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 1080px) {
  .summary-card,
  .toolbar-card {
    flex-direction: column;
  }

  .summary-metrics {
    min-width: 0;
  }
}
</style>
