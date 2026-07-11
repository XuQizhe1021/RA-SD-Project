<script setup lang="ts">
import { Check, Close, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import {
  confirmEnrollment,
  createEnrollment,
  fetchEnrollmentCourseOptions,
  fetchEnrollmentPage,
  fetchEnrollmentStudentOptions,
  type EnrollmentConfirmPayload,
  type EnrollmentCreatePayload,
} from '../api/training'
import { getErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import type {
  CourseOptionRecord,
  EnrollmentRecord,
  StudentOptionRecord,
} from '../types/api'

const authStore = useAuthStore()
const route = useRoute()
const loading = ref(false)
const createDialogVisible = ref(false)
const confirmDialogVisible = ref(false)
const submitting = ref(false)
const createFormRef = ref<FormInstance>()
const confirmFormRef = ref<FormInstance>()
const selectedEnrollment = ref<EnrollmentRecord | null>(null)
const courseOptions = ref<CourseOptionRecord[]>([])
const studentOptions = ref<StudentOptionRecord[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  courseId: undefined as number | undefined,
  studentId: undefined as number | undefined,
})

const pageData = reactive({
  list: [] as EnrollmentRecord[],
  total: 0,
})

const createForm = reactive<EnrollmentCreatePayload>({
  courseId: 0,
  studentId: 0,
  paymentType: 'PERSONAL',
})

const confirmForm = reactive<EnrollmentConfirmPayload>({
  approved: true,
  rejectReason: '',
})

const createRules: FormRules<EnrollmentCreatePayload> = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  studentId: [{ required: true, message: '请选择学员', trigger: 'change' }],
  paymentType: [{ required: true, message: '请选择付费类型', trigger: 'change' }],
}

const confirmRules: FormRules<EnrollmentConfirmPayload> = {
  approved: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
}

const confirmedCount = computed(() => pageData.list.filter((item) => item.status === 'CONFIRMED').length)
const pendingCount = computed(() => pageData.list.filter((item) => item.status === 'PENDING').length)
const rejectedCount = computed(() => pageData.list.filter((item) => item.status === 'REJECTED').length)
const isStudentView = computed(() => authStore.hasRole('STUDENT'))
const currentStudentOption = computed(() => studentOptions.value[0] ?? null)
const pageTag = computed(() => (isStudentView.value ? '课程报名' : '报名审核'))
const pageTitle = computed(() => (isStudentView.value ? '我的培训报名' : '学员报名管理'))
const pageDescription = computed(() =>
  isStudentView.value
    ? '学员在此查看可报名课程、提交自己的报名申请，并跟踪审核结果与后续缴费安排。'
    : '执行人负责汇总报名申请、审核通过或驳回，并为后续签到与收费生成准确名单。',
)

async function loadOptions() {
  const [courseResponse, studentResponse] = await Promise.all([
    fetchEnrollmentCourseOptions(),
    fetchEnrollmentStudentOptions(),
  ])
  courseOptions.value = courseResponse.data
  studentOptions.value = studentResponse.data
  if (isStudentView.value && currentStudentOption.value) {
    createForm.studentId = currentStudentOption.value.id
  }
  applyRouteCourseFilter()
}

