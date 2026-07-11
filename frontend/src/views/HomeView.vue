<script setup lang="ts">
import { computed } from 'vue'

import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const primaryRole = computed(() => authStore.primaryRole)

const roleNameMap: Record<string, string> = {
  MANAGER: '经理',
  EXECUTOR: '执行人',
  SITE_STAFF: '现场工作人员',
  STUDENT: '学员',
}

const heroContent = computed(() => {
  if (primaryRole.value === 'MANAGER') {
    return {
      tag: '首页概览',
      title: '关注培训申请、课程计划和经营统计',
      description: '围绕培训申请处理、课程推进情况和统计结果查看整体业务运行状态。',
      alert: '经理可查看课程与讲师资源情况，并在培训申请、统计报表模块跟踪整体经营表现。',
    }
  }
  if (primaryRole.value === 'EXECUTOR') {
    return {
      tag: '首页概览',
      title: '负责课程落地、讲师协调、通知发布与报名审核',
      description: '围绕课程、讲师、学员、通知和报名流转组织培训执行工作。',
      alert: '执行人是培训主流程的核心协调角色，需持续跟进报名审核、开课提醒与现场交接。',
    }
  }
  if (primaryRole.value === 'SITE_STAFF') {
    return {
      tag: '首页概览',
      title: '聚焦签到、收费与培训现场支持',
      description: '围绕培训当天的签到、收费和现场收尾工作开展处理与核验。',
      alert: '现场工作人员负责培训当日到场核验、资料发放、收费登记与评价回收。',
    }
  }
  return {
    tag: '首页概览',
    title: '查看通知、提交报名、完成缴费与课程评价',
    description: '围绕通知浏览、报名、缴费和评价提交完成个人培训参与流程。',
    alert: '学员仅可查看和操作自己的报名及缴费记录，不再接触后台审核类操作。',
  }
})

const stats = computed(() => {
  const baseStats = [
    {
      title: '当前登录角色',
      value: roleNameMap[primaryRole.value] ?? '未识别',
      description: '系统会根据账号职责开放对应菜单与业务功能。',
    },
    {
      title: '当前开放模块',
      value: `${authStore.menus.length} 个`,
      description: authStore.menus.map((item) => item.name).join(' / '),
    },
  ]

  if (primaryRole.value === 'MANAGER') {
    return baseStats.concat([
      {
        title: '角色关注点',
        value: '申请审批',
        description: '确认培训需求、预算与课程计划是否合理',
      },
      {
        title: '经营关注点',
        value: '统计报表',
        description: '关注课程开班、收入汇总与执行情况',
      },
    ])
  }

  if (primaryRole.value === 'EXECUTOR') {
    return baseStats.concat([
      {
        title: '主流程职责',
        value: '课程到报名',
        description: '负责课程维护、通知发布、报名审核与开课提醒',
      },
      {
        title: '当前重点',
        value: '数据流转',
        description: '为签到、收费、评价等后续模块提供准确数据',
      },
    ])
  }

  if (primaryRole.value === 'SITE_STAFF') {
    return baseStats.concat([
      {
        title: '现场动作',
        value: '签到 + 收费',
        description: '核验报名名单、记录签到、完成收费登记',
      },
      {
        title: '培训收尾',
        value: '评价回收',
        description: '培训结束后整理课程反馈与满意度信息',
      },
    ])
  }

  return baseStats.concat([
    {
      title: '报名入口',
      value: '通知 / 报名',
      description: '先查看通知，再选择课程并提交报名',
    },
    {
      title: '后续动作',
      value: '缴费 / 评价',
      description: '报名确认后完成缴费，培训结束后提交评价',
    },
  ])
})

const milestones = computed(() => {
  if (primaryRole.value === 'MANAGER') {
    return ['查看培训申请处理进度', '查看课程与讲师准备情况', '在统计报表中跟踪经营结果']
  }
  if (primaryRole.value === 'EXECUTOR') {
    return ['维护课程与讲师基础数据', '发布通知并处理报名审核', '为现场签到与收费准备完整名单']
  }
  if (primaryRole.value === 'SITE_STAFF') {
    return ['查看已确认报名名单', '完成培训当天签到与收费', '回收评价并整理现场反馈']
  }
  return ['浏览已发布的培训通知', '提交并跟踪自己的报名记录', '完成缴费并在培训结束后提交评价']
})

const guidanceItems = computed(() => {
  if (primaryRole.value === 'MANAGER') {
    return ['通过培训申请跟踪需求受理情况', '通过课程与讲师信息判断资源配置', '通过统计报表查看经营结果和执行进展']
  }
  if (primaryRole.value === 'EXECUTOR') {
    return ['优先维护课程、讲师和学员基础数据', '及时发布通知并处理报名审核', '培训前与现场工作人员核对签到和收费准备情况']
  }
  if (primaryRole.value === 'SITE_STAFF') {
    return ['按已确认报名名单执行签到', '根据收费记录完成现场收费登记', '培训结束后协助整理评价反馈']
  }
  return ['先查看通知，再按课程安排提交报名', '报名确认后关注缴费状态', '培训结束后及时提交课程评价']
})
</script>

<template>
  <div class="dashboard">
    <section class="hero page-card">
      <div>
        <div class="hero-tag">{{ heroContent.tag }} · {{ authStore.user?.displayName }}</div>
        <h2>{{ heroContent.title }}</h2>
        <p>{{ heroContent.description }}</p>
      </div>
      <el-alert
        :title="heroContent.alert"
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
        <div class="section-title">业务处理重点</div>
        <el-timeline>
          <el-timeline-item v-for="milestone in milestones" :key="milestone" type="primary">
            {{ milestone }}
          </el-timeline-item>
        </el-timeline>
      </article>

      <article class="page-card account-card">
        <div class="section-title">使用说明</div>
        <el-timeline>
          <el-timeline-item v-for="item in guidanceItems" :key="item" type="success">
            {{ item }}
          </el-timeline-item>
        </el-timeline>
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
