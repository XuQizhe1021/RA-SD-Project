<script setup lang="ts">
import { ArrowLeft, Lock, User } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { registerStudentAccount, type StudentRegisterPayload } from '../api/account'
import { getErrorMessage } from '../api/http'

const router = useRouter()
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<StudentRegisterPayload & { confirmPassword: string }>({
  username: '',
  password: '',
  confirmPassword: '',
  displayName: '',
  gender: '',
  companyName: '',
  jobTitle: '',
  educationLevel: '',
  techLevel: '',
  phone: '',
  email: '',
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  displayName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  companyName: [{ required: true, message: '请输入所属企业', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    await registerStudentAccount({
      username: form.username,
      password: form.password,
      displayName: form.displayName,
      gender: form.gender || undefined,
      companyName: form.companyName,
      jobTitle: form.jobTitle || undefined,
      educationLevel: form.educationLevel || undefined,
      techLevel: form.techLevel || undefined,
      phone: form.phone,
      email: form.email || undefined,
    })
    ElMessage.success('注册申请已提交，审核通过后即可登录系统')
    router.push({ path: '/login', query: { username: form.username } })
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '注册申请提交失败，请稍后重试'))
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="register-page">
    <section class="intro-card">
      <div class="chip">账号注册</div>
      <h1>学员账号申请</h1>
      <p>
        请填写真实的个人与企业信息。注册申请提交后，系统会进行审核；审核通过后，您即可登录并办理课程报名、缴费与评价等业务。
      </p>
      <div class="timeline page-card">
        <div class="timeline-item">
          <strong>注册范围</strong>
          <span>公开注册仅支持学员账号。管理与现场岗位账号由系统管理员统一创建。</span>
        </div>
        <div class="timeline-item">
          <strong>审核说明</strong>
          <span>请确保姓名、手机号和所属企业信息准确无误，以便及时完成审核。</span>
        </div>
      </div>
    </section>

    <section class="register-card page-card">
      <div class="card-header">
        <h2>填写注册信息</h2>
        <span>系统将根据提交内容建立学员账号与学员档案。</span>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="displayName">
              <el-input v-model="form.displayName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="请输入密码">
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属企业" prop="companyName">
              <el-input v-model="form.companyName" placeholder="请输入所属企业" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-input v-model="form.jobTitle" placeholder="请输入岗位名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="性别">
              <el-select v-model="form.gender" clearable placeholder="请选择性别" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学历">
              <el-input v-model="form.educationLevel" placeholder="如：本科 / 硕士" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="技术级别">
              <el-input v-model="form.techLevel" placeholder="如：初级 / 中级 / 高级" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="action-row">
          <el-button :icon="ArrowLeft" @click="goToLogin">返回登录</el-button>
          <el-button type="primary" :loading="loading" @click="handleSubmit">提交注册申请</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1.1fr;
  gap: 28px;
  padding: 32px;
}

.intro-card {
  padding: 32px;
  color: #0f172a;
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
  font-weight: 600;
}

h1 {
  margin: 22px 0 16px;
  font-size: 40px;
  line-height: 1.2;
}

p {
  margin: 0;
  color: #475569;
  font-size: 17px;
  line-height: 1.8;
}

.timeline {
  margin-top: 28px;
  padding: 22px;
}

.timeline-item + .timeline-item {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
}

.timeline-item strong {
  display: block;
  margin-bottom: 6px;
}

.timeline-item span {
  color: #64748b;
}

.register-card {
  padding: 28px 30px;
}

.card-header {
  margin-bottom: 18px;
}

.card-header h2 {
  margin: 0 0 8px;
  font-size: 28px;
  color: #0f172a;
}

.card-header span {
  color: #64748b;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

@media (max-width: 1080px) {
  .register-page {
    grid-template-columns: 1fr;
    padding: 20px;
  }

  .intro-card,
  .register-card {
    padding: 24px;
  }

  h1 {
    font-size: 32px;
  }
}
</style>
