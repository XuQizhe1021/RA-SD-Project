<script setup lang="ts">
import { Box, Check, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  checkInAttendance,
  fetchAttendancePage,
  fetchEnrollmentCourseOptions,
  updateAttendanceMaterials,
  type AttendanceCheckInPayload,
  type AttendanceMaterialPayload,
} from '../api/training'
import { getErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import type { AttendanceRecordView, CourseOptionRecord } from '../types/api'

const authStore = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const materialDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const materialFormRef = ref<FormInstance>()
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
  materialStatus: 'PENDING',
  materialRemark: '',
})

const materialForm = reactive<AttendanceMaterialPayload>({
  materialStatus: 'PENDING',
  materialRemark: '',
})

const rules: FormRules<AttendanceCheckInPayload> = {
  remark: [{ max: 255, message: '签到备注长度不能超过255', trigger: 'blur' }],
  materialRemark: [{ max: 255, message: '资料备注长度不能超过255', trigger: 'blur' }],
}

const materialRules: FormRules<AttendanceMaterialPayload> = {
  materialStatus: [{ required: true, message: '请选择资料发放状态', trigger: 'change' }],
  materialRemark: [{ max: 255, message: '资料备注长度不能超过255', trigger: 'blur' }],
}

const checkedInCount = computed(() => pageData.list.filter((item) => item.attendanceStatus === 'CHECKED_IN').length)
const uncheckedCount = computed(() => pageData.list.filter((item) => item.attendanceStatus === 'NOT_CHECKED_IN').length)
const pageTag = computed(() => (authStore.hasRole('SITE_STAFF') ? '签到办理' : '签到总览'))
const pageTitle = computed(() => (authStore.hasRole('SITE_STAFF') ? '签到管理' : '签到记录'))
const pageDescription = computed(() =>
  authStore.hasRole('SITE_STAFF')
    ? '培训当天由现场工作人员根据已确认报名名单执行签到，签到结果、操作人和备注直接写入数据库。'
    : '当前角色仅可查看签到说明，不具备培训现场签到权限。',
)

const roleBoundaries = [
  '经理：不进入签到模块，通过统计报表查看培训到场情况。',
  '执行人：不执行签到，负责在培训前完成课程、通知、报名审核等准备工作。',
  '现场工作人员：查看已确认报名名单、执行签到、记录现场备注。',
  '学员：不进入签到管理页面，只在现场由工作人员核验后签到。',
]

const businessRules = [
  '签到记录来源于报名审核通过后自动生成的 attendance_record 数据。',
  '仅状态为 CONFIRMED 的报名允许签到，未在名单内的学员无法签到。',
  '同一报名记录只允许签到一次，重复签到会被后端拒绝。',
]

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
  form.materialStatus = row.materialStatus ?? 'PENDING'
  form.materialRemark = row.materialRemark ?? ''
  dialogVisible.value = true
}

function openMaterialDialog(row: AttendanceRecordView) {
  selectedRecord.value = row
  materialForm.materialStatus = row.materialStatus ?? 'PENDING'
  materialForm.materialRemark = row.materialRemark ?? ''
  materialDialogVisible.value = true
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
      materialStatus: form.materialStatus,
      materialRemark: form.materialRemark?.trim() || '',
    })
    ElMessage.success('签到已完成')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '签到失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

function handleDialogClosed() {
  formRef.value?.resetFields()
  selectedRecord.value = null
  form.remark = ''
  form.materialStatus = 'PENDING'
  form.materialRemark = ''
}

async function submitMaterialForm() {
  const valid = await materialFormRef.value?.validate().catch(() => false)
  if (!valid || !selectedRecord.value) {
    return
  }

  submitting.value = true
  try {
    await updateAttendanceMaterials(selectedRecord.value.id, {
      materialStatus: materialForm.materialStatus,
      materialRemark: materialForm.materialRemark?.trim() || '',
    })
    ElMessage.success('资料发放记录已更新')
    materialDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '资料发放记录更新失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

function handleMaterialDialogClosed() {
  materialFormRef.value?.resetFields()
  selectedRecord.value = null
  materialForm.materialStatus = 'PENDING'
  materialForm.materialRemark = ''
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

function materialStatusText(value: string) {
  return value === 'ISSUED' ? '已发放' : '待发放'
}

function materialStatusTagType(value: string) {
  return value === 'ISSUED' ? 'success' : 'warning'
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
          <span>签到记录数</span>
          <strong>{{ pageData.total }}</strong>
        </div>
        <div class="metric-item">
          <span>已签到</span>
          <strong>{{ checkedInCount }}</strong>
        </div>
        <div class="metric-item">
          <span>待签到</span>
          <strong>{{ uncheckedCount }}</strong>
        </div>
      </div>
    </section>

    <section class="page-card meta-card">
      <div class="meta-grid">
        <article class="meta-panel">
          <div class="meta-title">角色边界</div>
          <ul class="meta-list">
            <li v-for="item in roleBoundaries" :key="item">{{ item }}</li>
          </ul>
        </article>
        <article class="meta-panel">
          <div class="meta-title">业务规则</div>
          <ul class="meta-list">
            <li v-for="item in businessRules" :key="item">{{ item }}</li>
          </ul>
        </article>
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
        <el-table-column prop="materialStatus" label="资料发放" width="110">
          <template #default="{ row }">
            <el-tag :type="materialStatusTagType(row.materialStatus)">
              {{ materialStatusText(row.materialStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="materialRemark" label="资料备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
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
              <el-button link type="primary" :icon="Box" @click="openMaterialDialog(row)">资料记录</el-button>
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
        <el-form-item label="资料状态" prop="materialStatus">
          <el-radio-group v-model="form.materialStatus">
            <el-radio value="PENDING">待发放</el-radio>
            <el-radio value="ISSUED">已发放</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="资料备注" prop="materialRemark">
          <el-input
            v-model="form.materialRemark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="可填写资料包、讲义领取情况等说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCheckIn">确认签到</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="materialDialogVisible"
      title="维护资料发放记录"
      width="600px"
      @closed="handleMaterialDialogClosed"
    >
      <div class="confirm-summary" v-if="selectedRecord">
        <div>报名编号：{{ selectedRecord.enrollmentNo }}</div>
        <div>课程名称：{{ selectedRecord.courseName }}</div>
        <div>学员姓名：{{ selectedRecord.studentName }}</div>
      </div>
      <el-form ref="materialFormRef" :model="materialForm" :rules="materialRules" label-width="88px">
        <el-form-item label="发放状态" prop="materialStatus">
          <el-radio-group v-model="materialForm.materialStatus">
            <el-radio value="PENDING">待发放</el-radio>
            <el-radio value="ISSUED">已发放</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="资料备注" prop="materialRemark">
          <el-input
            v-model="materialForm.materialRemark"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="请填写资料包、教材或签领情况"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="materialDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMaterialForm">保存记录</el-button>
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

.meta-card {
  padding: 22px 24px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.meta-panel {
  padding: 18px;
  border-radius: 18px;
  background: #f8fafc;
}

.meta-title {
  margin-bottom: 12px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.meta-list {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
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
  align-items: center;
  gap: 8px;
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

  .meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
