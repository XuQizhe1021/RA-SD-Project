<script setup lang="ts">
import { EditPen, Search, View } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  fetchCourseEvaluationReport,
  fetchEvaluationProxyCandidates,
  fetchEvaluationProxyCourses,
  fetchEvaluationSummaries,
  fetchMyEvaluations,
  fetchPendingEvaluationCourses,
  submitEvaluation,
  type EvaluationSubmitPayload,
} from '../api/training'
import { getErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import type {
  CourseEvaluationReport,
  CourseEvaluationSummary,
  CourseOptionRecord,
  EvaluationCandidateRecord,
  EvaluationRecordView,
  PendingEvaluationCourse,
} from '../types/api'

const authStore = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const reportVisible = ref(false)
const reportLoading = ref(false)
const formRef = ref<FormInstance>()

const pendingCourses = ref<PendingEvaluationCourse[]>([])
const myEvaluations = ref<EvaluationRecordView[]>([])
const proxyCourses = ref<CourseOptionRecord[]>([])
const proxyCandidates = ref<EvaluationCandidateRecord[]>([])
const summaryList = ref<CourseEvaluationSummary[]>([])
const selectedCandidate = ref<EvaluationCandidateRecord | null>(null)
const selectedPendingCourse = ref<PendingEvaluationCourse | null>(null)
const selectedSummary = ref<CourseEvaluationSummary | null>(null)
const reportData = ref<CourseEvaluationReport | null>(null)

const isStudentView = computed(() => authStore.hasRole('STUDENT'))
const isStaffView = computed(() => authStore.hasRole('SITE_STAFF'))
const isManagerView = computed(() => authStore.hasRole('MANAGER') || authStore.hasRole('EXECUTOR'))

const proxyQuery = reactive({
  courseId: undefined as number | undefined,
})

const summaryQuery = reactive({
  keyword: '',
  dateRange: getDefaultDateRange() as [string, string],
  hasEvaluation: 'ALL',
})

const form = reactive<EvaluationSubmitPayload>({
  courseId: 0,
  enrollmentId: 0,
  studentId: undefined,
  rating: 5,
  commentText: '',
})

const rules: FormRules<EvaluationSubmitPayload> = {
  rating: [{ required: true, message: '请选择满意度评分', trigger: 'change' }],
}

const pageTitle = computed(() => {
  if (isStudentView.value) {
    return '我的课程评价'
  }
  if (isStaffView.value) {
    return '评价代录管理'
  }
  return '课程评价汇总'
})

const pageTag = computed(() => {
  if (isStudentView.value) {
    return '学员评价视角'
  }
  if (isStaffView.value) {
    return '现场工作人员视角'
  }
  return '经理 / 执行人视角'
})

const pageDescription = computed(() => {
  if (isStudentView.value) {
    return '课程结束后，学员可对已签到课程提交满意度评分和文字反馈，系统会自动拦截重复评价。'
  }
  if (isStaffView.value) {
    return '现场工作人员可为已签到但尚未提交评价的学员代录反馈，已提交记录会自动锁定，避免重复录入。'
  }
  return '经理与执行人查看课程评价完成度、满意度均分和单课程反馈明细，为评审演示与后续优化提供依据。'
})

const totalCount = computed(() => {
  if (isStudentView.value) {
    return pendingCourses.value.length + myEvaluations.value.length
  }
  if (isStaffView.value) {
    return proxyCandidates.value.length
  }
  return summaryList.value.length
})

const pendingCount = computed(() => {
  if (isStudentView.value) {
    return pendingCourses.value.length
  }
  if (isStaffView.value) {
    return proxyCandidates.value.filter((item) => item.evaluationStatus === 'PENDING').length
  }
  return summaryList.value.reduce((sum, item) => sum + Number(item.shouldEvaluateCount ?? 0), 0)
})

const submittedCount = computed(() => {
  if (isStudentView.value) {
    return myEvaluations.value.length
  }
  if (isStaffView.value) {
    return proxyCandidates.value.filter((item) => item.evaluationStatus === 'SUBMITTED').length
  }
  return summaryList.value.reduce((sum, item) => sum + Number(item.evaluatedCount ?? 0), 0)
})

const averageScore = computed(() => {
  if (isStudentView.value) {
    return calculateAverage(myEvaluations.value.map((item) => item.rating))
  }
  if (isStaffView.value) {
    const submittedScores = proxyCandidates.value.filter((item) => item.evaluationStatus === 'SUBMITTED').length
    return submittedScores ? Number(((submittedScores / Math.max(proxyCandidates.value.length, 1)) * 5).toFixed(1)) : 0
  }
  let weightedScore = 0
  let count = 0
  summaryList.value.forEach((item) => {
    if (item.averageRating != null && item.evaluatedCount > 0) {
      weightedScore += Number(item.averageRating) * item.evaluatedCount
      count += item.evaluatedCount
    }
  })
  return count ? Number((weightedScore / count).toFixed(1)) : 0
})

async function loadStudentData() {
  const [pendingResponse, mineResponse] = await Promise.all([
    fetchPendingEvaluationCourses(),
    fetchMyEvaluations(),
  ])
  pendingCourses.value = pendingResponse.data
  myEvaluations.value = mineResponse.data
}

async function loadProxyCourses() {
  const response = await fetchEvaluationProxyCourses()
  proxyCourses.value = response.data
  if (!proxyQuery.courseId && proxyCourses.value.length) {
    proxyQuery.courseId = proxyCourses.value[0].id
  }
}

async function loadProxyCandidates() {
  if (!proxyQuery.courseId) {
    proxyCandidates.value = []
    return
  }
  const response = await fetchEvaluationProxyCandidates(proxyQuery.courseId)
  proxyCandidates.value = response.data
}

async function loadSummaryData() {
  const response = await fetchEvaluationSummaries({
    keyword: summaryQuery.keyword || undefined,
    startDate: summaryQuery.dateRange?.[0],
    endDate: summaryQuery.dateRange?.[1],
    hasEvaluation:
      summaryQuery.hasEvaluation === 'ALL'
        ? undefined
        : summaryQuery.hasEvaluation === 'YES',
  })
  summaryList.value = response.data
}

async function loadInitialData() {
  loading.value = true
  try {
    if (isStudentView.value) {
      await loadStudentData()
      return
    }
    if (isStaffView.value) {
      await loadProxyCourses()
      await loadProxyCandidates()
      return
    }
    await loadSummaryData()
  } finally {
    loading.value = false
  }
}

async function handleSummarySearch() {
  loading.value = true
  try {
    await loadSummaryData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '评价汇总加载失败'))
  } finally {
    loading.value = false
  }
}

