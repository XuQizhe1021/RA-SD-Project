<script setup lang="ts">
import { Check, EditPen, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import {
  createCourse,
  fetchApprovedApplicationOptions,
  fetchCoursePage,
  fetchLecturerOptions,
  publishCourse,
  updateCourse,
  type CoursePayload,
} from '../api/training'
import { getErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import type { ApplicationOptionRecord, CourseRecord, LecturerRecord } from '../types/api'

const authStore = useAuthStore()
const route = useRoute()
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const lecturerOptions = ref<LecturerRecord[]>([])
const applicationOptions = ref<ApplicationOptionRecord[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  lecturerId: undefined as number | undefined,
})

const pageData = reactive({
  list: [] as CourseRecord[],
  total: 0,
})

const form = reactive<CoursePayload>({
  applicationId: null,
  courseName: '',
  lecturerId: undefined,
  startTime: '',
  endTime: '',
  location: '',
  quota: 30,
  feeAmount: 0,
})

const rules: FormRules<CoursePayload> = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  location: [{ required: true, message: '请输入培训地点', trigger: 'blur' }],
}

const publishedCount = computed(() => pageData.list.filter((item) => item.status === 'PUBLISHED').length)
const isManagerView = computed(() => authStore.hasRole('MANAGER'))
const pageTag = computed(() => (isManagerView.value ? '课程总览' : '课程维护'))
const pageTitle = computed(() => (isManagerView.value ? '课程执行总览' : '课程管理'))
const pageDescription = computed(() =>
  isManagerView.value
    ? '经理可查看课程计划、讲师安排与发布进度，用于掌握培训项目执行情况，但不直接修改课程数据。'
    : '执行人负责新增、编辑和发布课程，并可直接承接审批通过的培训申请，继续推进通知、报名和现场执行流程。',
)

async function loadLecturerOptions() {
  const response = await fetchLecturerOptions()
  lecturerOptions.value = response.data
}

async function loadApplicationOptions() {
  if (isManagerView.value) {
    applicationOptions.value = []
    return
  }
  const response = await fetchApprovedApplicationOptions()
  applicationOptions.value = response.data
}

async function loadData() {
  loading.value = true
  try {
    const response = await fetchCoursePage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      lecturerId: query.lecturerId,
    })
    pageData.list = response.data.list
    pageData.total = response.data.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.applicationId = null
  form.courseName = ''
  form.lecturerId = undefined
  form.startTime = ''
  form.endTime = ''
  form.location = ''
  form.quota = 30
  form.feeAmount = 0
}

function openCreateDialog() {
  resetForm()
  applyRouteApplicationPreset()
  dialogVisible.value = true
}

function openEditDialog(row: CourseRecord) {
  editingId.value = row.id
  form.applicationId = row.applicationId
  form.courseName = row.courseName
  form.lecturerId = row.lecturerId ?? undefined
  form.startTime = row.startTime
  form.endTime = row.endTime
  form.location = row.location
  form.quota = row.quota
  form.feeAmount = Number(row.feeAmount ?? 0)
  dialogVisible.value = true
}

function applyRouteApplicationPreset() {
  const routeApplicationId = Number(route.query.applicationId ?? 0)
  if (!routeApplicationId) {
    return
  }
  const matchedApplication = applicationOptions.value.find((item) => item.id === routeApplicationId)
  if (!matchedApplication) {
    return
  }
  form.applicationId = matchedApplication.id
  form.courseName = matchedApplication.topic
  form.quota = matchedApplication.attendeeCount
  form.feeAmount = Number(matchedApplication.budgetAmount ?? 0)
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    if (editingId.value) {
      await updateCourse(editingId.value, { ...form })
      ElMessage.success('课程信息已更新')
    } else {
      await createCourse({ ...form })
      ElMessage.success('课程已新增')
    }
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, editingId.value ? '课程更新失败，请稍后重试' : '课程创建失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

async function handlePublish(row: CourseRecord) {
  await publishCourse(row.id)
  ElMessage.success('课程已发布')
  await loadData()
}

function handleSearch() {
  query.pageNum = 1
  void loadData()
}

function handleDialogClosed() {
  formRef.value?.resetFields()
  resetForm()
}

function formatTime(value: string) {
  return value ? value.replace('T', ' ') : '-'
}

function formatMoney(value: number) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

onMounted(async () => {
  await Promise.all([loadLecturerOptions(), loadApplicationOptions(), loadData()])
})

watch(
  () => route.query.applicationId,
  () => {
    if (!editingId.value && dialogVisible.value) {
      applyRouteApplicationPreset()
    }
  },
)
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
          <span>课程总数</span>
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
          placeholder="搜索课程编号 / 名称 / 地点 / 讲师"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="query.status" clearable placeholder="课程状态">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
        </el-select>
        <el-select v-model="query.lecturerId" clearable placeholder="授课讲师">
          <el-option
            v-for="lecturer in lecturerOptions"
            :key="lecturer.id"
            :label="`${lecturer.fullName} / ${lecturer.title || '未设置职称'}`"
            :value="lecturer.id"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
      <el-button v-if="!isManagerView" type="primary" :icon="Plus" @click="openCreateDialog">新增课程</el-button>
    </section>

    <section class="page-card table-card">
      <el-table v-loading="loading" :data="pageData.list" stripe>
        <el-table-column prop="courseNo" label="课程编号" min-width="150" />
        <el-table-column prop="courseName" label="课程名称" min-width="220" />
        <el-table-column prop="lecturerName" label="授课讲师" min-width="130" />
        <el-table-column prop="location" label="培训地点" min-width="180" show-overflow-tooltip />
        <el-table-column prop="startTime" label="开始时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="quota" label="名额" width="90" />
        <el-table-column prop="feeAmount" label="费用" width="120">
          <template #default="{ row }">
            {{ formatMoney(row.feeAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button v-if="!isManagerView" link type="primary" :icon="EditPen" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                v-if="!isManagerView && row.status === 'DRAFT'"
                link
                type="success"
                :icon="Check"
                @click="handlePublish(row)"
              >
                发布
              </el-button>
              <span v-if="isManagerView" class="handled-text">当前账号可查看课程执行情况</span>
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

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑课程' : '新增课程'"
      width="780px"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" placeholder="请输入课程名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源申请">
              <el-select v-model="form.applicationId" clearable placeholder="可选择已审批申请" style="width: 100%">
                <el-option
                  v-for="application in applicationOptions"
                  :key="application.id"
                  :label="`${application.topic} / ${application.companyName} / ${application.applicationNo}`"
                  :value="application.id"
                />
              </el-select>
              <div class="field-hint">选择申请后会自动带入主题、人数和预算，可继续按建课需要微调。</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="授课讲师">
              <el-select v-model="form.lecturerId" clearable placeholder="请选择讲师" style="width: 100%">
                <el-option
                  v-for="lecturer in lecturerOptions"
                  :key="lecturer.id"
                  :label="`${lecturer.fullName} / ${lecturer.specialty || '未填写专长'}`"
                  :value="lecturer.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="培训地点" prop="location">
              <el-input v-model="form.location" placeholder="请输入培训地点" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="请选择开始时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="请选择结束时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="培训名额">
              <el-input-number v-model="form.quota" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="培训费用">
              <el-input-number v-model="form.feeAmount" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
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
  min-width: 240px;
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

.action-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.field-hint {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.handled-text {
  color: #64748b;
  font-size: 13px;
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
