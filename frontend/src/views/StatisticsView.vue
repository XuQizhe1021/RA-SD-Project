<script setup lang="ts">
import { DataAnalysis, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  fetchCourseStatistics,
  fetchExecutorStatistics,
  fetchLecturerStatistics,
  fetchRevenueStatistics,
  fetchStudentStatistics,
} from '../api/training'
import { getErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import type {
  CourseStatisticsRecord,
  ExecutorStatisticsRecord,
  LecturerStatisticsRecord,
  RevenueStatisticsResponse,
  StudentStatisticsRecord,
} from '../types/api'

const authStore = useAuthStore()
const loading = ref(false)
const activeTab = ref('course')
const courseStats = ref<CourseStatisticsRecord[]>([])
const studentStats = ref<StudentStatisticsRecord[]>([])
const lecturerStats = ref<LecturerStatisticsRecord[]>([])
const executorStats = ref<ExecutorStatisticsRecord[]>([])
const revenueStats = ref<RevenueStatisticsResponse>({
  receivableAmountTotal: 0,
  paidAmountTotal: 0,
  specialPaymentCount: 0,
  cashAmountRatio: 0,
  transferAmountRatio: 0,
  details: [],
})

const query = reactive({
  keyword: '',
  dateRange: getDefaultDateRange() as [string, string],
})

const isManagerView = computed(() => authStore.hasRole('MANAGER'))
const pageTag = computed(() => (isManagerView.value ? '统计概览' : '执行统计'))
const pageDescription = computed(() =>
  isManagerView.value
    ? '围绕经营结果、课程执行和执行人负荷查看培训业务状态，支持经理进行过程监管与结果分析。'
    : '围绕课程推进、报名审核、现场转化和收入结果查看执行成效，帮助执行人识别重点课程和协同压力。',
)

const totalAttendance = computed(() =>
  courseStats.value.reduce((sum, item) => sum + Number(item.attendanceCount ?? 0), 0),
)

const totalEnrollment = computed(() =>
  courseStats.value.reduce((sum, item) => sum + Number(item.enrollmentCount ?? 0), 0),
)

const totalExecutorReviews = computed(() =>
  executorStats.value.reduce((sum, item) => sum + Number(item.enrollmentReviewedCount ?? 0), 0),
)

const totalCompletedCourses = computed(() =>
  executorStats.value.reduce((sum, item) => sum + Number(item.trainingCompletedCount ?? 0), 0),
)

const attendanceConversionRate = computed(() => {
  if (!totalEnrollment.value) {
    return 0
  }
  return Number(((totalAttendance.value / totalEnrollment.value) * 100).toFixed(1))
})

const overallRating = computed(() => {
  let totalScore = 0
  let ratingCount = 0
  courseStats.value.forEach((item) => {
    if (item.averageRating != null && item.attendanceCount > 0) {
      totalScore += Number(item.averageRating) * Math.max(item.attendanceCount, 1)
      ratingCount += Math.max(item.attendanceCount, 1)
    }
  })
  return ratingCount ? Number((totalScore / ratingCount).toFixed(1)) : 0
})

const courseRanking = computed(() =>
  [...courseStats.value]
    .sort((a, b) => Number(b.enrollmentCount ?? 0) - Number(a.enrollmentCount ?? 0))
    .slice(0, 5),
)

const executorRanking = computed(() =>
  [...executorStats.value]
    .sort((a, b) => {
      const reviewDiff = Number(b.enrollmentReviewedCount ?? 0) - Number(a.enrollmentReviewedCount ?? 0)
      if (reviewDiff !== 0) {
        return reviewDiff
      }
      return Number(b.courseCount ?? 0) - Number(a.courseCount ?? 0)
    })
    .slice(0, 5),
)

const revenueStructure = computed(() => {
  const cashAmount = revenueStats.value.details
    .filter((item) => item.paymentMethod === 'CASH')
    .reduce((sum, item) => sum + Number(item.paidAmount ?? 0), 0)
  const transferAmount = revenueStats.value.details
    .filter((item) => item.paymentMethod === 'TRANSFER')
    .reduce((sum, item) => sum + Number(item.paidAmount ?? 0), 0)
  const specialAmount = revenueStats.value.details
    .filter((item) => !['CASH', 'TRANSFER', '未收费'].includes(item.paymentMethod))
    .reduce((sum, item) => sum + Number(item.paidAmount ?? 0), 0)
  const total = cashAmount + transferAmount + specialAmount

  return [
    { label: '现金', amount: cashAmount, ratio: total ? Number(((cashAmount / total) * 100).toFixed(1)) : 0, color: '#2563eb' },
    { label: '转账', amount: transferAmount, ratio: total ? Number(((transferAmount / total) * 100).toFixed(1)) : 0, color: '#14b8a6' },
    { label: '特殊支付', amount: specialAmount, ratio: total ? Number(((specialAmount / total) * 100).toFixed(1)) : 0, color: '#f59e0b' },
  ]
})

function buildQueryParams() {
  return {
    keyword: query.keyword || undefined,
    startDate: query.dateRange?.[0],
    endDate: query.dateRange?.[1],
  }
}

async function loadData() {
  loading.value = true
  try {
    const params = buildQueryParams()
    const [courseResponse, studentResponse, lecturerResponse, executorResponse, revenueResponse] = await Promise.all([
      fetchCourseStatistics(params),
      fetchStudentStatistics(params),
      fetchLecturerStatistics(params),
      fetchExecutorStatistics(params),
      fetchRevenueStatistics(params),
    ])
    courseStats.value = courseResponse.data
    studentStats.value = studentResponse.data
    lecturerStats.value = lecturerResponse.data
    executorStats.value = executorResponse.data
    revenueStats.value = revenueResponse.data
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '统计报表加载失败'))
  } finally {
    loading.value = false
  }
}

