import { ElMessage } from 'element-plus'
import router from '../router'
import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_BASE_URL,
  timeout: 30000
})

request.interceptors.request.use(config => {
  config.headers['Content-Type'] = 'application/json;charset=utf-8'
  const token = localStorage.getItem('system-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, error => Promise.reject(error))

request.interceptors.response.use(response => {
  let res = response.data
  if (response.config.responseType === 'blob') return res
  if (typeof res === 'string') {
    res = res ? JSON.parse(res) : res
  }
  if (res.code === '401') {
    ElMessage.error(res.msg || '登录失效')
    localStorage.removeItem('system-token')
    localStorage.removeItem('system-user')
    router.push('/login')
  }
  return res
}, error => Promise.reject(error))

export default request
