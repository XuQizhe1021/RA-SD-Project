<script setup lang="ts">
import { Check, Close, Plus, Promotion, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  approveApplication,
  createApplication,
  fetchApplicationPage,
  type ApplicationApprovePayload,
  type ApplicationPayload,
} from '../api/training'
import { getErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import type { TrainingApplicationRecord } from '../types/api'

const authStore = useAuthStore()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const createDialogVisible = ref(false)
const approveDialogVisible = ref(false)
const createFormRef = ref<FormInstance>()
const approveFormRef = ref<FormInstance>()
const selectedRecord = ref<TrainingApplicationRecord | null>(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
})

const pageData = reactive({
  list: [] as TrainingApplicationRecord[],
  total: 0,
})

const createForm = reactive<ApplicationPayload>({
  companyName: '',
  topic: '',
  expectedStartDate: '',
  expectedEndDate: '',
  attendeeCount: 30,
  budgetAmount: 0,
  requirementDesc: '',
})

const approveForm = reactive<ApplicationApprovePayload>({
  approved: true,
  approvalComment: '',
})

const createRules: FormRules<ApplicationPayload> = {
  companyName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  topic: [{ required: true, message: '请输入培训主题', trigger: 'blur' }],
  attendeeCount: [{ required: true, message: '请输入预计参训人数', trigger: 'blur' }],
}

const pendingCount = computed(() => pageData.list.filter((item) => item.status === 'PENDING').length)
const approvedCount = computed(() => pageData.list.filter((item) => item.status === 'APPROVED' || item.status === 'COURSE_CREATED').length)
const isManagerView = computed(() => authStore.hasRole('MANAGER'))
const pageTag = computed(() => (isManagerView.value ? '申请审批' : '申请承接'))
const pageTitle = '培训申请'
const pageDescription = computed(() =>
  isManagerView.value
    ? '经理在此录入培训需求、审批预算与主题，并将审批通过的申请继续流转到建课执行。'
    : '执行人可查看已审批申请，并直接承接到课程管理页面继续建课。'
)

