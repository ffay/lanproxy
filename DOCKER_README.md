# LanProxy 容器化部署指南

本文档介绍如何使用Docker和Docker Compose部署LanProxy内网穿透工具。

## 📋 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- Maven 3.6+ (用于构建)
- JDK 8+ (用于构建)

## 🚀 快速开始

### 1. 构建镜像

```bash
# 使用构建脚本（推荐）
./docker/build.sh

# 或者手动构建
mvn clean package -DskipTests
docker build -f Dockerfile.server -t lanproxy-server:latest .
docker build -f Dockerfile.client -t lanproxy-client:latest .
```

### 2. 部署服务

```bash
# 使用部署脚本（推荐）
./docker/deploy.sh

# 或者手动部署
# 仅启动服务器
docker-compose up -d proxy-server

# 启动服务器和客户端
docker-compose --profile client up -d
```

### 3. 访问管理界面

打开浏览器访问：http://localhost:8090

默认账号：`admin` / `admin`

## 📁 项目结构

```
lanproxy/
├── docker/
│   ├── build.sh              # 构建脚本
│   ├── deploy.sh             # 部署脚本
│   ├── server-entrypoint.sh  # 服务器启动脚本
│   └── client-entrypoint.sh  # 客户端启动脚本
├── Dockerfile.server         # 服务器镜像构建文件
├── Dockerfile.client         # 客户端镜像构建文件
├── docker-compose.yml        # Docker Compose配置
├── .env.example             # 环境变量配置示例
└── DOCKER_README.md         # 本文档
```

## ⚙️ 配置说明

### 环境变量配置

复制 `.env.example` 为 `.env` 并根据实际情况修改：

```bash
cp .env.example .env
```

主要配置项：

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SERVER_BIND` | 服务器绑定地址 | `0.0.0.0` |
| `SERVER_PORT` | 代理服务端口 | `4900` |
| `SSL_ENABLE` | 是否启用SSL | `true` |
| `SSL_PORT` | SSL端口 | `4993` |
| `CONFIG_SERVER_PORT` | Web管理端口 | `8090` |
| `CONFIG_ADMIN_USERNAME` | 管理员用户名 | `admin` |
| `CONFIG_ADMIN_PASSWORD` | 管理员密码 | `admin` |
| `CLIENT_KEY` | 客户端密钥 | 需要从服务器获取 |
| `SERVER_HOST` | 服务器地址 | `proxy-server` |

### 端口映射

默认映射的端口：

- `4900`: 代理服务器与客户端通信端口
- `4993`: SSL端口
- `8090`: Web管理界面端口
- `8080-8082`: 示例用户端口（可根据需要修改）

## 🔧 使用方法

### 1. 启动服务器

```bash
# 仅启动服务器
docker-compose up -d proxy-server
```

### 2. 配置客户端

1. 访问Web管理界面：http://localhost:8090
2. 使用默认账号登录：`admin` / `admin`
3. 在客户端管理页面添加新客户端
4. 复制生成的客户端密钥
5. 修改 `.env` 文件中的 `CLIENT_KEY`

### 3. 启动客户端

```bash
# 启动客户端（需要先配置CLIENT_KEY）
docker-compose --profile client up -d
```

### 4. 配置代理规则

在Web管理界面中：
1. 选择对应的客户端
2. 添加代理配置
3. 设置外网端口和内网地址

## 📊 监控和日志

### 查看服务状态

```bash
docker-compose ps
```

### 查看日志

```bash
# 查看服务器日志
docker-compose logs -f proxy-server

# 查看客户端日志
docker-compose logs -f proxy-client

# 查看所有服务日志
docker-compose logs -f
```

### 日志文件位置

- 服务器日志：`./data/server-logs/`
- 客户端日志：`./data/client-logs/`
- 服务器配置：`./data/server-config/`

## 🛠️ 常用命令

```bash
# 停止所有服务
docker-compose --profile client down

# 重启服务
docker-compose restart proxy-server
docker-compose restart proxy-client

# 更新镜像
./docker/build.sh
docker-compose --profile client up -d

# 清理数据
docker-compose --profile client down -v
sudo rm -rf data/
```

## 🔍 故障排除

### 常见问题

1. **客户端连接失败**
   - 检查 `CLIENT_KEY` 是否正确
   - 检查 `SERVER_HOST` 是否可达
   - 查看客户端日志：`docker-compose logs proxy-client`

2. **端口冲突**
   - 修改 `docker-compose.yml` 中的端口映射
   - 确保宿主机端口未被占用

3. **Web界面无法访问**
   - 检查防火墙设置
   - 确认端口8090未被占用
   - 查看服务器日志：`docker-compose logs proxy-server`

4. **内存不足**
   - 调整 `.env` 文件中的 `JAVA_OPTS` 参数
   - 增加Docker可用内存

### 调试模式

启用调试模式：

```bash
# 修改环境变量
SERVER_JAVA_OPTS="-Xms256m -Xmx512m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
CLIENT_JAVA_OPTS="-Xms128m -Xmx256m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006"

# 添加调试端口映射到docker-compose.yml
ports:
  - "5005:5005"  # 服务器调试端口
  - "5006:5006"  # 客户端调试端口
```

## 📝 注意事项

1. **安全性**
   - 修改默认的管理员密码
   - 在生产环境中禁用不必要的端口
   - 使用SSL加密连接

2. **性能优化**
   - 根据实际负载调整JVM参数
   - 监控容器资源使用情况
   - 定期清理日志文件

3. **数据备份**
   - 定期备份 `./data/server-config/` 目录
   - 备份客户端配置信息

## 🤝 贡献

欢迎提交Issue和Pull Request来改进容器化部署方案。

## 📄 许可证

本项目遵循原项目的许可证。