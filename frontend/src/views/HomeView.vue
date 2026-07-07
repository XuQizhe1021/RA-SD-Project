<script setup lang="ts">
import { computed } from 'vue'

import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const stats = computed(() => [
  {
    title: '今日已完成',
    value: '4 项',
    description: '登录、鉴权、角色菜单、首页布局',
  },
  {
    title: '下一步优先级',
    value: 'P0',
    description: '课程管理、讲师管理、通知与报名',
  },
  {
    title: 'Sprint 范围',
    value: '增量1',
    description: '只做 PC Web 最小闭环，不混入增量2',
  },
  {
    title: '当前登录角色',
    value: authStore.user?.roles[0] ?? '未识别',
    description: '菜单按角色动态返回，后续可扩接口级权限',
  },
])

const milestones = [
  '7.8 完成课程管理与讲师管理',
  '7.9 打通通知发布、学员管理与报名审核',
  '7.10 完成签到与收费主流程',
  '7.11 完成评价、统计与联调修复',
]
</script>

<template>
  <div class="dashboard">
    <section class="hero page-card">
      <div>
        <div class="hero-tag">欢迎回来，{{ authStore.user?.displayName }}</div>
        <h2>7.7 Sprint 启动日基础工程已就位</h2>
        <p>
          当前版本已经具备系统登录、角色菜单、首页布局和数据库脚本基础，可以作为后续
          `M1-M9` 模块开发的统一底座。
        </p>
      </div>
      <el-alert
        title="建议开发顺序：M0 -> M2 -> M3 -> M1 -> M5 -> M6 -> M7 -> M8 -> M9"
        type="success"
        :closable="false"
        show-icon
      />
    </section>

    <section class="stat-grid">
      <article v-for="item in stats" :key="item.title" class="stat-card page-card">
        <div class="stat-title">{{ item.title }}</div>
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-desc">{{ item.description }}</div>
      </article>
    </section>

    <section class="bottom-grid">
      <article class="page-card plan-card">
        <div class="section-title">后续冲刺计划</div>
        <el-timeline>
          <el-timeline-item v-for="milestone in milestones" :key="milestone" type="primary">
            {{ milestone }}
          </el-timeline-item>
        </el-timeline>
      </article>

      <article class="page-card account-card">
        <div class="section-title">演示账号提醒</div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="经理">manager01 / 123456</el-descriptions-item>
          <el-descriptions-item label="执行人">executor01 / 123456</el-descriptions-item>
          <el-descriptions-item label="现场工作人员">staff01 / 123456</el-descriptions-item>
          <el-descriptions-item label="学员">student01 / 123456</el-descriptions-item>
        </el-descriptions>
      </article>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.hero {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 24px;
  padding: 28px;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-weight: 600;
}

h2 {
  margin: 16px 0 10px;
  font-size: 30px;
  color: #0f172a;
}

p {
  margin: 0;
  max-width: 760px;
  color: #64748b;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.stat-card {
  padding: 22px;
}

.stat-title {
  color: #64748b;
}

.stat-value {
  margin: 12px 0 8px;
  font-size: 30px;
  font-weight: 700;
  color: #0f172a;
}

.stat-desc {
  color: #475569;
}

.bottom-grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 18px;
}

.plan-card,
.account-card {
  padding: 24px;
}

.section-title {
  margin-bottom: 18px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

@media (max-width: 1200px) {
  .hero,
  .stat-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}
</style>