async function loadData() {
  loading.value = true
  try {
    const response = await fetchApplicationPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
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
  createForm.companyName = ''
  createForm.topic = ''
  createForm.expectedStartDate = ''
  createForm.expectedEndDate = ''
  createForm.attendeeCount = 30
  createForm.budgetAmount = 0
  createForm.requirementDesc = ''
}

function openCreateDialog() {
  if (!isManagerView.value) {
    return
  }
  resetCreateForm()
  createDialogVisible.value = true
}

function openApproveDialog(row: TrainingApplicationRecord, approved: boolean) {
  if (!isManagerView.value) {
    return
  }
  selectedRecord.value = row
  approveForm.approved = approved
  approveForm.approvalComment = approved ? '申请已通过，允许执行人建课。' : ''
  approveDialogVisible.value = true
}

async function submitCreateForm() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    await createApplication({
      ...createForm,
      expectedStartDate: createForm.expectedStartDate || undefined,
      expectedEndDate: createForm.expectedEndDate || undefined,
      budgetAmount: createForm.budgetAmount ?? 0,
      requirementDesc: createForm.requirementDesc?.trim() || undefined,
    })
    ElMessage.success('培训申请已提交')
    createDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '培训申请提交失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

async function submitApproveForm() {
  if (!selectedRecord.value) {
    return
  }
  if (!approveForm.approved && !approveForm.approvalComment?.trim()) {
    ElMessage.warning('驳回申请时请填写审批意见')
    return
  }

  submitting.value = true
  try {
    await approveApplication(selectedRecord.value.id, {
      approved: approveForm.approved,
      approvalComment: approveForm.approvalComment?.trim() || '',
    })
    ElMessage.success(approveForm.approved ? '培训申请已审批通过' : '培训申请已驳回')
    approveDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '申请审批失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

function goCreateCourse(row: TrainingApplicationRecord) {
  void router.push({
    path: '/courses',
    query: { applicationId: String(row.id) },
  })
}

function handleCreateClosed() {
  createFormRef.value?.resetFields()
  resetCreateForm()
}

function handleApproveClosed() {
  approveFormRef.value?.resetFields()
  approveForm.approved = true
  approveForm.approvalComment = ''
  selectedRecord.value = null
}

function formatDate(value: string | null) {
  return value || '-'
}

function formatMoney(value: number | null) {
  return value == null ? '-' : `¥${Number(value).toFixed(2)}`
}

function statusText(value: string) {
  if (value === 'APPROVED') {
    return '已审批'
  }
  if (value === 'COURSE_CREATED') {
    return '已建课'
  }
  if (value === 'REJECTED') {
    return '已驳回'
  }
  return '待审批'
}

function statusTagType(value: string) {
  if (value === 'APPROVED' || value === 'COURSE_CREATED') {
    return 'success'
  }
  if (value === 'REJECTED') {
    return 'danger'
  }
  return 'warning'
}

onMounted(() => {
  void loadData()
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
          <span>申请总数</span>
          <strong>{{ pageData.total }}</strong>
        </div>
        <div class="metric-item">
          <span>待审批</span>
          <strong>{{ pendingCount }}</strong>
        </div>
        <div class="metric-item">
          <span>已审批/已建课</span>
          <strong>{{ approvedCount }}</strong>
        </div>
      </div>
    </section>

    <section class="page-card toolbar-card">
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索申请编号 / 企业 / 培训主题"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="query.status" clearable placeholder="申请状态">
          <el-option label="待审批" value="PENDING" />
          <el-option label="已审批" value="APPROVED" />
          <el-option label="已建课" value="COURSE_CREATED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
      <el-button v-if="isManagerView" type="primary" :icon="Plus" @click="openCreateDialog">新建申请</el-button>
    </section>

    <section class="page-card table-card">
      <el-table v-loading="loading" :data="pageData.list" stripe>
        <el-table-column prop="applicationNo" label="申请编号" min-width="160" />
        <el-table-column prop="companyName" label="企业名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="topic" label="培训主题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="attendeeCount" label="预计人数" width="100" />
        <el-table-column prop="budgetAmount" label="预算金额" width="130">
          <template #default="{ row }">
            {{ formatMoney(row.budgetAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="计划时间" min-width="220">
          <template #default="{ row }">
            {{ formatDate(row.expectedStartDate) }} 至 {{ formatDate(row.expectedEndDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="申请状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approvalComment" label="审批意见" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button
                v-if="isManagerView && row.status === 'PENDING'"
                link
                type="success"
                :icon="Check"
                @click="openApproveDialog(row, true)"
              >
                通过
              </el-button>
              <el-button
                v-if="isManagerView && row.status === 'PENDING'"
                link
                type="danger"
                :icon="Close"
                @click="openApproveDialog(row, false)"
              >
                驳回
              </el-button>
              <el-button
                v-if="!isManagerView && row.status === 'APPROVED'"
                link
                type="primary"
                :icon="Promotion"
                @click="goCreateCourse(row)"
              >
                去建课
              </el-button>
              <span v-if="isManagerView && row.status === 'APPROVED'" class="handled-text">已审批，待执行人建课</span>
              <span v-if="row.status === 'COURSE_CREATED'" class="handled-text">已完成建课流转</span>
              <span v-if="row.status === 'REJECTED'" class="handled-text">已结束审批</span>
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

    <el-dialog v-model="createDialogVisible" title="新建培训申请" width="760px" @closed="handleCreateClosed">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="企业名称" prop="companyName">
              <el-input v-model="createForm.companyName" placeholder="请输入客户企业名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计人数" prop="attendeeCount">
              <el-input-number v-model="createForm.attendeeCount" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="培训主题" prop="topic">
          <el-input v-model="createForm.topic" placeholder="请输入培训主题" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始日期">
              <el-date-picker
                v-model="createForm.expectedStartDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择预计开始日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期">
              <el-date-picker
                v-model="createForm.expectedEndDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择预计结束日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="预算金额">
          <el-input-number v-model="createForm.budgetAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="需求说明">
          <el-input
            v-model="createForm.requirementDesc"
            type="textarea"
            :rows="4"
            maxlength="4000"
            show-word-limit
            placeholder="请描述培训目标、关注重点与交付要求"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreateForm">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="approveDialogVisible"
      :title="approveForm.approved ? '审批通过培训申请' : '驳回培训申请'"
      width="620px"
      @closed="handleApproveClosed"
    >
      <div class="confirm-summary" v-if="selectedRecord">
        <div>申请编号：{{ selectedRecord.applicationNo }}</div>
        <div>企业名称：{{ selectedRecord.companyName }}</div>
        <div>培训主题：{{ selectedRecord.topic }}</div>
      </div>
      <el-form ref="approveFormRef" :model="approveForm" label-width="96px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="approveForm.approved">
            <el-radio :value="true">审批通过</el-radio>
            <el-radio :value="false">驳回申请</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input
            v-model="approveForm.approvalComment"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            :placeholder="approveForm.approved ? '可填写建课建议、执行重点等说明' : '请填写驳回原因'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApproveForm">确认提交</el-button>
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
}
</style>
