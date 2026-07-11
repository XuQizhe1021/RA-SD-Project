<script setup lang="ts">
import { ArrowRight, Lock, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getErrorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

async function handleLogin() {
  loading.value = true
  try {
    await authStore.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.push(redirect)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '登录失败，请检查账号信息'))
  } finally {
    loading.value = false
  }
}

function goToRegister() {
  router.push('/register/student')
}

onMounted(() => {
  if (typeof route.query.username === 'string') {
    form.username = route.query.username
  }
})
</script>

<template>
  <div class="login-page">
    <div class="intro-card">
      <div class="chip">系统入口</div>
      <h1>HQ技术培训管理系统</h1>
      <p>
        面向培训申请、课程执行、报名审核、签到收费、培训评价与统计分析的统一业务平台，
        支持多角色按职责边界协同完成培训业务闭环。
      </p>
      <div class="timeline page-card">
        <div class="timeline-item">
          <strong>业务覆盖</strong>
          <span>支持申请审批、课程维护、通知发布、报名处理、签到收费与统计分析。</span>
        </div>
        <div class="timeline-item">
          <strong>权限控制</strong>
          <span>系统根据当前账号角色展示可用菜单，并限制跨职责访问与操作。</span>
        </div>
      </div>
    </div>

    <div class="login-card page-card">
      <div class="card-header">
        <h2>系统登录</h2>
        <span>请输入账号和密码后登录系统；如尚未开通学员账号，可先提交注册申请。</span>
      </div>
      <el-form label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入用户名">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            placeholder="请输入密码"
            show-password
            type="password"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-button class="login-button" :loading="loading" type="primary" @click="handleLogin">
          登录系统
        </el-button>
      </el-form>

      <div class="register-panel">
        <div class="register-title">还没有账号？</div>
        <el-button text type="primary" :icon="ArrowRight" @click="goToRegister">前往学员注册</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 32px;
  padding: 40px;
}

.intro-card {
  padding: 36px;
  color: #0f172a;
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-weight: 600;
}

h1 {
  margin: 22px 0 18px;
  font-size: 44px;
  line-height: 1.2;
}

p {
  max-width: 640px;
  font-size: 18px;
  color: #475569;
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

.login-card {
  align-self: center;
  padding: 32px;
}

.card-header {
  margin-bottom: 16px;
}

.card-header h2 {
  margin: 0 0 8px;
  font-size: 28px;
  color: #0f172a;
}

.card-header span {
  color: #64748b;
}

.login-button {
  width: 100%;
  height: 44px;
  margin-top: 10px;
}

.register-panel {
  margin-top: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.register-title {
  font-size: 14px;
  color: #475569;
}

@media (max-width: 1024px) {
  .login-page {
    grid-template-columns: 1fr;
    padding: 20px;
  }

  .intro-card,
  .login-card {
    padding: 24px;
  }

  h1 {
    font-size: 34px;
  }
}
</style>
