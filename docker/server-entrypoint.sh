#!/bin/bash

# 设置配置文件路径
CONF_FILE="/app/conf/config.properties"

# 创建配置文件
cat > $CONF_FILE << EOF
server.bind=${SERVER_BIND:-0.0.0.0}
server.port=${SERVER_PORT:-4900}

server.ssl.enable=${SSL_ENABLE:-true}
server.ssl.bind=${SERVER_BIND:-0.0.0.0}
server.ssl.port=${SSL_PORT:-4993}
server.ssl.jksPath=test.jks
server.ssl.keyStorePassword=123456
server.ssl.keyManagerPassword=123456
server.ssl.needsClientAuth=false

config.server.bind=${CONFIG_SERVER_BIND:-0.0.0.0}
config.server.port=${CONFIG_SERVER_PORT:-8090}
config.admin.username=${CONFIG_ADMIN_USERNAME:-admin}
config.admin.password=${CONFIG_ADMIN_PASSWORD:-admin}
EOF

echo "Configuration file created:"
cat $CONF_FILE

# 构建classpath
LIB_JARS=$(find /app/lib -name "*.jar" | tr '\n' ':')
CLASSPATH="/app/conf:$LIB_JARS"

echo "Starting lanproxy server..."
echo "JAVA_OPTS: $JAVA_OPTS"
echo "CLASSPATH: $CLASSPATH"

# 启动应用
exec java $JAVA_OPTS -Dapp.home=/app -classpath "$CLASSPATH" org.fengfei.lanproxy.server.ProxyServerContainer