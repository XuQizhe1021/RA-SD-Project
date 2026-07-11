<script setup lang="ts">
import { EditPen, Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  createStudent,
  fetchStudentPage,
  updateStudent,
  type StudentPayload,
} from '../api/training'
import { getErrorMessage } from '../api/http'
import type { StudentProfileRecord } from '../types/api'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
})

const pageData = reactive({
  list: [] as StudentProfileRecord[],
  total: 0,
})

const form = reactive<StudentPayload>({
  fullName: '',
  gender: '',
  companyName: '',
  jobTitle: '',
  educationLevel: '',
  techLevel: '',
  phone: '',
  email: '',
})

const rules: FormRules<StudentPayload> = {
  fullName: [{ required: true, message: '请输入学员姓名', trigger: 'blur' }],
  companyName: [{ required: true, message: '请输入所属企业', trigger: 'blur' }],
}

const companyCount = computed(() => new Set(pageData.list.map((item) => item.companyName).filter(Boolean)).size)
const pageTag = '学员档案'
const pageTitle = '学员管理'
const pageDescription = '执行人在此维护学员档案、企业归属与联系方式，为报名、签到、收费和评价模块提供稳定基础数据。'

async function loadData() {
  loading.value = true
  try {
    const response = await fetchStudentPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
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

function resetForm() {
  editingId.value = null
  form.fullName = ''
  form.gender = ''
  form.companyName = ''
  form.jobTitle = ''
  form.educationLevel = ''
  form.techLevel = ''
  form.phone = ''
  form.email = ''
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: StudentProfileRecord) {
  editingId.value = row.id
  form.fullName = row.fullName
  form.gender = row.gender
  form.companyName = row.companyName
  form.jobTitle = row.jobTitle
  form.educationLevel = row.educationLevel
  form.techLevel = row.techLevel
  form.phone = row.phone
  form.email = row.email
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
      await updateStudent(editingId.value, { ...form })
      ElMessage.success('学员档案已更新')
    } else {
      await createStudent({ ...form })
      ElMessage.success('学员档案已创建')
    }
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, editingId.value ? '学员档案更新失败，请稍后重试' : '学员档案创建失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

function handleDialogClosed() {
  formRef.value?.resetFields()
  resetForm()
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
        <div class="module-tag">{{ pageTag }}</div>
        <h2>{{ pageTitle }}</h2>
        <p>{{ pageDescription }}</p>
      </div>
      <div class="summary-metrics">
        <div class="metric-item">
          <span>学员档案数</span>
          <strong>{{ pageData.total }}</strong>
        </div>
        <div class="metric-item">
          <span>覆盖企业数</span>
          <strong>{{ companyCount }}</strong>
        </div>
      </div>
    </section>

    <section class="page-card toolbar-card">
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索学员编号 / 姓名 / 企业 / 电话 / 邮箱"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增学员</el-button>
    </section>

    <section class="page-card table-card">
      <el-table v-loading="loading" :data="pageData.list" stripe>
        <el-table-column prop="studentNo" label="学员编号" min-width="160" />
        <el-table-column prop="fullName" label="学员姓名" min-width="120" />
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="companyName" label="所属企业" min-width="180" show-overflow-tooltip />
        <el-table-column prop="jobTitle" label="岗位" min-width="120" show-overflow-tooltip />
        <el-table-column prop="educationLevel" label="学历" width="100" />
        <el-table-column prop="techLevel" label="技术级别" width="110" />
        <el-table-column prop="phone" label="联系电话" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="更新时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="EditPen" @click="openEditDialog(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑学员档案' : '新增学员档案'" width="760px" @closed="handleDialogClosed">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学员姓名" prop="fullName">
              <el-input v-model="form.fullName" placeholder="请输入学员姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" clearable placeholder="请选择性别" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属企业" prop="companyName">
              <el-input v-model="form.companyName" placeholder="请输入所属企业名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-input v-model="form.jobTitle" placeholder="请输入岗位名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学历">
              <el-input v-model="form.educationLevel" placeholder="如：本科 / 硕士" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="技术级别">
              <el-input v-model="form.techLevel" placeholder="如：初级 / 中级 / 高级" />
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
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱地址" />
            </el-form-item>
          </el-col>
        </el-row>
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
  background: rgba(14, 165, 233, 0.12);
  color: #0369a1;
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

.filter-row .el-input {
  max-width: 320px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
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
