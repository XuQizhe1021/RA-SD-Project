<script setup lang="ts">
import { DataAnalysis, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  fetchCourseStatistics,
  fetchLecturerStatistics,
  fetchRevenueStatistics,
  fetchStudentStatistics,
} from '../api/training'
import { getErrorMessage } from '../api/http'
import type {
  CourseStatisticsRecord,
  LecturerStatisticsRecord,
  RevenueStatisticsResponse,
  StudentStatisticsRecord,
} from '../types/api'

const loading = ref(false)
const activeTab = ref('course')
const courseStats = ref<CourseStatisticsRecord[]>([])
const studentStats = ref<StudentStatisticsRecord[]>([])
const lecturerStats = ref<LecturerStatisticsRecord[]>([])
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

const totalAttendance = computed(() =>
  courseStats.value.reduce((sum, item) => sum + Number(item.attendanceCount ?? 0), 0),
)

const totalEnrollment = computed(() =>
  courseStats.value.reduce((sum, item) => sum + Number(item.enrollmentCount ?? 0), 0),
)

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
    const [courseResponse, studentResponse, lecturerResponse, revenueResponse] = await Promise.all([
      fetchCourseStatistics(params),
      fetchStudentStatistics(params),
      fetchLecturerStatistics(params),
      fetchRevenueStatistics(params),
    ])
    courseStats.value = courseResponse.data
    studentStats.value = studentResponse.data
    lecturerStats.value = lecturerResponse.data
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
  return value || '—'
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
  void loadData()
})
</script>

<template>
  <div class="module-page">
    <section class="page-card summary-card">
      <div>
        <div class="module-tag">经理 / 执行人视角</div>
        <h2>统计报表</h2>
        <p>按课程、学员、讲师和收入四个维度查看业务执行结果，支持 Day12 主流程演示与 Day13 评审汇报。</p>
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
          <span>签到总人次</span>
          <strong>{{ totalAttendance }}</strong>
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
          placeholder="搜索课程 / 学员 / 讲师关键词"
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
      <div class="toolbar-tip">收入汇总中的“特殊支付笔数”当前按企业统付 / 免收口径统计。</div>
    </section>

    <section class="page-card table-card">
      <el-tabs v-model="activeTab">
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
          <div class="footnote">课酬合计按“授课次数 × 讲师课酬标准”计算，用于本次增量1演示口径。</div>
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
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  min-width: 640px;
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
.footnote {
  color: #64748b;
  font-size: 13px;
}

.revenue-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 1280px) {
  .summary-card,
  .toolbar-card {
    flex-direction: column;
  }

  .summary-metrics,
  .revenue-grid {
    min-width: 0;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
  }
}

@media (max-width: 768px) {
  .summary-metrics,
  .revenue-grid {
    grid-template-columns: 1fr;
  }
}
</style>
