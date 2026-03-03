import {createRouter, createWebHistory} from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    {
      path: '/manager/:subject',
      component: () => import('@/views/Manager.vue'),
      redirect: to => `/manager/${to.params.subject}/home`,
      children: [
        { path: 'home', component: () => import('@/views/manager/Home.vue')},
        { path: 'admin', component: () => import('@/views/manager/Admin.vue')},
        { path: 'daily', component: () => import('@/views/student/Daily.vue')},
        { path: 'practice', component: () => import('@/views/student/Practice.vue')},
        { path: 'recommend', component: () => import('@/views/student/Recommend.vue')},
        { path: 'records', component: () => import('@/views/student/Records.vue')},
      ]
    },
    { path: '/login', component: () => import('@/views/Login.vue') }
  ]
})

router.beforeEach((to, from, next) => {
  const user = JSON.parse(localStorage.getItem('system-user') || '{}')
  if (to.path.startsWith('/manager')) {
    if (!user?.token) return next('/login')
    if (to.params.subject && to.params.subject !== user.subject) return next(`/manager/${user.subject}/home`)
    if (to.path.endsWith('/admin') && user.role !== 'ADMIN') return next(`/manager/${user.subject}/home`)
  }
  next()
})

export default router