function formatTime(value: string | null) {
  return value ? value.replace('T', ' ') : '—'
}

function formatRange(startTime: string, endTime: string) {
  return `${formatTime(startTime)} 至 ${formatTime(endTime)}`
}

function formatMoney(value: number | null | undefined) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function formatRating(value: number | null | undefined) {
  return value == null ? '—' : `${Number(value).toFixed(1)} 分`
}

function formatPercent(value: number | null | undefined) {
  return `${Number(value ?? 0).toFixed(1)}%`
}

function paymentMethodText(value: string) {
  if (value === 'CASH') {
    return '现金'
  }
  if (value === 'TRANSFER') {
    return '转账'
  }
  if (value === 'CORPORATE') {
    return '企业统付'
  }
  if (value === 'WAIVED') {
    return '免收'
  }
  if (value === 'AGENT') {
    return '代缴'
  }
  return value || '—'
}

function getBarWidth(value: number, maxValue: number) {
  if (!maxValue) {
    return '0%'
  }
  return `${Math.max((value / maxValue) * 100, 8)}%`
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

onMounted(() => {
  activeTab.value = isManagerView.value ? 'executor' : 'course'
  void loadData()
})
</script>

<template>
  <div class="module-page">
    <section class="page-card summary-card">
      <div>
        <div class="module-tag">{{ pageTag }}</div>
        <h2>统计报表</h2>
        <p>{{ pageDescription }}</p>
      </div>
      <div class="summary-metrics">
        <div class="metric-item">
          <span>课程数</span>
          <strong>{{ courseStats.length }}</strong>
        </div>
        <div class="metric-item">
          <span>报名总人次</span>
          <strong>{{ totalEnrollment }}</strong>
        </div>
        <div class="metric-item">
          <span>签到转化率</span>
          <strong>{{ formatPercent(attendanceConversionRate) }}</strong>
        </div>
        <div class="metric-item">
          <span>审核处理数</span>
          <strong>{{ totalExecutorReviews }}</strong>
        </div>
        <div class="metric-item">
          <span>实收总额</span>
          <strong>{{ formatMoney(revenueStats.paidAmountTotal) }}</strong>
        </div>
        <div class="metric-item">
          <span>评价均分</span>
          <strong>{{ formatRating(overallRating) }}</strong>
        </div>
      </div>
    </section>

    <section class="page-card toolbar-card">
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索课程 / 学员 / 讲师 / 执行人关键词"
          @keyup.enter="loadData"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
        <el-button type="primary" :icon="DataAnalysis" @click="loadData">刷新统计</el-button>
      </div>
      <div class="toolbar-tip">支持查看课程、学员、讲师、执行人和收入等多维度统计结果。</div>
    </section>

    <section class="overview-grid">
      <article class="page-card overview-card">
        <div class="section-title">热门课程排行</div>
        <div v-if="courseRanking.length" class="ranking-list">
          <div v-for="item in courseRanking" :key="item.courseId" class="ranking-row">
            <div class="ranking-head">
              <span class="ranking-name">{{ item.courseName }}</span>
              <span class="ranking-value">{{ item.enrollmentCount }} 人</span>
            </div>
            <div class="ranking-track">
              <div
                class="ranking-bar course-bar"
                :style="{ width: getBarWidth(Number(item.enrollmentCount ?? 0), Number(courseRanking[0]?.enrollmentCount ?? 0)) }"
              />
            </div>
            <div class="ranking-meta">
              <span>签到 {{ item.attendanceCount }} 人</span>
              <span>实收 {{ formatMoney(item.paidAmountTotal) }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="当前时间范围内暂无课程统计数据" />
      </article>

      <article class="page-card overview-card">
        <div class="section-title">收入结构</div>
        <div class="structure-list">
          <div v-for="item in revenueStructure" :key="item.label" class="structure-row">
            <div class="structure-head">
              <span>{{ item.label }}</span>
              <span>{{ formatMoney(item.amount) }} / {{ formatPercent(item.ratio) }}</span>
            </div>
            <div class="ranking-track">
              <div class="ranking-bar" :style="{ width: `${item.ratio}%`, background: item.color }" />
            </div>
          </div>
        </div>
        <div class="structure-footnote">
          已完成课程 {{ totalCompletedCourses }} 门，特殊支付笔数 {{ revenueStats.specialPaymentCount }} 笔。
        </div>
      </article>

      <article class="page-card overview-card">
        <div class="section-title">执行人工作情况</div>
        <div v-if="executorRanking.length" class="ranking-list">
          <div v-for="item in executorRanking" :key="item.executorUserId" class="ranking-row">
            <div class="ranking-head">
              <span class="ranking-name">{{ item.executorName }}</span>
              <span class="ranking-value">{{ item.enrollmentReviewedCount }} 单审核</span>
            </div>
            <div class="ranking-track">
              <div
                class="ranking-bar executor-bar"
                :style="{ width: getBarWidth(Number(item.enrollmentReviewedCount ?? 0), Number(executorRanking[0]?.enrollmentReviewedCount ?? 0)) }"
              />
            </div>
            <div class="ranking-meta">
              <span>负责课程 {{ item.courseCount }} 门</span>
              <span>完成课程 {{ item.trainingCompletedCount }} 门</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="当前时间范围内暂无执行人统计数据" />
      </article>
    </section>

    <section class="page-card table-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="执行人统计" name="executor">
          <el-table v-loading="loading" :data="executorStats" stripe>
            <el-table-column prop="executorName" label="执行人" min-width="140" />
            <el-table-column prop="courseCount" label="负责课程数" width="120" />
            <el-table-column prop="publishedCourseCount" label="已发布课程数" width="130" />
            <el-table-column prop="enrollmentReviewedCount" label="报名审核数" width="120" />
            <el-table-column prop="trainingCompletedCount" label="完成培训数" width="120" />
            <el-table-column prop="attendanceCount" label="签到总人次" width="120" />
            <el-table-column prop="paidAmountTotal" label="课程实收额" width="140">
              <template #default="{ row }">
                {{ formatMoney(row.paidAmountTotal) }}
              </template>
            </el-table-column>
          </el-table>
          <div class="footnote">执行人统计按课程负责人、报名审核人及课程实收结果聚合，便于查看各执行人的工作量与完成情况。</div>
        </el-tab-pane>

        <el-tab-pane label="按课程统计" name="course">
          <el-table v-loading="loading" :data="courseStats" stripe>
            <el-table-column prop="courseNo" label="课程编号" min-width="140" />
            <el-table-column prop="courseName" label="课程名称" min-width="220" />
            <el-table-column prop="lecturerName" label="授课讲师" min-width="120" />
            <el-table-column label="培训时间" min-width="220">
              <template #default="{ row }">
                {{ formatRange(row.startTime, row.endTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="quota" label="计划名额" width="100" />
            <el-table-column prop="enrollmentCount" label="报名人数" width="100" />
            <el-table-column prop="attendanceCount" label="签到人数" width="100" />
            <el-table-column prop="paidAmountTotal" label="实收金额" width="130">
              <template #default="{ row }">
                {{ formatMoney(row.paidAmountTotal) }}
              </template>
            </el-table-column>
            <el-table-column prop="averageRating" label="评价均分" width="110">
              <template #default="{ row }">
                {{ formatRating(row.averageRating) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="按学员统计" name="student">
          <el-table v-loading="loading" :data="studentStats" stripe>
            <el-table-column prop="studentNo" label="学员编号" min-width="140" />
            <el-table-column prop="studentName" label="学员姓名" min-width="120" />
            <el-table-column prop="companyName" label="所属公司" min-width="180" show-overflow-tooltip />
            <el-table-column prop="attendanceCount" label="参训次数" width="100" />
            <el-table-column prop="enrollmentCount" label="报名次数" width="100" />
            <el-table-column prop="paidAmountTotal" label="累计缴费" width="130">
              <template #default="{ row }">
                {{ formatMoney(row.paidAmountTotal) }}
              </template>
            </el-table-column>
            <el-table-column prop="averageRating" label="平均评分" width="110">
              <template #default="{ row }">
                {{ formatRating(row.averageRating) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="按讲师统计" name="lecturer">
          <el-table v-loading="loading" :data="lecturerStats" stripe>
            <el-table-column prop="lecturerNo" label="讲师编号" min-width="140" />
            <el-table-column prop="lecturerName" label="讲师姓名" min-width="120" />
            <el-table-column prop="courseCount" label="授课次数" width="100" />
            <el-table-column prop="attendanceCount" label="参训总人次" width="120" />
            <el-table-column prop="averageRating" label="评价均分" width="110">
              <template #default="{ row }">
                {{ formatRating(row.averageRating) }}
              </template>
            </el-table-column>
            <el-table-column prop="feeAmountTotal" label="课酬合计" width="130">
              <template #default="{ row }">
                {{ formatMoney(row.feeAmountTotal) }}
              </template>
            </el-table-column>
          </el-table>
          <div class="footnote">课酬合计按“授课次数 × 讲师课酬标准”计算，便于查看课程资源投入情况。</div>
        </el-tab-pane>

        <el-tab-pane label="收入汇总" name="revenue">
          <div class="revenue-grid">
            <div class="metric-box">
              <span>应收总额</span>
              <strong>{{ formatMoney(revenueStats.receivableAmountTotal) }}</strong>
            </div>
            <div class="metric-box">
              <span>实收总额</span>
              <strong>{{ formatMoney(revenueStats.paidAmountTotal) }}</strong>
            </div>
            <div class="metric-box">
              <span>特殊支付笔数</span>
              <strong>{{ revenueStats.specialPaymentCount }}</strong>
            </div>
            <div class="metric-box">
              <span>现金 / 转账占比</span>
              <strong>{{ Number(revenueStats.cashAmountRatio || 0).toFixed(1) }}% / {{ Number(revenueStats.transferAmountRatio || 0).toFixed(1) }}%</strong>
            </div>
          </div>

          <el-table v-loading="loading" :data="revenueStats.details" stripe style="margin-top: 18px">
            <el-table-column prop="courseName" label="课程名称" min-width="220" />
            <el-table-column prop="studentName" label="学员姓名" min-width="120" />
            <el-table-column prop="receivableAmount" label="应缴金额" width="120">
              <template #default="{ row }">
                {{ formatMoney(row.receivableAmount) }}
              </template>
            </el-table-column>
            <el-table-column prop="paidAmount" label="实收金额" width="120">
              <template #default="{ row }">
                {{ formatMoney(row.paidAmount) }}
              </template>
            </el-table-column>
            <el-table-column prop="paymentMethod" label="支付方式" width="110">
              <template #default="{ row }">
                {{ paymentMethodText(row.paymentMethod) }}
              </template>
            </el-table-column>
            <el-table-column prop="paidAt" label="收费时间" min-width="170">
              <template #default="{ row }">
                {{ formatTime(row.paidAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="handledByName" label="操作人" min-width="120" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>
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
.table-card,
.overview-card {
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
  background: rgba(59, 130, 246, 0.14);
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
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
  min-width: 760px;
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
  font-size: 24px;
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
.filter-row .el-date-editor {
  max-width: 320px;
}

.toolbar-tip,
.footnote,
.structure-footnote {
  color: #64748b;
  font-size: 13px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.section-title {
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.ranking-list,
.structure-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ranking-row,
.structure-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ranking-head,
.structure-head,
.ranking-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.ranking-name {
  color: #0f172a;
  font-weight: 600;
}

.ranking-value,
.ranking-meta,
.structure-head {
  color: #64748b;
  font-size: 13px;
}

.ranking-track {
  width: 100%;
  height: 10px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.ranking-bar {
  height: 100%;
  border-radius: inherit;
}

.course-bar {
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
}

.executor-bar {
  background: linear-gradient(90deg, #8b5cf6, #a78bfa);
}

.revenue-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 1440px) {
  .summary-card,
  .toolbar-card {
    flex-direction: column;
  }

  .summary-metrics {
    min-width: 0;
    width: 100%;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .summary-metrics,
  .revenue-grid {
    grid-template-columns: 1fr;
  }
}
</style>
