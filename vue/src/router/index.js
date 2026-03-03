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
      ]
    },
    { path: '/login', component: () => import('@/views/Login.vue') }
  ]
})

router.beforeEach((to, from, next) => {
  const user = JSON.parse(localStorage.getItem('system-user') || '{}')
  if (to.path.startsWith('/manager')) {
    if (!user?.token) {
      next('/login')
      return
    }
    const routeSubject = to.params.subject
    if (routeSubject && routeSubject !== user.subject) {
      next(`/manager/${user.subject}/home`)
      return
    }
    if (to.path.endsWith('/admin') && user.role !== 'ADMIN') {
      next(`/manager/${user.subject}/home`)
      return
    }
  }
  next()
})

export default router
