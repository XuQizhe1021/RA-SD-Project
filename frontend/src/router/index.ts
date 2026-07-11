import { createRouter, createWebHistory } from 'vue-router'

import AppLayout from '../layout/AppLayout.vue'
import ApplicationsView from '../views/ApplicationsView.vue'
import AttendanceView from '../views/AttendanceView.vue'
import { useAuthStore } from '../stores/auth'
import CoursesView from '../views/CoursesView.vue'
import EnrollmentsView from '../views/EnrollmentsView.vue'
import EvaluationView from '../views/EvaluationView.vue'
import HomeView from '../views/HomeView.vue'
import LecturersView from '../views/LecturersView.vue'
import LoginView from '../views/LoginView.vue'
import NoticesView from '../views/NoticesView.vue'
import PaymentsView from '../views/PaymentsView.vue'
import StatisticsView from '../views/StatisticsView.vue'
import StudentsView from '../views/StudentsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true, title: '系统登录' },
    },
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          redirect: '/dashboard',
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: HomeView,
          meta: { title: '首页概览' },
        },
        {
          path: 'applications',
          name: 'applications',
          component: ApplicationsView,
          meta: { title: '培训申请' },
        },
        {
          path: 'courses',
          name: 'courses',
          component: CoursesView,
          meta: { title: '课程管理' },
        },
        {
          path: 'lecturers',
          name: 'lecturers',
          component: LecturersView,
          meta: { title: '讲师管理' },
        },
        {
          path: 'students',
          name: 'students',
          component: StudentsView,
          meta: { title: '学员管理' },
        },
        {
          path: 'notices',
          name: 'notices',
          component: NoticesView,
          meta: { title: '通知发布' },
        },
        {
          path: 'enrollments',
          name: 'enrollments',
          component: EnrollmentsView,
          meta: { title: '报名管理' },
        },
        {
          path: 'attendance',
          name: 'attendance',
          component: AttendanceView,
          meta: { title: '签到管理' },
        },
        {
          path: 'payments',
          name: 'payments',
          component: PaymentsView,
          meta: { title: '收费管理' },
        },
        {
          path: 'evaluations',
          name: 'evaluations',
          component: EvaluationView,
          meta: { title: '评价管理' },
        },
        {
          path: 'statistics',
          name: 'statistics',
          component: StatisticsView,
          meta: { title: '统计报表' },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  document.title = `${String(to.meta.title ?? 'HQ技术培训管理系统')} - HQ技术培训管理系统`

  if (to.meta.public) {
    return true
  }

  const authStore = useAuthStore()
  authStore.restoreSession()

  if (!authStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (!authStore.user) {
    try {
      await authStore.bootstrap()
    } catch {
      authStore.logout()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  const accessiblePaths = new Set(authStore.accessiblePaths)
  if (to.path !== '/dashboard' && !accessiblePaths.has(to.path)) {
    const fallbackPath = authStore.menus[0]?.path ?? '/dashboard'
    return { path: fallbackPath }
  }

  return true
})

export default router
