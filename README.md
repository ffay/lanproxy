# Port Bridge

Build bridges between server and LAN devices.

## Usage

JRE 1.8 required.

### 1. Server

#### Typical

```
$ java -jar server-0.5-release.jar
```

default: user=admin, pwd=admin

#### Config Properties

###### Example

```
$ java -jar server-0.5-release.jar \
        -p 4900 \
        -cp 999 \
        -u user \
        -pw qwe123 \ 
        -ssl true \
        -ssl_bind 0.0.0.0 \
        -ssl_port 4933 \
        -ssl_jks xxx.jks \
        -ssl_ks_pwd 123 \
        -ssl_km_pwd 123 \
        -l '~/log4j.properties' \
```

###### Custom

    -l	log4j.properties , Specific log4j.properties file location.
    -s	server.bind , Bind address of server.
    -p	server.port , Bind port of server.
    -cs	config.server.bind , Bind web config address of server.
    -cp	config.server.port , Bind web config port of server.
    -u	config.admin.username , Specific web config username.
    -pw	config.admin.password , Specific web config password.
    -h/--help	help , Print help information.

#### Deamonlize

```
$ nohup java -jar server-0.5-release.jar >/dev/null 2>&1 &
```

### 2. Client

#### Typical

```
$ java -jar client-0.5-release.jar \
        -h server.com \
        -k clientkey
```

#### Config Properties

###### Example

```bash
$ java -jar client-0.5-release.jar \
        -h server.com \
        -k clientkey \
        -p 888 \
        -c 1h \
        -ssl true \
        -ssl_jks xxx.jks \
        -ssl_ks_pwd 123 \
        -l '~/log4j.properties'
```

###### Custom

    -l	log4j.properties , Specific log4j.properties file location.
	-s	server.host , Server host address/ip.
	-p	server.port , Server host port.
    -c  close after, Run a while if configuared, 1d/2h/3m
	-k	client.key , Client key shows in admin web of this client.
	-h/--help	help , Print help information.

#### Deamonlize

```
$ nohup java -jar client-0.5-release.jar >/dev/null 2>&1 &
```

## Develop

### packaging

```
mvn clean package -DskipTests
```