async function handleProxyCourseChange() {
  loading.value = true
  try {
    await loadProxyCandidates()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '待代录名单加载失败'))
  } finally {
    loading.value = false
  }
}

function openStudentDialog(row: PendingEvaluationCourse) {
  selectedPendingCourse.value = row
  selectedCandidate.value = null
  form.courseId = row.courseId
  form.enrollmentId = row.enrollmentId
  form.studentId = undefined
  form.rating = 5
  form.commentText = ''
  dialogVisible.value = true
}

function openProxyDialog(row: EvaluationCandidateRecord) {
  selectedCandidate.value = row
  selectedPendingCourse.value = null
  form.courseId = row.courseId
  form.enrollmentId = row.enrollmentId
  form.studentId = row.studentId
  form.rating = 5
  form.commentText = ''
  dialogVisible.value = true
}

async function openReport(row: CourseEvaluationSummary) {
  selectedSummary.value = row
  reportVisible.value = true
  reportLoading.value = true
  try {
    const response = await fetchCourseEvaluationReport(row.courseId)
    reportData.value = response.data
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '评价报告加载失败'))
    reportVisible.value = false
  } finally {
    reportLoading.value = false
  }
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    await submitEvaluation({
      courseId: form.courseId,
      enrollmentId: form.enrollmentId,
      studentId: form.studentId,
      rating: form.rating,
      commentText: form.commentText?.trim() || undefined,
    })
    ElMessage.success(isStudentView.value ? '评价提交成功' : '代录评价成功')
    dialogVisible.value = false
    if (isStudentView.value) {
      await loadStudentData()
    } else {
      await loadProxyCandidates()
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '评价提交失败'))
  } finally {
    submitting.value = false
  }
}

