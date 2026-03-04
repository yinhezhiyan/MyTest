import { ElMessage } from 'element-plus'
import router from '../router'
import axios from "axios";

const request = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    timeout: 30000
})

request.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';
    const user = JSON.parse(localStorage.getItem('system-user') || '{}')
    if (user.token) {
        config.headers['Authorization'] = `Bearer ${user.token}`
    }
    return config
}, error => Promise.reject(error));

request.interceptors.response.use(
    response => {
        let res = response.data;
        if (response.config.responseType === 'blob') {
            return res
        }
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res
        }
        if (res.code === '401') {
            ElMessage.error(res.msg || '登录已失效，请重新登录');
            localStorage.removeItem('system-user')
            router.push('/login')
        }
        return res;
    },
    error => {
        ElMessage.error('请求失败，请检查后端服务和数据库配置')
        return Promise.reject(error)
    }
)

export default request
