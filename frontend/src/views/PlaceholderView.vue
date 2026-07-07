<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const suggestions: Record<string, string[]> = {
  '/applications': ['申请录入表单', '申请列表与状态筛选', '经理审批流转'],
  '/courses': ['课程 CRUD', '课程状态发布', '讲师关联选择'],
  '/lecturers': ['讲师档案维护', '停用讲师', '讲师筛选'],
  '/students': ['学员信息维护', '所属公司字段', '报名关联'],
  '/notices': ['通知编辑', '报名时间窗口', '通知发布'],
  '/enrollments': ['报名列表', '报名审核', '名额校验'],
  '/attendance': ['签到名单', '签到操作', '签到备注'],
  '/payments': ['收费列表', '收费登记', '企业代缴/免收'],
  '/evaluations': ['评价录入', '评价列表', '课程评分汇总'],
  '/statistics': ['报名人数统计', '签到人数统计', '收费汇总与评价均分'],
}

const moduleName = computed(() => String(route.meta.title ?? '模块页面'))
const todoList = computed(() => suggestions[route.path] ?? ['等待进入下一日开发任务'])
</script>

<template>
  <div class="placeholder page-card">
    <div class="placeholder-head">
      <div>
        <div class="tag">Day8 基础骨架</div>
        <h2>{{ moduleName }}</h2>
        <p>当前页面已预留路由入口，后续按 Sprint Backlog 逐日补完业务 CRUD 与状态流转。</p>
      </div>
      <el-result icon="info" title="待开发" sub-title="当前版本先保证可登录、可进入、可演示" />
    </div>

    <div class="todo-panel">
      <div class="todo-title">进入本模块后的建议开发点</div>
      <el-steps direction="vertical" :active="todoList.length">
        <el-step v-for="todo in todoList" :key="todo" :title="todo" />
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
}
</style>
