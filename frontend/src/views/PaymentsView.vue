<script setup lang="ts">
import { Money, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  fetchEnrollmentCourseOptions,
  fetchPaymentPage,
  payPayment,
  type PaymentPayPayload,
} from '../api/training'
import { useAuthStore } from '../stores/auth'
import type { CourseOptionRecord, PaymentRecordView } from '../types/api'

const authStore = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const selectedRecord = ref<PaymentRecordView | null>(null)
const courseOptions = ref<CourseOptionRecord[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  paymentStatus: '',
  courseId: undefined as number | undefined,
})

const pageData = reactive({
  list: [] as PaymentRecordView[],
  total: 0,
})

const form = reactive<PaymentPayPayload>({
  paidAmount: 0,
  paymentMethod: 'CASH',
})

const rules: FormRules<PaymentPayPayload> = {
  paidAmount: [{ required: true, message: '请输入实收金额', trigger: 'blur' }],
  paymentMethod: [{ required: true, message: '请选择收费方式', trigger: 'change' }],
}

const paidCount = computed(() =>
  pageData.list.filter((item) => item.paymentStatus === 'PAID' || item.paymentStatus === 'CORPORATE_PAID').length,
)
const isStudentView = computed(() => authStore.hasRole('STUDENT'))
const payableCount = computed(() => pageData.list.filter((item) => item.paymentStatus === 'UNPAID').length)
const pageTag = computed(() => (isStudentView.value ? '学员缴费视角' : '现场收费视角'))
const pageTitle = computed(() => (isStudentView.value ? '我的缴费记录' : '收费管理'))
const pageDescription = computed(() =>
  isStudentView.value
    ? '学员可查看自己的缴费状态，并对个人付费课程提交缴费；企业付费课程会显示为企业统一结算。'
    : '现场工作人员根据已确认报名记录完成收费登记，收费结果与金额直接写入数据库。',
)

async function loadCourseOptions() {
  const response = await fetchEnrollmentCourseOptions()
  courseOptions.value = response.data
}

async function loadData() {
  loading.value = true
  try {
    const response = await fetchPaymentPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      paymentStatus: query.paymentStatus || undefined,
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

function openPayDialog(row: PaymentRecordView) {
  selectedRecord.value = row
  form.paidAmount = Number(row.receivableAmount ?? 0)
  form.paymentMethod = row.paymentType === 'CORPORATE' ? 'CORPORATE' : 'CASH'
  dialogVisible.value = true
}

async function submitPay() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !selectedRecord.value) {
    return
  }

  submitting.value = true
  try {
    await payPayment(selectedRecord.value.id, {
      paidAmount: form.paidAmount,
      paymentMethod: form.paymentMethod,
    })
    ElMessage.success('收费已完成')
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

function handleDialogClosed() {
  formRef.value?.resetFields()
  selectedRecord.value = null
  form.paidAmount = 0
  form.paymentMethod = 'CASH'
}

function formatTime(value: string | null) {
  return value ? value.replace('T', ' ') : '-'
}

function formatMoney(value: number) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function paymentTypeText(value: string) {
  return value === 'CORPORATE' ? '企业付费' : '个人付费'
}

function paymentStatusText(value: string) {
  if (value === 'PAID') {
    return '已收费'
  }
  if (value === 'CORPORATE_PAID') {
    return '企业已登记'
  }
  return '待收费'
}

function paymentStatusTagType(value: string) {
  return value === 'UNPAID' ? 'warning' : 'success'
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
          <span>收费记录数</span>
          <strong>{{ pageData.total }}</strong>
        </div>
        <div class="metric-item">
          <span>{{ isStudentView ? '待缴费' : '已完成收费' }}</span>
          <strong>{{ isStudentView ? payableCount : paidCount }}</strong>
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
        <el-select v-model="query.paymentStatus" clearable placeholder="收费状态">
          <el-option label="待收费" value="UNPAID" />
          <el-option label="已收费" value="PAID" />
          <el-option label="企业已登记" value="CORPORATE_PAID" />
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
        <el-table-column prop="paymentType" label="付费类型" width="110">
          <template #default="{ row }">
            {{ paymentTypeText(row.paymentType) }}
          </template>
        </el-table-column>
        <el-table-column prop="receivableAmount" label="应收金额" width="120">
          <template #default="{ row }">
            {{ formatMoney(row.receivableAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="paidAmount" label="实收金额" width="120">
          <template #default="{ row }">
            {{ formatMoney(row.paidAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="paymentStatus" label="收费状态" width="120">
          <template #default="{ row }">
            <el-tag :type="paymentStatusTagType(row.paymentStatus)">
              {{ paymentStatusText(row.paymentStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="收费方式" width="110" />
        <el-table-column prop="paidAt" label="收费时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.paidAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.paymentStatus === 'UNPAID' && (!isStudentView || row.paymentType === 'PERSONAL')"
              link
              type="primary"
              :icon="Money"
              @click="openPayDialog(row)"
            >
              {{ isStudentView ? '去缴费' : '执行收费' }}
            </el-button>
            <span v-else class="handled-text">
              {{
                row.paymentStatus === 'UNPAID' && row.paymentType === 'CORPORATE'
                  ? '企业统一结算'
                  : isStudentView
                    ? '已完成缴费'
                    : '已完成收费'
              }}
            </span>
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
      :title="isStudentView ? '提交缴费' : '执行收费'"
      width="620px"
      @closed="handleDialogClosed"
    >
      <div class="confirm-summary" v-if="selectedRecord">
        <div>报名编号：{{ selectedRecord.enrollmentNo }}</div>
        <div>课程名称：{{ selectedRecord.courseName }}</div>
        <div>学员姓名：{{ selectedRecord.studentName }}</div>
        <div>应收金额：{{ formatMoney(selectedRecord.receivableAmount) }}</div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="实收金额" prop="paidAmount">
          <el-input-number v-model="form.paidAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收费方式" prop="paymentMethod">
          <el-select v-model="form.paymentMethod" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="转账" value="TRANSFER" />
            <el-option v-if="!isStudentView" label="企业登记" value="CORPORATE" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPay">
          {{ isStudentView ? '确认缴费' : '确认收费' }}
        </el-button>
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
  display: flex;
  gap: 16px;
  min-width: 300px;
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