async function loadData() {
  loading.value = true
  try {
    const response = await fetchEnrollmentPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      courseId: query.courseId,
      studentId: query.studentId,
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

function resetCreateForm() {
  createForm.courseId = query.courseId ?? 0
  createForm.studentId = isStudentView.value ? (currentStudentOption.value?.id ?? 0) : 0
  createForm.paymentType = 'PERSONAL'
}

function resetConfirmForm() {
  confirmForm.approved = true
  confirmForm.rejectReason = ''
  selectedEnrollment.value = null
}

function openCreateDialog() {
  resetCreateForm()
  createDialogVisible.value = true
}

function applyRouteCourseFilter() {
  const routeCourseId = Number(route.query.courseId ?? 0)
  query.courseId = routeCourseId || undefined
}

function openConfirmDialog(row: EnrollmentRecord, approved: boolean) {
  selectedEnrollment.value = row
  confirmForm.approved = approved
  confirmForm.rejectReason = ''
  confirmDialogVisible.value = true
}

async function submitCreateForm() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    await createEnrollment({ ...createForm })
    ElMessage.success('报名记录已创建')
    createDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '报名提交失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

async function submitConfirmForm() {
  const valid = await confirmFormRef.value?.validate().catch(() => false)
  if (!valid || !selectedEnrollment.value) {
    return
  }
  if (!confirmForm.approved && !confirmForm.rejectReason?.trim()) {
    ElMessage.warning('驳回时请填写原因')
    return
  }

  submitting.value = true
  try {
    await confirmEnrollment(selectedEnrollment.value.id, {
      approved: confirmForm.approved,
      rejectReason: confirmForm.approved ? '' : (confirmForm.rejectReason ?? '').trim(),
    })
    ElMessage.success(confirmForm.approved ? '报名已审核通过' : '报名已驳回')
    confirmDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '报名审核失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

function formatTime(value: string | null) {
  return value ? value.replace('T', ' ') : '-'
}

function formatMoney(value: number) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function formatPaymentType(value: string) {
  return value === 'CORPORATE' ? '企业付费' : '个人付费'
}

function statusTagType(value: string) {
  if (value === 'CONFIRMED') {
    return 'success'
  }
  if (value === 'REJECTED') {
    return 'danger'
  }
  return 'warning'
}

function statusText(value: string) {
  if (value === 'CONFIRMED') {
    return '已确认'
  }
  if (value === 'REJECTED') {
    return '已驳回'
  }
  return '待审核'
}

function handleCreateDialogClosed() {
  createFormRef.value?.resetFields()
  resetCreateForm()
}

function handleConfirmDialogClosed() {
  confirmFormRef.value?.resetFields()
  resetConfirmForm()
}

onMounted(async () => {
  await Promise.all([loadOptions(), loadData()])
})

watch(
  () => route.query.courseId,
  async () => {
    applyRouteCourseFilter()
    await loadData()
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
          <span>报名总数</span>
          <strong>{{ pageData.total }}</strong>
        </div>
        <div class="metric-item">
          <span>待审核</span>
          <strong>{{ pendingCount }}</strong>
        </div>
        <div class="metric-item">
          <span>{{ isStudentView ? '已驳回' : '已确认' }}</span>
          <strong>{{ isStudentView ? rejectedCount : confirmedCount }}</strong>
        </div>
      </div>
    </section>

    <section class="page-card toolbar-card">
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索报名编号 / 课程 / 学员 / 公司"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="query.status" clearable placeholder="报名状态">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已确认" value="CONFIRMED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-select v-model="query.courseId" clearable placeholder="课程筛选">
          <el-option
            v-for="course in courseOptions"
            :key="course.id"
            :label="`${course.courseName} / ${course.courseNo}`"
            :value="course.id"
          />
        </el-select>
        <el-select v-if="!isStudentView" v-model="query.studentId" clearable placeholder="学员筛选">
          <el-option
            v-for="student in studentOptions"
            :key="student.id"
            :label="`${student.fullName} / ${student.studentNo}`"
            :value="student.id"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">
        {{ isStudentView ? '我要报名' : '新增报名' }}
      </el-button>
    </section>

    <section class="page-card table-card">
      <el-table v-loading="loading" :data="pageData.list" stripe>
        <el-table-column prop="enrollmentNo" label="报名编号" min-width="150" />
        <el-table-column prop="courseName" label="课程名称" min-width="220" />
        <el-table-column prop="studentName" label="学员姓名" min-width="120" />
        <el-table-column prop="companyName" label="所属公司" min-width="180" show-overflow-tooltip />
        <el-table-column prop="paymentType" label="付费类型" width="110">
          <template #default="{ row }">
            {{ formatPaymentType(row.paymentType) }}
          </template>
        </el-table-column>
        <el-table-column prop="courseFeeAmount" label="课程费用" width="120">
          <template #default="{ row }">
            {{ formatMoney(row.courseFeeAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="courseStartTime" label="开课时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.courseStartTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="confirmedAt" label="审核时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.confirmedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="驳回原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button
                v-if="!isStudentView && row.status === 'PENDING'"
                link
                type="success"
                :icon="Check"
                @click="openConfirmDialog(row, true)"
              >
                通过
              </el-button>
              <el-button
                v-if="!isStudentView && row.status === 'PENDING'"
                link
                type="danger"
                :icon="Close"
                @click="openConfirmDialog(row, false)"
              >
                驳回
              </el-button>
              <span v-if="isStudentView" class="handled-text">
                {{ row.status === 'PENDING' ? '等待执行人审核' : row.status === 'CONFIRMED' ? '报名已通过' : '报名已驳回' }}
              </span>
              <span v-else-if="row.status !== 'PENDING'" class="handled-text">已完成审核</span>
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
      v-model="createDialogVisible"
      title="新增报名"
      width="720px"
      @closed="handleCreateDialogClosed"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="96px">
        <el-form-item label="培训课程" prop="courseId">
          <el-select v-model="createForm.courseId" placeholder="请选择已发布课程" style="width: 100%">
            <el-option
              v-for="course in courseOptions"
              :key="course.id"
              :label="`${course.courseName} / ${course.location} / ${formatTime(course.startTime)}`"
              :value="course.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isStudentView" label="学员信息" prop="studentId">
          <el-select v-model="createForm.studentId" placeholder="请选择学员" style="width: 100%">
            <el-option
              v-for="student in studentOptions"
              :key="student.id"
              :label="`${student.fullName} / ${student.companyName || '未绑定公司'} / ${student.studentNo}`"
              :value="student.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="报名学员">
          <div class="readonly-block">
            {{ currentStudentOption ? `${currentStudentOption.fullName} / ${currentStudentOption.companyName || '未绑定公司'} / ${currentStudentOption.studentNo}` : '正在加载当前学员信息' }}
          </div>
        </el-form-item>
        <el-form-item label="付费类型" prop="paymentType">
          <el-radio-group v-model="createForm.paymentType">
            <el-radio value="PERSONAL">个人付费</el-radio>
            <el-radio value="CORPORATE">企业付费</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreateForm">提交报名</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="confirmDialogVisible"
      :title="confirmForm.approved ? '审核通过报名' : '驳回报名'"
      width="600px"
      @closed="handleConfirmDialogClosed"
    >
      <div class="confirm-summary" v-if="selectedEnrollment">
        <div>报名编号：{{ selectedEnrollment.enrollmentNo }}</div>
        <div>课程名称：{{ selectedEnrollment.courseName }}</div>
        <div>学员姓名：{{ selectedEnrollment.studentName }}</div>
      </div>
      <el-form ref="confirmFormRef" :model="confirmForm" :rules="confirmRules" label-width="96px">
        <el-form-item label="审核结果" prop="approved">
          <el-radio-group v-model="confirmForm.approved">
            <el-radio :value="true">审核通过</el-radio>
            <el-radio :value="false">驳回报名</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="驳回原因" v-if="!confirmForm.approved">
          <el-input
            v-model="confirmForm.rejectReason"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="请填写驳回原因"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitConfirmForm">确认提交</el-button>
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
  background: rgba(249, 115, 22, 0.14);
  color: #c2410c;
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
  min-width: 360px;
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

.handled-text {
  color: #64748b;
  font-size: 13px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.confirm-summary {
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
  color: #334155;
  line-height: 1.8;
}

.readonly-block {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  color: #334155;
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
