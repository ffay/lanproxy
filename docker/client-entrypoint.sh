#!/bin/bash

# 设置配置文件路径
CONF_FILE="/app/conf/config.properties"

# 检查必需的环境变量
if [ -z "$CLIENT_KEY" ]; then
    echo "ERROR: CLIENT_KEY environment variable is required"
    echo "Please set CLIENT_KEY to the client key from proxy server"
    exit 1
fi

if [ -z "$SERVER_HOST" ]; then
    echo "ERROR: SERVER_HOST environment variable is required"
    echo "Please set SERVER_HOST to the proxy server hostname or IP"
    exit 1
fi

# 创建配置文件
cat > $CONF_FILE << EOF
client.key=${CLIENT_KEY}
ssl.enable=${SSL_ENABLE:-false}
ssl.jksPath=test.jks
ssl.keyStorePassword=123456

server.host=${SERVER_HOST}
server.port=${SERVER_PORT:-4900}
EOF

echo "Configuration file created:"
cat $CONF_FILE

# 构建classpath
LIB_JARS=$(find /app/lib -name "*.jar" | tr '\n' ':')
CLASSPATH="/app/conf:$LIB_JARS"

echo "Starting lanproxy client..."
echo "JAVA_OPTS: $JAVA_OPTS"
echo "CLASSPATH: $CLASSPATH"
echo "Connecting to server: $SERVER_HOST:$SERVER_PORT"

# 启动应用
exec java $JAVA_OPTS -Dapp.home=/app -classpath "$CLASSPATH" org.fengfei.lanproxy.client.ProxyClientContainer