function handleDialogClosed() {
  formRef.value?.resetFields()
  form.courseId = 0
  form.enrollmentId = 0
  form.studentId = undefined
  form.rating = 5
  form.commentText = ''
  selectedCandidate.value = null
  selectedPendingCourse.value = null
}

function formatTime(value: string | null) {
  return value ? value.replace('T', ' ') : '—'
}

function formatRange(startTime: string, endTime: string) {
  return `${formatTime(startTime)} 至 ${formatTime(endTime)}`
}

function formatRating(value: number | null | undefined) {
  return value == null ? '—' : `${Number(value).toFixed(1)} 分`
}

function formatSource(value: string | null | undefined) {
  if (value === 'STAFF_PROXY') {
    return '现场代录'
  }
  return '学员自评'
}

function formatStatus(value: string) {
  return value === 'SUBMITTED' ? '已评价' : '待评价'
}

function statusTagType(value: string) {
  return value === 'SUBMITTED' ? 'success' : 'warning'
}

function calculateAverage(values: number[]) {
  if (!values.length) {
    return 0
  }
  const total = values.reduce((sum, value) => sum + Number(value || 0), 0)
  return Number((total / values.length).toFixed(1))
}

function getDefaultDateRange() {
  const end = new Date()
  const start = new Date()
  start.setMonth(start.getMonth() - 3)
  return [formatDate(start), formatDate(end)]
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

onMounted(async () => {
  try {
    await loadInitialData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '评价管理数据加载失败'))
  }
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
          <span>{{ isManagerView ? '课程数' : '记录数' }}</span>
          <strong>{{ totalCount }}</strong>
        </div>
        <div class="metric-item">
          <span>{{ isManagerView ? '应评人数' : '待处理' }}</span>
          <strong>{{ pendingCount }}</strong>
        </div>
        <div class="metric-item">
          <span>{{ isManagerView ? '已评人数' : '已提交' }}</span>
          <strong>{{ submittedCount }}</strong>
        </div>
        <div class="metric-item">
          <span>{{ isManagerView ? '综合均分' : '平均得分' }}</span>
          <strong>{{ formatRating(averageScore) }}</strong>
        </div>
      </div>
    </section>

    <section v-if="isManagerView" class="page-card toolbar-card">
      <div class="filter-row">
        <el-input
          v-model="summaryQuery.keyword"
          clearable
          placeholder="搜索课程编号 / 课程名称 / 讲师"
          @keyup.enter="handleSummarySearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-date-picker
          v-model="summaryQuery.dateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
        <el-select v-model="summaryQuery.hasEvaluation" placeholder="评价状态">
          <el-option label="全部课程" value="ALL" />
          <el-option label="已有评价" value="YES" />
          <el-option label="暂无评价" value="NO" />
        </el-select>
        <el-button type="primary" @click="handleSummarySearch">查询</el-button>
      </div>
    </section>

    <section v-if="isStaffView" class="page-card toolbar-card">
      <div class="filter-row">
        <el-select
          v-model="proxyQuery.courseId"
          clearable
          placeholder="请选择已结束课程"
          style="min-width: 320px"
          @change="handleProxyCourseChange"
        >
          <el-option
            v-for="course in proxyCourses"
            :key="course.id"
            :label="`${course.courseName} / ${course.courseNo}`"
            :value="course.id"
          />
        </el-select>
      </div>
      <div class="toolbar-tip">仅展示已签到的学员，已提交评价的记录会自动禁用代录入口。</div>
    </section>

    <section v-if="isStudentView" class="page-card table-card">
      <div class="section-title">待评价课程</div>
      <el-table v-loading="loading" :data="pendingCourses" stripe>
        <el-table-column prop="courseNo" label="课程编号" min-width="140" />
        <el-table-column prop="courseName" label="课程名称" min-width="220" />
        <el-table-column prop="lecturerName" label="授课讲师" min-width="120" />
        <el-table-column label="培训时间" min-width="220">
          <template #default="{ row }">
            {{ formatRange(row.startTime, row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="location" label="培训地点" min-width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="EditPen" @click="openStudentDialog(row)">去评价</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="isStudentView" class="page-card table-card">
      <div class="section-title">已提交评价</div>
      <el-table v-loading="loading" :data="myEvaluations" stripe>
        <el-table-column prop="courseName" label="课程名称" min-width="220" />
        <el-table-column prop="lecturerName" label="授课讲师" min-width="120" />
        <el-table-column prop="rating" label="评分" width="120">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="commentText" label="意见反馈" min-width="260" show-overflow-tooltip />
        <el-table-column prop="source" label="评价方式" width="120">
          <template #default="{ row }">
            {{ formatSource(row.source) }}
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.submittedAt) }}
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="isStaffView" class="page-card table-card">
      <el-table v-loading="loading" :data="proxyCandidates" stripe>
        <el-table-column prop="studentNo" label="学员编号" min-width="140" />
        <el-table-column prop="studentName" label="学员姓名" min-width="120" />
        <el-table-column prop="companyName" label="所属公司" min-width="180" show-overflow-tooltip />
        <el-table-column prop="checkedInAt" label="签到时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.checkedInAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="evaluationStatus" label="评价状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.evaluationStatus)">
              {{ formatStatus(row.evaluationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="evaluationSource" label="提交方式" width="120">
          <template #default="{ row }">
            {{ row.evaluationStatus === 'SUBMITTED' ? formatSource(row.evaluationSource) : '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.submittedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.evaluationStatus === 'PENDING'"
              link
              type="primary"
              :icon="EditPen"
              @click="openProxyDialog(row)"
            >
              代录评价
            </el-button>
            <span v-else class="handled-text">已完成</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="isManagerView" class="page-card table-card">
      <el-table v-loading="loading" :data="summaryList" stripe>
        <el-table-column prop="courseNo" label="课程编号" min-width="140" />
        <el-table-column prop="courseName" label="课程名称" min-width="220" />
        <el-table-column prop="lecturerName" label="授课讲师" min-width="120" />
        <el-table-column label="培训时间" min-width="220">
          <template #default="{ row }">
            {{ formatRange(row.startTime, row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="shouldEvaluateCount" label="应评人数" width="100" />
        <el-table-column prop="evaluatedCount" label="已评人数" width="100" />
        <el-table-column prop="averageRating" label="平均评分" width="110">
          <template #default="{ row }">
            {{ formatRating(row.averageRating) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openReport(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="isStudentView ? '提交课程评价' : '代录课程评价'"
      width="680px"
      @closed="handleDialogClosed"
    >
      <div class="dialog-summary">
        <div v-if="selectedPendingCourse">课程名称：{{ selectedPendingCourse.courseName }}</div>
        <div v-if="selectedPendingCourse">培训时间：{{ formatRange(selectedPendingCourse.startTime, selectedPendingCourse.endTime) }}</div>
        <div v-if="selectedCandidate">学员姓名：{{ selectedCandidate.studentName }}</div>
        <div v-if="selectedCandidate">所属公司：{{ selectedCandidate.companyName || '—' }}</div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="满意度" prop="rating">
          <el-rate v-model="form.rating" :max="5" show-score />
        </el-form-item>
        <el-form-item label="意见反馈">
          <el-input
            v-model="form.commentText"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请输入课程建议、满意点或待改进问题"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ isStudentView ? '提交评价' : '确认代录' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="reportVisible"
      title="课程评价报告"
      size="56%"
      destroy-on-close
    >
      <div v-loading="reportLoading" class="report-panel" v-if="reportData">
        <section class="report-card">
          <div class="report-head">
            <div>
              <div class="module-tag subtle-tag">课程概况</div>
              <h3>{{ reportData.courseName }}</h3>
              <p>{{ reportData.courseNo }} / {{ reportData.lecturerName }} / {{ reportData.location }}</p>
              <p>{{ formatRange(reportData.startTime, reportData.endTime) }}</p>
            </div>
          </div>
          <div class="report-metrics">
            <div class="metric-box">
              <span>应评人数</span>
              <strong>{{ reportData.shouldEvaluateCount }}</strong>
            </div>
            <div class="metric-box">
              <span>已评人数</span>
              <strong>{{ reportData.evaluatedCount }}</strong>
            </div>
            <div class="metric-box">
              <span>参评率</span>
              <strong>{{ Number(reportData.participationRate || 0).toFixed(1) }}%</strong>
            </div>
            <div class="metric-box">
              <span>平均满意度</span>
              <strong>{{ formatRating(reportData.averageRating) }}</strong>
            </div>
          </div>
        </section>

        <section class="report-card">
          <div class="section-title">分值分布</div>
          <div class="distribution-list">
            <div v-for="item in reportData.scoreDistribution" :key="item.score" class="distribution-item">
              <span>{{ item.score }} 分</span>
              <el-progress :percentage="reportData.evaluatedCount ? Number(((item.count / reportData.evaluatedCount) * 100).toFixed(1)) : 0" />
              <strong>{{ item.count }} 人</strong>
            </div>
          </div>
        </section>

        <section class="report-card">
          <div class="section-title">评价明细</div>
          <el-table :data="reportData.details" stripe>
            <el-table-column prop="studentName" label="学员姓名" min-width="120" />
            <el-table-column prop="companyName" label="所属公司" min-width="180" show-overflow-tooltip />
            <el-table-column prop="rating" label="评分" width="120">
              <template #default="{ row }">
                <el-rate :model-value="row.rating" disabled />
              </template>
            </el-table-column>
            <el-table-column prop="commentText" label="意见反馈" min-width="260" show-overflow-tooltip />
            <el-table-column prop="source" label="评价方式" width="110">
              <template #default="{ row }">
                {{ formatSource(row.source) }}
              </template>
            </el-table-column>
            <el-table-column prop="submittedAt" label="提交时间" min-width="170">
              <template #default="{ row }">
                {{ formatTime(row.submittedAt) }}
              </template>
            </el-table-column>
          </el-table>
        </section>
      </div>
    </el-drawer>
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
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
  font-weight: 600;
}

.subtle-tag {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

h2 {
  margin: 14px 0 8px;
  font-size: 28px;
  color: #0f172a;
}

h3 {
  margin: 12px 0 8px;
  font-size: 24px;
  color: #0f172a;
}

p {
  margin: 0;
  color: #64748b;
}

.summary-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  min-width: 520px;
}

.metric-item,
.metric-box {
  padding: 18px;
  border-radius: 18px;
  background: #f8fafc;
}

.metric-item span,
.metric-box span {
  color: #64748b;
}

.metric-item strong,
.metric-box strong {
  display: block;
  margin-top: 10px;
  font-size: 26px;
  color: #0f172a;
}

.toolbar-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.filter-row {
  display: flex;
  gap: 12px;
  flex: 1;
  flex-wrap: wrap;
}

.filter-row .el-input,
.filter-row .el-select,
.filter-row .el-date-editor {
  max-width: 320px;
}

.toolbar-tip,
.handled-text {
  color: #64748b;
  font-size: 13px;
}

.section-title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.dialog-summary {
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
  color: #334155;
  line-height: 1.8;
}

.report-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.report-card {
  padding: 20px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
}

.report-head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.report-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.distribution-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.distribution-item {
  display: grid;
  grid-template-columns: 60px 1fr 60px;
  gap: 14px;
  align-items: center;
}

@media (max-width: 1200px) {
  .summary-card,
  .toolbar-card {
    flex-direction: column;
  }

  .summary-metrics,
  .report-metrics {
    min-width: 0;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
  }
}

@media (max-width: 768px) {
  .summary-metrics,
  .report-metrics {
    grid-template-columns: 1fr;
  }

  .distribution-item {
    grid-template-columns: 1fr;
  }
}
</style>
