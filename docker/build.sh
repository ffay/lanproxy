#!/bin/bash

# LanProxy 容器化构建脚本

set -e

echo "========================================"
echo "LanProxy 容器化构建脚本"
echo "========================================"

# 检查是否存在Maven
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven，请先安装Maven"
    exit 1
fi

# 检查是否存在Docker
if ! command -v docker &> /dev/null; then
    echo "错误: 未找到Docker，请先安装Docker"
    exit 1
fi

# 进入项目根目录
cd "$(dirname "$0")/.."

echo "1. 清理之前的构建..."
mvn clean

echo "2. 编译和打包项目..."
mvn package -DskipTests

echo "3. 检查构建结果..."
if [ ! -d "distribution/proxy-server-0.1.1" ]; then
    echo "错误: proxy-server构建失败，未找到distribution/proxy-server-0.1.1目录"
    exit 1
fi

if [ ! -d "distribution/proxy-client-0.1" ]; then
    echo "错误: proxy-client构建失败，未找到distribution/proxy-client-0.1目录"
    exit 1
fi

echo "4. 构建Docker镜像..."

# 构建服务器镜像
echo "构建 lanproxy-server 镜像..."
docker build -f Dockerfile.server -t lanproxy-server:latest .

# 构建客户端镜像
echo "构建 lanproxy-client 镜像..."
docker build -f Dockerfile.client -t lanproxy-client:latest .

echo "5. 构建完成！"
echo "========================================"
echo "镜像构建成功:"
echo "  - lanproxy-server:latest"
echo "  - lanproxy-client:latest"
echo "========================================"
echo "使用以下命令启动服务:"
echo "  docker-compose up -d proxy-server"
echo "或者启动完整服务（包括客户端）:"
echo "  docker-compose --profile client up -d"
echo "========================================"