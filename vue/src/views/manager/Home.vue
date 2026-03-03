<template>
  <div>

    <div class="card" style="line-height: 30px; margin-bottom: 10px">
      <div style="margin-bottom: 20px; font-size: 24px">青哥带小白做毕设（2026最新版）</div>
      <div>同学们好！我是 <a href="https://space.bilibili.com/402779077">程序员青戈</a>，接下来我会从0开始带你搭建一套完整的毕设Web项目。</div>
      <div>技术栈：SpringBoot3 + Vue3 + MySQL，前后端分离</div>
      <div>目标是让你自己学会做一套完整的前后端分离的项目，<b>毕设不求人！毕设不被骗！</b></div>
    </div>

    <div class="card">
      本套教程 <b style="color: red">完全免费~</b> 宝子们记得B站传统美德走一走！<b style="color: red">一键三连</b> 支持一波青哥！你们的支持是我持续更新的最大动力！
    </div>

  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request";

const data = reactive({
  user: JSON.parse(localStorage.getItem('system-user') || '{}'),
})
</script>