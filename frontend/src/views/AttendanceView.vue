<script setup lang="ts">
import { Check, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  checkInAttendance,
  fetchAttendancePage,
  fetchEnrollmentCourseOptions,
  type AttendanceCheckInPayload,
} from '../api/training'
import type { AttendanceRecordView, CourseOptionRecord } from '../types/api'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const selectedRecord = ref<AttendanceRecordView | null>(null)
const courseOptions = ref<CourseOptionRecord[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  courseId: undefined as number | undefined,
})

const pageData = reactive({
  list: [] as AttendanceRecordView[],
  total: 0,
})

const form = reactive<AttendanceCheckInPayload>({
  remark: '',
})

const rules: FormRules<AttendanceCheckInPayload> = {
  remark: [{ max: 255, message: '签到备注长度不能超过255', trigger: 'blur' }],
}

const checkedInCount = computed(() => pageData.list.filter((item) => item.attendanceStatus === 'CHECKED_IN').length)

async function loadCourseOptions() {
  const response = await fetchEnrollmentCourseOptions()
  courseOptions.value = response.data
}

async function loadData() {
  loading.value = true
  try {
    const response = await fetchAttendancePage({
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

function openCheckInDialog(row: AttendanceRecordView) {
  selectedRecord.value = row
  form.remark = row.remark ?? ''
  dialogVisible.value = true
}

async function submitCheckIn() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !selectedRecord.value) {
    return
  }

  submitting.value = true
  try {
    await checkInAttendance(selectedRecord.value.id, {
      remark: form.remark?.trim() || '',
    })
    ElMessage.success('签到已完成')
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

function handleDialogClosed() {
  formRef.value?.resetFields()
  selectedRecord.value = null
  form.remark = ''
}

function formatTime(value: string | null) {
  return value ? value.replace('T', ' ') : '-'
}

function statusTagType(value: string) {
  return value === 'CHECKED_IN' ? 'success' : 'warning'
}

function statusText(value: string) {
  return value === 'CHECKED_IN' ? '已签到' : '未签到'
}

onMounted(async () => {
  await Promise.all([loadCourseOptions(), loadData()])
})
</script>

<template>
  <div class="module-page">
    <section class="page-card summary-card">
      <div>
        <div class="module-tag">Day11 上午交付</div>
        <h2>签到管理</h2>
        <p>基于已确认报名生成的签到记录，支持按课程检索并执行现场签到，签到结果直接写入数据库。</p>
      </div>
      <div class="summary-metrics">
        <div class="metric-item">
          <span>签到记录数</span>
          <strong>{{ pageData.total }}</strong>
        </div>
        <div class="metric-item">
          <span>已签到</span>
          <strong>{{ checkedInCount }}</strong>
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
        <el-select v-model="query.status" clearable placeholder="签到状态">
          <el-option label="未签到" value="NOT_CHECKED_IN" />
          <el-option label="已签到" value="CHECKED_IN" />
        </el-select>
        <el-select v-model="query.courseId" clearable placeholder="课程筛选">
          <el-option
            v-for="course in courseOptions"
            :key="course.id"
            :label="`${course.courseName} / ${course.courseNo}`"
            :value="course.id"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </section>

    <section class="page-card table-card">
      <el-table v-loading="loading" :data="pageData.list" stripe>
        <el-table-column prop="enrollmentNo" label="报名编号" min-width="150" />
        <el-table-column prop="courseName" label="课程名称" min-width="220" />
        <el-table-column prop="studentName" label="学员姓名" min-width="120" />
        <el-table-column prop="companyName" label="所属公司" min-width="180" show-overflow-tooltip />
        <el-table-column prop="courseStartTime" label="开课时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.courseStartTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="attendanceStatus" label="签到状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.attendanceStatus)">
              {{ statusText(row.attendanceStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkedInAt" label="签到时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.checkedInAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.attendanceStatus === 'NOT_CHECKED_IN'"
              link
              type="success"
              :icon="Check"
              @click="openCheckInDialog(row)"
            >
              执行签到
            </el-button>
            <span v-else class="handled-text">已完成签到</span>
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
      title="执行签到"
      width="600px"
      @closed="handleDialogClosed"
    >
      <div class="confirm-summary" v-if="selectedRecord">
        <div>报名编号：{{ selectedRecord.enrollmentNo }}</div>
        <div>课程名称：{{ selectedRecord.courseName }}</div>
        <div>学员姓名：{{ selectedRecord.studentName }}</div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="签到备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="可填写座位、资料发放等备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCheckIn">确认签到</el-button>
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
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
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
