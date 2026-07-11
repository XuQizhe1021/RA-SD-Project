<script setup lang="ts">
import { Check, CloseBold, Plus } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createInternalAccount,
  fetchManagedAccounts,
  fetchPendingRegistrations,
  reviewRegistration,
  type InternalAccountCreatePayload,
  type RegistrationReviewPayload,
} from '../api/account'
import { getErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import type { ManagedAccountRecord, RegistrationReviewRecord } from '../types/api'

const authStore = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const createDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const createFormRef = ref<FormInstance>()
const reviewFormRef = ref<FormInstance>()
const selectedRegistration = ref<RegistrationReviewRecord | null>(null)
const pendingRegistrations = ref<RegistrationReviewRecord[]>([])
const managedAccounts = ref<ManagedAccountRecord[]>([])

const createForm = reactive<InternalAccountCreatePayload>({
  username: '',
  password: '',
  displayName: '',
  roleCode: 'EXECUTOR',
  phone: '',
  email: '',
})

const reviewForm = reactive<RegistrationReviewPayload>({
  approved: true,
  reviewComment: '',
})

const createRules: FormRules<InternalAccountCreatePayload> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6位', trigger: 'blur' },
  ],
  displayName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const reviewRules: FormRules<RegistrationReviewPayload> = {
  reviewComment: [
    {
      validator: (_rule, value: string, callback) => {
        if (!reviewForm.approved && !value.trim()) {
          callback(new Error('驳回时请填写审核说明'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const isAdmin = computed(() => authStore.hasRole('ADMIN'))
const pageTag = computed(() => (isAdmin.value ? '账号管理' : '注册审核'))
const pageTitle = computed(() => (isAdmin.value ? '账号管理' : '学员注册审核'))
const pageDescription = computed(() =>
  isAdmin.value
    ? '系统管理员可审核学员注册申请，并统一创建内部岗位账号。'
    : '请及时审核学员注册申请，确保报名与培训业务顺利开展。',
)
const activeAccountCount = computed(() => managedAccounts.value.filter((item) => item.accountStatus === 'ACTIVE').length)

const roleOptions = [
  { label: '系统管理员', value: 'ADMIN' },
  { label: '经理', value: 'MANAGER' },
  { label: '执行人', value: 'EXECUTOR' },
  { label: '现场工作人员', value: 'SITE_STAFF' },
]

async function loadData() {
  loading.value = true
  try {
    const [pendingResponse, managedResponse] = await Promise.all([
      fetchPendingRegistrations(),
      isAdmin.value ? fetchManagedAccounts() : Promise.resolve({ data: [] as ManagedAccountRecord[] }),
    ])
    pendingRegistrations.value = pendingResponse.data
    managedAccounts.value = managedResponse.data
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  createForm.username = ''
  createForm.password = ''
  createForm.displayName = ''
  createForm.roleCode = 'EXECUTOR'
  createForm.phone = ''
  createForm.email = ''
  createDialogVisible.value = true
}

function openReviewDialog(row: RegistrationReviewRecord, approved: boolean) {
  selectedRegistration.value = row
  reviewForm.approved = approved
  reviewForm.reviewComment = ''
  reviewDialogVisible.value = true
}

async function submitCreateForm() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    await createInternalAccount({
      username: createForm.username,
      password: createForm.password,
      displayName: createForm.displayName,
      roleCode: createForm.roleCode,
      phone: createForm.phone || undefined,
      email: createForm.email || undefined,
    })
    ElMessage.success('内部账号已创建')
    createDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '内部账号创建失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

async function submitReviewForm() {
  const valid = await reviewFormRef.value?.validate().catch(() => false)
  if (!valid || !selectedRegistration.value) {
    return
  }

  submitting.value = true
  try {
    await reviewRegistration(selectedRegistration.value.userId, {
      approved: reviewForm.approved,
      reviewComment: (reviewForm.reviewComment ?? '').trim() || undefined,
    })
    ElMessage.success(reviewForm.approved ? '注册申请已通过' : '注册申请已驳回')
    reviewDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '审核处理失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

function handleCreateDialogClosed() {
  createFormRef.value?.resetFields()
}

function handleReviewDialogClosed() {
  reviewFormRef.value?.resetFields()
  selectedRegistration.value = null
  reviewForm.approved = true
  reviewForm.reviewComment = ''
}

function formatTime(value: string | null) {
  return value ? value.replace('T', ' ') : '-'
}

function statusTagType(value: string) {
  if (value === 'ACTIVE') {
    return 'success'
  }
  if (value === 'PENDING') {
    return 'warning'
  }
  if (value === 'REJECTED') {
    return 'danger'
  }
  return 'info'
}

function statusText(value: string) {
  if (value === 'ACTIVE') {
    return '已启用'
  }
  if (value === 'PENDING') {
    return '待审核'
  }
  if (value === 'REJECTED') {
    return '已驳回'
  }
  return '已停用'
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
          <span>待审核申请</span>
          <strong>{{ pendingRegistrations.length }}</strong>
        </div>
        <div class="metric-item" v-if="isAdmin">
          <span>已启用账号</span>
          <strong>{{ activeAccountCount }}</strong>
        </div>
      </div>
    </section>

    <section class="page-card table-card">
      <div class="section-head">
        <div class="section-title">学员注册申请</div>
        <span class="section-tip">审核通过后，学员即可登录并办理报名、缴费与评价业务。</span>
      </div>

      <el-table v-loading="loading" :data="pendingRegistrations" stripe>
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="displayName" label="姓名" min-width="120" />
        <el-table-column prop="companyName" label="所属企业" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="jobTitle" label="岗位" min-width="120" />
        <el-table-column prop="educationLevel" label="学历" width="100" />
        <el-table-column prop="techLevel" label="技术级别" width="110" />
        <el-table-column prop="createdAt" label="申请时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button link type="success" :icon="Check" @click="openReviewDialog(row, true)">通过</el-button>
              <el-button link type="danger" :icon="CloseBold" @click="openReviewDialog(row, false)">驳回</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="isAdmin" class="page-card table-card">
      <div class="section-head">
        <div>
          <div class="section-title">内部账号</div>
          <span class="section-tip">管理与现场岗位账号由系统管理员统一创建与维护。</span>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增内部账号</el-button>
      </div>

      <el-table v-loading="loading" :data="managedAccounts" stripe>
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="displayName" label="姓名" min-width="120" />
        <el-table-column prop="roleName" label="角色" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="accountStatus" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.accountStatus)">
              {{ statusText(row.accountStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最近登录" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.lastLoginAt) }}
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="reviewDialogVisible" title="处理注册申请" width="520px" @closed="handleReviewDialogClosed">
      <div v-if="selectedRegistration" class="confirm-summary">
        <div>用户名：{{ selectedRegistration.username }}</div>
        <div>姓名：{{ selectedRegistration.displayName }}</div>
        <div>所属企业：{{ selectedRegistration.companyName }}</div>
      </div>

      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="72px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewForm.approved">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核说明" prop="reviewComment">
          <el-input
            v-model="reviewForm.reviewComment"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            :placeholder="reviewForm.approved ? '可填写审核说明' : '请填写驳回原因'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReviewForm">保存结果</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createDialogVisible" title="新增内部账号" width="620px" @closed="handleCreateDialogClosed">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="88px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password placeholder="请输入登录密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="displayName">
          <el-input v-model="createForm.displayName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="createForm.roleCode" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="createForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreateForm">创建账号</el-button>
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

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.section-tip {
  color: #64748b;
  font-size: 13px;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 8px;
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
  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary-metrics {
    min-width: 0;
    width: 100%;
  }
}
</style>
