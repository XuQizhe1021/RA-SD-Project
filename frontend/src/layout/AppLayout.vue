<script setup lang="ts">
import {
  Calendar,
  DataAnalysis,
  Document,
  EditPen,
  HomeFilled,
  Money,
  Reading,
  School,
  Ticket,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const roleNameMap: Record<string, string> = {
  MANAGER: '经理',
  EXECUTOR: '执行人',
  SITE_STAFF: '现场工作人员',
  STUDENT: '学员',
}

const iconMap = {
  HomeFilled,
  EditPen,
  Reading,
  UserFilled,
  User,
  Document,
  Ticket,
  Calendar,
  Money,
  DataAnalysis,
  School,
}

const activeMenu = computed(() => route.path)

const titleMap: Record<string, string> = {
  '/dashboard': '首页概览',
  '/applications': '培训申请',
  '/courses': '课程管理',
  '/lecturers': '讲师管理',
  '/students': '学员管理',
  '/notices': '通知发布',
  '/enrollments': '报名管理',
  '/attendance': '签到管理',
  '/payments': '收费管理',
  '/evaluations': '评价管理',
  '/statistics': '统计报表',
}

const currentTitle = computed(() => titleMap[route.path] ?? 'HQ技术培训管理系统')
const currentRoleName = computed(() => roleNameMap[authStore.primaryRole] ?? authStore.primaryRole)
const topbarMeta = computed(() => {
  if (authStore.hasRole('MANAGER')) {
    return '聚焦培训申请审批、课程计划跟踪与经营统计分析。'
  }
  if (authStore.hasRole('EXECUTOR')) {
    return '维护课程、讲师、学员、通知与报名等培训执行数据。'
  }
  if (authStore.hasRole('SITE_STAFF')) {
    return '负责签到登记、收费处理与培训现场收尾工作。'
  }
  return '查看培训通知、提交报名、完成缴费并参与课程评价。'
})

function menuIcon(icon?: string) {
  return icon ? iconMap[icon as keyof typeof iconMap] : HomeFilled
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout-shell">
    <el-aside class="sidebar" width="248px">
      <div class="brand-block">
        <div class="brand-icon">HQ</div>
        <div>
          <div class="brand-title">HQ技术培训管理系统</div>
          <div class="brand-subtitle">培训业务管理平台</div>
        </div>
      </div>
      <el-menu :default-active="activeMenu" class="menu-panel" router>
        <el-menu-item
          v-for="menu in authStore.menus"
          :key="menu.path"
          :index="menu.path"
        >
          <el-icon>
            <component :is="menuIcon(menu.icon)" />
          </el-icon>
          <span>{{ menu.name }}</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <div class="footer-label">当前角色</div>
        <div class="footer-value">{{ currentRoleName }}</div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <div class="topbar-title">{{ currentTitle }}</div>
          <div class="topbar-meta">{{ topbarMeta }}</div>
        </div>
        <div class="topbar-actions">
          <el-tag type="primary" effect="light">{{ currentRoleName }}权限已启用</el-tag>
          <el-dropdown>
            <span class="user-badge">
              <el-avatar :size="34">{{ authStore.user?.displayName?.slice(0, 1) }}</el-avatar>
              <span>{{ authStore.user?.displayName }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="content-area">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-shell {
  min-height: 100vh;
}

.sidebar {
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #0f172a 0%, #172554 100%);
  color: #e2e8f0;
  padding: 24px 18px;
  gap: 20px;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 4px 8px 16px;
}

.brand-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #0f172a;
  background: linear-gradient(135deg, #fde68a, #f59e0b);
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
}

.brand-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.72);
}

.menu-panel {
  border-right: none;
  background: transparent;
}

.menu-panel :deep(.el-menu-item) {
  margin-bottom: 8px;
  border-radius: 14px;
  color: #cbd5e1;
}

.menu-panel :deep(.el-menu-item.is-active) {
  background: rgba(59, 130, 246, 0.2);
  color: #ffffff;
}

.menu-panel :deep(.el-menu-item:hover) {
  background: rgba(148, 163, 184, 0.12);
}

.sidebar-footer {
  margin-top: auto;
  padding: 16px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.38);
}

.footer-label {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.7);
}

.footer-value {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px 0;
  height: auto;
}

.topbar-title {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
}

.topbar-meta {
  margin-top: 6px;
  color: #64748b;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 18px;
}

.user-badge {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  cursor: pointer;
}

.content-area {
  padding: 24px 28px 32px;
}
</style>
