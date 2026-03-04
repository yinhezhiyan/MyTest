#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PUBLIC_LINK="false"
if [[ "${1:-}" == "--public" ]]; then
  PUBLIC_LINK="true"
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "[ERROR] 未检测到 docker，请先安装 Docker Desktop / Docker Engine。" >&2
  exit 1
fi

echo "[1/2] 构建并启动服务（MySQL + Backend + Web）..."
docker compose up -d --build

HOST_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
if [[ -z "${HOST_IP}" ]]; then
  HOST_IP="127.0.0.1"
fi

echo
echo "✅ 打包并启动完成，可通过以下链接访问："
echo "- 本机链接: http://localhost:8088"
echo "- 局域网链接: http://${HOST_IP}:8088"

echo
echo "停止服务命令：docker compose down"

if [[ "$PUBLIC_LINK" == "true" ]]; then
  if ! command -v npx >/dev/null 2>&1; then
    echo "[WARN] 未检测到 npx，无法生成公网临时链接。请先安装 Node.js。" >&2
    exit 0
  fi
  echo
  echo "[2/2] 生成公网临时链接（localtunnel）..."
  echo "提示：按 Ctrl+C 可关闭公网链接（不会停止容器服务）。"
  npx localtunnel --port 8088
fi
