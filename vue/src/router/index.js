import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: () => import('@/views/Login.vue') },
    { path: '/register', component: () => import('@/views/Register.vue') },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/admin/home',
      children: [
        { path: 'home', component: () => import('@/views/admin/AdminHome.vue') },
        { path: 'users', component: () => import('@/views/manager/Users.vue') },
        { path: 'exercises', component: () => import('@/views/manager/Exercises.vue') },
        { path: 'knowledge', component: () => import('@/views/manager/Knowledge.vue') },
        { path: 'recommend-logs', component: () => import('@/views/manager/RecommendLogs.vue') }
      ]
    },
    {
      path: '/student',
      component: () => import('@/layouts/StudentLayout.vue'),
      redirect: '/student/dashboard',
      children: [
        { path: 'dashboard', component: () => import('@/views/student/StudentDashboard.vue') },
        { path: 'recommend', component: () => import('@/views/student/StudentRecommend.vue') },
        { path: 'practice', component: () => import('@/views/student/StudentPractice.vue') },
        { path: 'records', component: () => import('@/views/student/StudentRecords.vue') }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('system-token')
  const user = JSON.parse(localStorage.getItem('system-user') || '{}')

  if ((to.path.startsWith('/admin') || to.path.startsWith('/student')) && !token) {
    next('/login')
    return
  }

  if (to.path.startsWith('/admin') && user.role === 'STUDENT') {
    next('/student/dashboard')
    return
  }

  if (to.path.startsWith('/student') && user.role === 'ADMIN') {
    next('/admin/home')
    return
  }

  next()
})

export default router

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('system-token')
  if (to.path.startsWith('/manager') && !token) {
    next('/login')
    return
  }
  next()
})
