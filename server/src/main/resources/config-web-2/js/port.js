var portInformation = {
    infoArray: [],
    toastDetail: function (name) {
        var s = this;
        var process = {ports: []};
        for (var i = 0; i < s.infoArray.length; i++) {
            var item = s.infoArray[i];
            if (item.name === name) {
                process.name = item.name;
                process.ports.push('[' + item.type + ']' + item.host + ":" + item.port);
                process.pid = item.pid;
                // process.type = item.type;
                process.information = item.information;
            }
        }
        var html = '<table style="width: 30rem;margin: 2rem">';
        html += '<tr><th>Process Name</th><td>' + process.name + '</td></tr>';
        html += '<tr><th>PID</th><td>' + process.pid + '</td></tr>';
        // html += '<tr><th>Type</th><td>' + process.type + '</td></tr>';
        html += '<tr><th>Port</th><td>' + process.ports.join('<br/> ') + '</td></tr>';
        html += '<tr><th>Other</th><td>' + process.information + '</td></tr>';
        html += '</table>';

        layer.open({
            title: process.name + " 端口占用情况",
            type: 1,
            area: ['34rem', '28rem'], //宽高
            content: html
        });
    },
    init: function () {
        this.initArray();
        this.render()
    },
    initArray: function () {
        var s = this;
        layerLoading.load();
        $.ajax({
            async: false,
            url: '/info/port',
            success: function (infoArray) {
                s.infoArray = infoArray;
                console.log(infoArray);
                setTimeout(function () {
                    layerLoading.finish();
                }, 250)
            },
            error: function () {
                setTimeout(function () {
                    layerLoading.finish();
                }, 250)
            }
        })
    },
    render: function () {
        var map = this.getMap();
        var html = '';
        for (processName in map) {
            var processList = map[processName];
            var isSelf = processName.indexOf('JUnitStarter') >= 0;

            var first = true;
            for (var index in processList) {
                var process = processList[index];
                html += '<tr class="' + (isSelf ? 'bridge' : 'other') + '">';
                if (first) {
                    first = false;
                    html += ('<td rowspan="{processListLength}">' +
                        '<h6><a href="javascript:portInformation.toastDetail(\'{processName}\')">{processName}</a></h6>' +
                        '<p>{processListLength} 个端口</p>' +
                        '<p>PID:{PID}</p>' +
                        '<p>{other}</p>' +
                        '</td>').format({
                        processListLength: processList.length,
                        processName: processName,
                        PID: process.pid,
                        other: process.information
                    })
                }

                html += '<td>' + process.type + '</td>';
                html += '<td>' + process.host + ":" + process.port + '</td>';
                html += '</tr>';
            }

        }

        var $portInfoContainer = $('#portInfoContainer');
        $portInfoContainer.hide();
        $portInfoContainer.html('<table class="centered"><thead><tr><th width=25rem>Info</th><th>type</th><th>Ports</th></tr></thead><tbody>{tableContent}</tbody></table>'.format({tableContent: html}));
        $portInfoContainer.show(500);
    },
    getMap: function () {
        var infoArray = this.infoArray;
        var map = {};
        for (var i = 0; i < infoArray.length; i++) {
            var item = infoArray[i];
            if (!map[item.name]) {
                map[item.name] = [];
            }
            map[item.name].push(item);
        }
        return map;
    }

};


var searchPort = {
    toastSearch: function () {
        var s = this;
        layer.prompt({title: '输入需要查询的端口'}, function (value, index) {
            layer.close(index);
            if (!value) {
                layer.msg('请输入端口');
                return
            }
            s.search(value);
        });
    },
    search: function (port) {
        var processName = this.findProcessByPort(port);
        if (!processName) {
            layer.msg(port + " 处于空闲状态");
        } else {
            portInformation.toastDetail(processName);
        }
    },
    portAvailable: function (port) {
        return !this.findProcessByPort(port);
    },
    findProcessByPort: function (port) {
        var infoArray = portInformation.infoArray;
        var processName = null;
        for (var i = 0; i < infoArray.length; i++) {
            var item = infoArray[i];
            if (port + '' === item.port + '') {
                processName = item.name;
                break;
            }
        }

        return processName;
    }


};