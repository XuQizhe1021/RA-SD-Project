<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { useAuthStore } from '../stores/auth'

type RoleCode = 'MANAGER' | 'EXECUTOR' | 'SITE_STAFF' | 'STUDENT'

interface ModuleConfig {
  status: '待开发' | '规划中'
  summary: string
  ownerRoles: RoleCode[]
  featuresByRole: Partial<Record<RoleCode, string[]>>
  nextSteps: string[]
}

const route = useRoute()
const authStore = useAuthStore()

const roleNameMap: Record<RoleCode, string> = {
  MANAGER: '经理',
  EXECUTOR: '执行人',
  SITE_STAFF: '现场工作人员',
  STUDENT: '学员',
}

const moduleConfigs: Record<string, ModuleConfig> = {
  '/applications': {
    status: '待开发',
    summary: '经理负责受理培训申请、审批预算和培训主题，并将申请转化为后续课程计划与执行任务。',
    ownerRoles: ['MANAGER'],
    featuresByRole: {
      MANAGER: ['录入培训申请', '审批申请并填写意见', '指定执行人并形成课程来源'],
    },
    nextSteps: ['打通申请录入与审批状态流转', '将申请关联到课程创建', '沉淀申请审核日志'],
  },
  '/students': {
    status: '待开发',
    summary: '执行人维护学员档案与企业信息，支撑报名、签到、收费、评价等后续环节的数据关联。',
    ownerRoles: ['EXECUTOR'],
    featuresByRole: {
      EXECUTOR: ['维护学员档案', '维护所属公司与联系方式', '支持报名流程选择学员'],
    },
    nextSteps: ['补齐学员 CRUD', '关联企业客户信息', '支持报名名单快速检索'],
  },
  '/notices': {
    status: '待开发',
    summary: '执行人负责发布课程通知，学员负责浏览通知并根据通知报名参加培训。',
    ownerRoles: ['EXECUTOR', 'STUDENT'],
    featuresByRole: {
      EXECUTOR: ['编辑培训通知', '设置报名时间窗口', '发布开课提醒与开课前通知'],
      STUDENT: ['浏览课程通知', '查看报名截止时间', '了解课程地点与费用说明'],
    },
    nextSteps: ['补齐通知编辑与发布', '关联已发布课程', '展示学员端通知详情'],
  },
  '/evaluations': {
    status: '待开发',
    summary: '培训结束后，学员提交课程评价，现场工作人员负责回收整理评价数据并形成课程反馈依据。',
    ownerRoles: ['SITE_STAFF', 'STUDENT'],
    featuresByRole: {
      SITE_STAFF: ['查看待回收评价名单', '汇总课程满意度', '整理课程反馈意见'],
      STUDENT: ['填写课程满意度', '提交意见与建议', '查看自己已提交的评价'],
    },
    nextSteps: ['补齐评价录入与查询', '限制已签到学员才能评价', '汇总课程评分供统计使用'],
  },
  '/statistics': {
    status: '规划中',
    summary: '经理与执行人共享统计报表，但关注点不同：经理看经营结果，执行人看课程执行与人员表现。',
    ownerRoles: ['MANAGER', 'EXECUTOR'],
    featuresByRole: {
      MANAGER: ['查看培训收入汇总', '查看课程开班与报名趋势', '查看执行人工作情况'],
      EXECUTOR: ['查看课程报名签到情况', '查看讲师与学员维度统计', '查看课程执行效果'],
    },
    nextSteps: ['补齐报名/签到/收费统计 SQL', '区分经理视角与执行人视角', '输出汇总指标卡片与表格'],
  },
}

const currentRole = computed(() => (authStore.primaryRole || 'STUDENT') as RoleCode)
const moduleName = computed(() => String(route.meta.title ?? '模块页面'))
const moduleConfig = computed(
  () =>
    moduleConfigs[route.path] ?? {
      status: '待开发',
      summary: '该模块已预留入口，后续会按角色职责补齐业务逻辑与数据库流转。',
      ownerRoles: [currentRole.value],
      featuresByRole: {
        [currentRole.value]: ['补齐页面占位信息', '明确该角色可执行的功能', '等待后续迭代接入真实业务'],
      },
      nextSteps: ['继续完善模块设计', '接入数据库表结构', '补充角色差异化页面'],
    },
)

const currentRoleFeatures = computed(
  () => moduleConfig.value.featuresByRole[currentRole.value] ?? ['当前角色仅具备查看模块说明的权限'],
)

const ownerRoleNames = computed(() =>
  moduleConfig.value.ownerRoles.map((role) => roleNameMap[role]).join('、'),
)
</script>

<template>
  <div class="placeholder page-card">
    <div class="placeholder-head">
      <div>
        <div class="tag">{{ moduleConfig.status }}</div>
        <h2>{{ moduleName }}</h2>
        <p>{{ moduleConfig.summary }}</p>
      </div>
      <el-result icon="info" :title="moduleConfig.status" sub-title="当前页面已按角色职责优化展示，后续继续补齐业务实现" />
    </div>

    <div class="meta-grid">
      <div class="meta-card">
        <div class="todo-title">当前角色</div>
        <div class="meta-value">{{ roleNameMap[currentRole] }}</div>
      </div>
      <div class="meta-card">
        <div class="todo-title">模块归属角色</div>
        <div class="meta-value">{{ ownerRoleNames }}</div>
      </div>
    </div>

    <div class="todo-panel">
      <div class="todo-title">当前角色可使用的功能</div>
      <el-steps direction="vertical" :active="currentRoleFeatures.length">
        <el-step v-for="feature in currentRoleFeatures" :key="feature" :title="feature" />
      </el-steps>
    </div>

    <div class="todo-panel">
      <div class="todo-title">后续补齐重点</div>
      <el-steps direction="vertical" :active="moduleConfig.nextSteps.length">
        <el-step v-for="todo in moduleConfig.nextSteps" :key="todo" :title="todo" />
      </el-steps>
    </div>
  </div>
</template>

<style scoped>
.placeholder {
  padding: 28px;
}

.placeholder-head {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 24px;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
  font-weight: 600;
}

h2 {
  margin: 18px 0 10px;
  font-size: 30px;
  color: #0f172a;
}

p {
  max-width: 680px;
  color: #64748b;
}

.todo-panel {
  margin-top: 18px;
  padding: 20px;
  border-radius: 18px;
  background: #f8fafc;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-top: 18px;
}

.meta-card {
  padding: 20px;
  border-radius: 18px;
  background: #f8fafc;
}

.meta-value {
  margin-top: 10px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.todo-title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

@media (max-width: 1024px) {
  .placeholder-head {
    grid-template-columns: 1fr;
  }

  .meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
