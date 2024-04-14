var proxyConfig = {
    detail: [],
    fetchDetail: function () {
        var s = this;
        layerLoading.load();
        $.ajax({
            async: false,
            url: "/config/detail",
            success: function (data) {
                s.detail = data;
                console.log('获取', s.detail.length);
                layerLoading.finish();
            }
        })
    },
    show: function () {
        var s = this;
        if (s.detail.length < 1) {
            layer.msg('没有映射，请创建客户端');
            return;
        }
        var html = '';
        for (var i = 0; i < s.detail.length; i++) {
            var client = s.detail[i];
            // 加上控制按钮
            var btns = '';
            var smallBtnHtml = '<button class="btn-small waves-effect waves-teal btn-flat" onclick="{onclickEvent}" title="{btnTitle}">{btnText}</button>';
            btns += smallBtnHtml.format({
                onclickEvent: 'proxyConfig.toastAddProxy(\'' + client.name + '\')',
                btnText: "add",
                btnTitle: '为客户端 ' + client.name + ' 添加端口映射'
            });
            btns += smallBtnHtml.format({
                onclickEvent: 'proxyConfig.toastDeleteClient(\'' + client.name + '\')',
                btnText: "delete",
                btnTitle: '删除客户端 ' + client.name
            });
            var center = '<td rowspan="{proxyCount}"><p><a href="javascript:{nameClickEvent}" title="{editClientTitle}">{clientName}</a><div style="margin-bottom: 0.8rem"><span class="new badge {badgeColor}" data-badge-caption="{onlineStatus}"></span></div><div>{btns}</div></p></td>'
                .format({
                    onlineStatus: client.status === 1 ? 'ONLINE' : 'OFFLINE',
                    badgeColor: client.status === 1 ? 'green' : 'grey',
                    proxyCount: client.proxyMappings.length > 0 ? client.proxyMappings.length : 1,
                    nameClickEvent: 'proxyConfig.toastEditClient(\'{clientName}\')',
                    editClientTitle: '修改客户端 {clientName} 名称、秘钥',
                    clientName: client.name,
                    btns: btns
                });
            if (client.proxyMappings.length === 0) {
                html += '<tr>{clientHtml}<td></td><td></td><td></td></tr>'.format({clientHtml: center})
            }
            for (var j = 0; j < client.proxyMappings.length; j++) {
                var proxyMapping = client.proxyMappings[j];
                var left = ('<td><p><a href="javascript:{proxyClickEvent}" title="{editProxyTitle}">{proxyName}</a></p></td><td><p><a href="javascript:{portClickEvent}">{outPort}</a></p></td>')
                    .format({
                        editProxyTitle: '修改映射 {proxyName}',
                        proxyClickEvent: "proxyConfig.toastEditProxy('{clientName}','{proxyName}')",
                        portClickEvent: "prompt('To copy','{hostName}:{outPort}')",
                        outPort: proxyMapping.inetPort,
                        proxyName: proxyMapping.name,
                        hostName: location.hostname,
                        clientName: client.name
                    });
                var right = ('<td><p><a href="javascript:{insideAddressClickEvent}">{insideAddress}</a><button class="right btn-small waves-effect waves-teal btn-flat" onclick="{proxyDeleteClickEvent}" title="{proxyDeleteClickTitle}"><i class="grey-text text-darken-2 material-icons">clear</i></button></p></td>')
                    .format({
                        proxyDeleteClickEvent: "proxyConfig.toastDeleteProxy('{clientName}','{proxyName}')",
                        proxyDeleteClickTitle: '删除映射 {proxyName}',
                        insideAddressClickEvent: "prompt('To copy','{insideAddress}')",
                        proxyName: proxyMapping.name,
                        insideAddress: proxyMapping.lan,
                        clientName: client.name
                    });

                if (j === 0) {
                    html += ('<tr>' + center + left + right + '</tr>');
                } else {
                    html += ('<tr>' + left + right + '</tr>');
                }
            }
        }
        var $proxyConfigContainer = $('#proxyConfigContainer');
        $proxyConfigContainer.hide();
        $proxyConfigContainer.html('<table class="centered"><thead><tr><th>客户端</th><th>名称</th><th>外部端口</th><th>内部地址</th></tr></thead><tbody>{tableContent}</tbody></table>'.format({tableContent: html}));
        $proxyConfigContainer.show(500);
    },
    render: function () {
        this.fetchDetail();
        this.show();
    },
    generateUUID: function () {
        return 'xxxxxxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            var r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    },
    toastAddClient: function () {
        var s = this;
        layer.open({
            type: 1,
            area: ['320px', '280px'], //宽高
            content:
                ('<div style="padding: 20px;padding-bottom: 0">' +
                    '<input placeholder="客户端名称" id="addClientName" class="layui-layer-input" value="">' +
                    '<input placeholder="客户端身份秘钥" id="addClientKey" class="layui-layer-input" value="{generatedUUID}">' +
                    '</div>' +
                    '<div class="layui-layer-btn layui-layer-btn-">' +
                    '<a href="javascript:{confirmEvent}" class="layui-layer-btn0">确定</a>' +
                    '<a class="layui-layer-btn1">取消</a>' +
                    '</div>')
                    .format({
                        generatedUUID: s.generateUUID(),
                        confirmEvent: "proxyConfig.addClient($('#addClientName').val(),$('#addClientKey').val())"
                    })
        });
    },
    addClient: function (clientName, clientKey) {
        if (!clientName) {
            layer.msg('添加失败 客户端名称不可为空');
            return
        }
        if (!clientKey) {
            layer.msg('添加失败 客户端身份秘钥不可为空');
            return
        }
        var client = this.findByName(clientName);

        if (client) {
            layer.msg('添加失败 客户端 ' + clientName + " 已存在");
            return;
        }

        this.detail.push({
            "name": clientName,
            "clientKey": clientKey,
            "status": 0,
            "proxyMappings": []
        });
        layer.msg('添加客户端 ' + clientName + ' 成功');
        this.show();
        this.save();
    },
    toastDeleteClient: function (clientName) {
        var s = this;
        layer.confirm('删除客户端 {clientName} ？'.format({clientName: clientName}), {
            btn: ['确定', '取消']
        }, function () {
            s.deleteClient(clientName);
        });
    },
    deleteClient: function (clientName) {
        var s = this;
        var newClients = [];
        for (var i = 0; i < s.detail.length; i++) {
            var client = s.detail[i];
            if (client.name === clientName) {
                continue;
            }
            newClients.push(client);
        }
        if (newClients.length < s.detail.length) {
            s.detail = newClients;
            layer.msg('删除客户端 ' + clientName + ' 成功');
            s.save();
            s.show();
        } else {
            layer.msg('删除客户端 ' + clientName + ' 失败，请刷新后重试');
        }
    },
    toastEditClient: function (clientName) {
        // find client
        var s = this;
        var client = s.findByName(clientName);

        if (!client) {
            layer.msg("客户端 {cn} 不存在".format({cn: clientName}));
            return;
        }

        layer.open({
            type: 1,
            title: '客户端 ' + clientName,
            area: ['320px', '280px'],
            content:
                ('<div style="padding: 20px;padding-bottom: 0">' +
                    '<input placeholder="客户端名称" id="editClientName" class="layui-layer-input" value="{cn}">' +
                    '<input placeholder="客户端身份秘钥" id="editClientKey" class="layui-layer-input" value="{generatedUUID}">' +
                    '</div>' +
                    '<div class="layui-layer-btn layui-layer-btn-">' +
                    '<a href="javascript:{cmdEvent}" class="layui-layer-btn1">查看指令</a>' +
                    '<a href="javascript:{confirmEvent}" class="layui-layer-btn0">确定</a>' +
                    '<a class="layui-layer-btn1">取消</a>' +
                    '</div>')
                    .format({
                        cmdEvent: "prompt('客户端 {cn} 运行命令', 'java -jar client{version}-release.jar -s {serverHost} -p {serverPort} -k {generatedUUID} -c 8h')",
                        confirmEvent: "proxyConfig.editClient('{cn}', $('#editClientName').val(),$('#editClientKey').val())",
                        generatedUUID: client.clientKey,
                        cn: clientName,
                        serverHost: location.hostname,
                        serverPort: sysSettings.settings.serverPort,
                        version: sysSettings.settings.artifactVersion
                    })
        });
    },
    editClient: function (clientName, newClientName, newClientKey) {
        var s = this;
        var client = s.findByName(clientName);
        if (!client) {
            layer.msg("客户端 {cn} 不存在".format({cn: clientName}));
            return;
        }

        if (clientName === newClientName && newClientKey === client.clientKey) {
            layer.msg("客户端 {cn} 没有更改".format({cn: clientName}));
            return;
        }

        client.name = newClientName;
        client.clientKey = newClientKey;
        layer.msg('修改客户端 {cn} 成功'.format({cn: newClientName}));
        s.show();
        s.save();
    },
    findByName: function (clientName) {
        var s = this;
        var client;
        for (var i = 0; i < s.detail.length; i++) {
            if (clientName === s.detail[i].name) {
                client = s.detail[i];
                break;
            }
        }
        return client;
    },
    findProxyByName: function (clientName, proxyName) {
        var client = this.findByName(clientName);
        if (!client) {
            return
        }

        if (!client.proxyMappings) {
            return
        }

        for (var j = 0; j < client.proxyMappings.length; j++) {
            var proxyMapping = client.proxyMappings[j];
            if (proxyMapping.name === proxyName) {
                return proxyMapping;
            }
        }
    },
    toastAddProxy: function (clientName) {
        // 获取建议端口
        // 规则 当前客户端最大端口 + 1
        // 如果为空 所有客户端当中最大端口 + 1
        var s = this;

        var maxClientPort = -1;
        var maxPort = -1;

        for (var i = 0; i < s.detail.length; i++) {
            var client = s.detail[i];
            if (client.proxyMappings && client.proxyMappings.length > 0) {
                for (var j = 0; j < client.proxyMappings.length; j++) {
                    var proxyMapping = client.proxyMappings[j];
                    if (proxyMapping.inetPort > maxPort) {
                        maxPort = proxyMapping.inetPort;
                    }
                    if (clientName === client.name && proxyMapping.inetPort > maxClientPort) {
                        maxClientPort = proxyMapping.inetPort;
                    }
                }
            }
        }
        var suggestPort = 30000;
        if (maxPort > 0) {
            suggestPort = maxPort;
        }
        if (maxClientPort > 0) {
            suggestPort = maxClientPort;
        }

        layer.open({
            type: 1,
            title: clientName + ' 添加端口映射',
            area: ['320px', '380px'],
            content:
                ('<div style="padding: 20px;padding-bottom: 0">' +
                    '<input placeholder="名称" id="addProxyName" class="layui-layer-input" value="">' +
                    '<input placeholder="内部地址 127.0.0.1:8080" id="addProxyInsideAddress" class="layui-layer-input" value="">' +
                    '<input placeholder="外部端口 1-65535" id="addProxyPort" class="layui-layer-input" value="{suggestPort}">' +
                    '</div>' +
                    '<div class="layui-layer-btn layui-layer-btn-">' +
                    '<a href="javascript:{confirmEvent}" class="layui-layer-btn0">确定</a>' +
                    '<a class="layui-layer-btn1">取消</a>' +
                    '</div>')
                    .format({
                        confirmEvent: "proxyConfig.addProxy('{cn}', $('#addProxyName').val(),$('#addProxyInsideAddress').val(),$('#addProxyPort').val())",
                        suggestPort: suggestPort + 1,
                        cn: clientName
                    })
        });
    },
    addProxy: function (clientName, proxyName, insideAddress, proxyPort) {
        var s = this;
        // 检查客户端是否存在
        var client = s.findByName(clientName);
        if (!client) {
            layer.msg("添加失败 客户端 {cn} 不存在".format({cn: clientName}));
            return;
        }

        // 检查名称是否重复
        if (!client.proxyMappings) {
            client.proxyMappings = [];
        }
        for (var j = 0; j < client.proxyMappings.length; j++) {
            var proxyMapping = client.proxyMappings[j];
            if (proxyMapping.name === proxyName) {
                layer.msg("添加失败 映射名称 {pn} 重复".format({pn: proxyName}));
                return
            }
        }

        // 检查内部地址不能为空
        if (!insideAddress) {
            layer.msg("添加失败 内部地址不能为空");
            return
        }
        if (!/\S+:[\d]+/.test(insideAddress)) {
            layer.msg("添加失败 内部地址格式：127.0.0.1:8080");
            return
        }

        // 检查端口是否可用
        if (!searchPort.portAvailable(proxyPort)) {
            layer.msg("添加失败 端口 {port} 已被占用".format({port: proxyPort}));
            return
        }

        //保存
        client.proxyMappings.push({
            "inetPort": proxyPort,
            "lan": insideAddress,
            "name": proxyName
        });
        layer.msg("添加 " + proxyName + " 成功 端口号：" + proxyPort);

        //  完成之后要刷新已经占用的端口缓存
        s.show();
        s.save();
        portInformation.initArray();
    },
    toastEditProxy: function (clientName, proxyName) {
        var proxy = this.findProxyByName(clientName, proxyName);
        if (!proxy) {
            layer.msg("{cn}.{pn} 不存在".format({cn: clientName, pn: proxyName}));
            return
        }

        layer.open({
            type: 1,
            title: "修改 {cn}.{pn}".format({cn: clientName, pn: proxyName}),
            area: ['320px', '380px'],
            content:
                ('<div style="padding: 20px;padding-bottom: 0">' +
                    '<input placeholder="名称" id="editProxyName" class="layui-layer-input" value="{editProxyName}">' +
                    '<input placeholder="内部地址 127.0.0.1:8080" id="editProxyInsideAddress" class="layui-layer-input" value="{editProxyInsideAddress}">' +
                    '<input placeholder="外部端口 1-65535" id="editProxyPort" class="layui-layer-input" value="{editProxyPort}">' +
                    '</div>' +
                    '<div class="layui-layer-btn layui-layer-btn-">' +
                    '<a href="javascript:{confirmEvent}" class="layui-layer-btn0">确定</a>' +
                    '<a class="layui-layer-btn1">取消</a>' +
                    '</div>')
                    .format({
                        confirmEvent: "proxyConfig.editProxy('{cn}','{editProxyName}', $('#editProxyName').val(),$('#editProxyInsideAddress').val(),$('#editProxyPort').val())",
                        editProxyPort: proxy.inetPort,
                        editProxyInsideAddress: proxy.lan,
                        editProxyName: proxy.name,
                        cn: clientName
                    })
        });
    },
    editProxy: function (clientName, proxyName, newProxyName, newProxyLan, newProxyPort) {
        var proxy = this.findProxyByName(clientName, proxyName);
        if (!proxy) {
            layer.msg("{cn}.{pn} 不存在".format({cn: clientName, pn: proxyName}));
            return
        }

        var client = this.findByName(clientName);
        // 检查名称是否重复
        if (!client.proxyMappings) {
            client.proxyMappings = [];
        }
        for (var j = 0; j < client.proxyMappings.length; j++) {
            var proxyMapping = client.proxyMappings[j];
            if (newProxyName !== proxy.name && proxyMapping.name === newProxyName) {
                layer.msg("添加失败 映射名称 {pn} 重复".format({pn: newProxyName}));
                return
            }
        }

        // 检查内部地址不能为空
        if (!newProxyLan) {
            layer.msg("添加失败 内部地址不能为空");
            return
        }
        if (!/\S+:[\d]+/.test(newProxyLan)) {
            layer.msg("添加失败 内部地址格式：127.0.0.1:8080");
            return
        }

        // 检查端口是否可用
        if (parseInt(newProxyPort) !== proxy.inetPort && !searchPort.portAvailable(newProxyPort)) {
            layer.msg("添加失败 端口 {port} 已被占用".format({port: newProxyPort}));
            return
        }

        proxy.lan = newProxyLan;
        proxy.name = newProxyName;
        proxy.inetPort = parseInt(newProxyPort);

        layer.msg("修改 {pn} 成功".format({pn: newProxyName}));
        this.show();
        this.save();
    },
    toastDeleteProxy: function (clientName, proxyName) {
        var s = this;
        layer.confirm('删除映射关系 {clientName}.{proxyName} ？'.format({clientName: clientName, proxyName: proxyName}), {
            btn: ['确定', '取消']
        }, function () {
            s.deleteProxy(clientName, proxyName);
        });
    },
    deleteProxy: function (clientName, proxyName) {
        var s = this;
        var proxy = this.findProxyByName(clientName, proxyName);
        if (!proxy) {
            layer.msg("{cn}.{pn} 不存在".format({cn: clientName, pn: proxyName}));
            return
        }

        var client = s.findByName(clientName);

        var newProxyList = [];
        for (var j = 0; j < client.proxyMappings.length; j++) {
            var proxyMapping = client.proxyMappings[j];
            if (proxyMapping.name !== proxyName) {
                newProxyList.push(proxyMapping);
            }
        }

        if (newProxyList.length === client.proxyMappings.length) {
            layer.msg("删除 {cn}.{pn} 失败".format({cn: clientName, pn: proxyName}));
            return;
        }

        // 删除成功
        layer.msg("删除 {cn}.{pn} 成功".format({cn: clientName, pn: proxyName}));
        client.proxyMappings = newProxyList;
        s.show();
        s.save();
    },
    save: function () {
        var s = this;
        $.ajax({
            type: 'post',
            url: "/config/update",
            data: JSON.stringify(s.detail),
            contentType: 'application/json',
            success: function (result) {
                console.log(result);
            }
        })
    }
};