<script setup lang="ts">
import { EditPen, Plus, Search, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import {
  createLecturer,
  disableLecturer,
  fetchLecturerPage,
  updateLecturer,
  type LecturerPayload,
} from '../api/training'
import type { LecturerRecord } from '../types/api'

const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
})

const pageData = reactive({
  list: [] as LecturerRecord[],
  total: 0,
})

const form = reactive<LecturerPayload>({
  fullName: '',
  title: '',
  specialty: '',
  phone: '',
  email: '',
  feeStandard: 0,
  profileText: '',
})

const rules: FormRules<LecturerPayload> = {
  fullName: [{ required: true, message: '请输入讲师姓名', trigger: 'blur' }],
  title: [{ max: 100, message: '职称长度不能超过100', trigger: 'blur' }],
  specialty: [{ max: 200, message: '专长方向长度不能超过200', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const response = await fetchLecturerPage({
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

function resetForm() {
  editingId.value = null
  form.fullName = ''
  form.title = ''
  form.specialty = ''
  form.phone = ''
  form.email = ''
  form.feeStandard = 0
  form.profileText = ''
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: LecturerRecord) {
  editingId.value = row.id
  form.fullName = row.fullName
  form.title = row.title ?? ''
  form.specialty = row.specialty ?? ''
  form.phone = row.phone ?? ''
  form.email = row.email ?? ''
  form.feeStandard = Number(row.feeStandard ?? 0)
  form.profileText = row.profileText ?? ''
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    if (editingId.value) {
      await updateLecturer(editingId.value, { ...form })
      ElMessage.success('讲师信息已更新')
    } else {
      await createLecturer({ ...form })
      ElMessage.success('讲师已新增')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDisable(row: LecturerRecord) {
  await ElMessageBox.confirm(`确认停用讲师“${row.fullName}”吗？`, '停用讲师', {
    type: 'warning',
  })
  await disableLecturer(row.id)
  ElMessage.success('讲师已停用')
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

function formatMoney(value: number) {
  return `¥${Number(value ?? 0).toFixed(2)}`
}

function formatTime(value: string) {
  return value ? value.replace('T', ' ') : '-'
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <div class="module-page">
    <section class="page-card summary-card">
      <div>
        <div class="module-tag">Day9 站会后优先完成</div>
        <h2>讲师管理</h2>
        <p>维护讲师档案、专长方向和费用标准，为课程创建时的讲师关联提供基础数据。</p>
      </div>
      <div class="summary-metrics">
        <div class="metric-item">
          <span>总讲师数</span>
          <strong>{{ pageData.total }}</strong>
        </div>
        <div class="metric-item">
          <span>启用中</span>
          <strong>{{ pageData.list.filter((item) => item.status === 'ACTIVE').length }}</strong>
        </div>
      </div>
    </section>

    <section class="page-card toolbar-card">
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索讲师编号 / 姓名 / 专长"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="query.status" clearable placeholder="讲师状态">
          <el-option label="启用中" value="ACTIVE" />
          <el-option label="已停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增讲师</el-button>
    </section>

    <section class="page-card table-card">
      <el-table v-loading="loading" :data="pageData.list" stripe>
        <el-table-column prop="lecturerNo" label="讲师编号" min-width="150" />
        <el-table-column prop="fullName" label="讲师姓名" min-width="120" />
        <el-table-column prop="title" label="职称" min-width="140" />
        <el-table-column prop="specialty" label="专长方向" min-width="220" show-overflow-tooltip />
        <el-table-column prop="feeStandard" label="费用标准" min-width="120">
          <template #default="{ row }">
            {{ formatMoney(row.feeStandard) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '启用中' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最近更新" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button link type="primary" :icon="EditPen" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                v-if="row.status === 'ACTIVE'"
                link
                type="danger"
                :icon="SwitchButton"
                @click="handleDisable(row)"
              >
                停用
              </el-button>
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
      :title="editingId ? '编辑讲师' : '新增讲师'"
      width="720px"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="讲师姓名" prop="fullName">
              <el-input v-model="form.fullName" placeholder="请输入讲师姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称" prop="title">
              <el-input v-model="form.title" placeholder="如：高级架构师" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱">
              <el-input v-model="form.email" placeholder="请输入联系邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="费用标准">
              <el-input-number v-model="form.feeStandard" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专长方向" prop="specialty">
              <el-input v-model="form.specialty" placeholder="如：微服务、项目管理" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="讲师简介">
          <el-input v-model="form.profileText" type="textarea" :rows="4" placeholder="填写讲师背景与培训经验" />
        </el-form-item>
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
  background: rgba(34, 197, 94, 0.12);
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
}

.filter-row .el-input,
.filter-row .el-select {
  max-width: 280px;
}

.action-row {
  display: flex;
  gap: 8px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 1080px) {
  .summary-card,
  .toolbar-card,
  .filter-row {
    flex-direction: column;
  }

  .summary-metrics {
    min-width: 0;
  }
}
</style>
