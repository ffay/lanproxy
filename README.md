lanproxy
--------

lanproxy是一个将局域网个人电脑、服务器代理到公网的工具，目前仅支持tcp流量转发，可支持任何tcp上层协议（ssh访问、web服务器访问、远程桌面...）。目前市面上提供类似服务的有花生壳、TeamView、GoToMyCloud等等，但天下没有免费的午餐，要使用第三方的公网服务器就必须为第三方付费，并且这些服务器都有各种各样的限制，此外，由于数据包会流经第三方，因此对数据安全也是一大隐患。

### 实现方案

![lanproxy](lanproxy.png)

### 使用

#### 编译生成运行包

-	拉取源码，运行 mvn package，打包后的资源放在distribution目录中，包括client和server

#### 配置

##### server

server的配置文件放置在conf目录中，配置 config.properties

```properties
server.bind=0.0.0.0

#与代理客户端通信端口
server.port=4900

#代理配置服务相关信息
config.server.bind=0.0.0.0
config.server.port=8090
config.admin.username=admin
config.admin.password=admin
```

代理配置，打开地址 http://ip:8090 ，使用上面配置中配置的用户名密码登录，进入如下代理配置界面

![webconfig](webconfig.png)

```js
[
  {
    "clientKey":"client_01",//一个proxy-server可以支持多个client连接，通过client_key区分
    "proxyMappings":[//支持同时配置多个代理映射
      {
        "inetPort":8080,//公网端口，本配置表示访问公网ip的8080端口将代理到内网192.168.1.5的8080端口
        "lan":"192.168.1.5:8080" // 要代理的后端服务器
      }
    ]
  }
  //支持同时配置多个client
]
```

> 配置存放在 ~/.lanproxy/config.json 文件中

##### client

client的配置文件放置在conf目录中，配置 config.properties

```
#该配置项必须与服务器端client_key对应
client.key=client_01

#proxy-server ip地址
server.host=127.0.0.1

#proxy-server 内部通信端口
server.port=4900
```

#### 运行

-	一台内网pc或服务器（运行proxy-client）；一台公网服务器（运行proxy-server）
-	安装java运行环境
-	linux（mac）环境中运行bin目录下的 startup.sh
-	windows环境中运行bin目录下的 startup.bat
