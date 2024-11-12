#!/bin/bash

# LanProxy 容器化部署脚本

set -e

echo "========================================"
echo "LanProxy 容器化部署脚本"
echo "========================================"

# 进入项目根目录
cd "$(dirname "$0")/.."

# 检查环境变量文件
if [ ! -f ".env" ]; then
    echo "警告: 未找到.env文件，将使用默认配置"
    echo "建议复制.env.example为.env并根据实际情况修改配置"
    echo ""
fi

# 创建数据目录
echo "1. 创建数据目录..."
mkdir -p data/server-logs data/client-logs data/server-config

# 检查Docker镜像是否存在
echo "2. 检查Docker镜像..."
if ! docker image inspect lanproxy-server:latest &> /dev/null; then
    echo "未找到lanproxy-server镜像，开始构建..."
    ./docker/build.sh
fi

# 显示部署选项
echo "3. 选择部署模式:"
echo "  1) 仅部署服务器"
echo "  2) 部署服务器和客户端"
echo "  3) 停止所有服务"
echo "  4) 查看服务状态"
echo "  5) 查看服务日志"
read -p "请选择 (1-5): " choice

case $choice in
    1)
        echo "启动代理服务器..."
        docker-compose up -d proxy-server
        echo "服务器启动完成！"
        echo "Web管理界面: http://localhost:8090"
        echo "默认账号: admin / admin"
        ;;
    2)
        echo "启动代理服务器和客户端..."
        if [ -f ".env" ]; then
            docker-compose --profile client up -d
        else
            echo "错误: 部署客户端需要配置.env文件中的CLIENT_KEY"
            echo "请先启动服务器，在Web管理界面创建客户端并获取CLIENT_KEY"
            exit 1
        fi
        echo "服务启动完成！"
        echo "Web管理界面: http://localhost:8090"
        ;;
    3)
        echo "停止所有服务..."
        docker-compose --profile client down
        echo "服务已停止"
        ;;
    4)
        echo "服务状态:"
        docker-compose ps
        ;;
    5)
        echo "选择要查看的日志:"
        echo "  1) 服务器日志"
        echo "  2) 客户端日志"
        read -p "请选择 (1-2): " log_choice
        case $log_choice in
            1)
                docker-compose logs -f proxy-server
                ;;
            2)
                docker-compose logs -f proxy-client
                ;;
            *)
                echo "无效选择"
                ;;
        esac
        ;;
    *)
        echo "无效选择"
        exit 1
        ;;
esac

echo "========================================"