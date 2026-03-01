import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    {
      path: '/manager',
      component: () => import('@/views/Manager.vue'),
      redirect: '/manager/home',
      children: [
        { path: 'home', component: () => import('@/views/manager/Home.vue') },
        { path: 'admin', component: () => import('@/views/manager/Admin.vue') },
        { path: 'users', component: () => import('@/views/manager/Users.vue') },
        { path: 'exercises', component: () => import('@/views/manager/Exercises.vue') },
        { path: 'knowledge', component: () => import('@/views/manager/Knowledge.vue') },
        { path: 'recommend', component: () => import('@/views/manager/Recommend.vue') },
        { path: 'recommend-logs', component: () => import('@/views/manager/RecommendLogs.vue') }
      ]
    },
    { path: '/login', component: () => import('@/views/Login.vue') },
    { path: '/register', component: () => import('@/views/Register.vue') }
  ]
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
