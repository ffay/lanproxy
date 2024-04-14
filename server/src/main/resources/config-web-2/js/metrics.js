var metricsData = {
    metrics: {},
    load: function (async, start) {
        var s = this;
        $.ajax({
            url: '/metrics/list',
            data: start ? {start: start} : {},
            async: !!async,
            success: function (data) {
                s.metrics = data;

                s.chartData.readMsgs = s.toChartData('readMsgs');
                s.chartData.wroteMsgs = s.toChartData('wroteMsgs');
                s.chartData.channels = s.toChartData('channels');
                s.chartData.readBytes = s.toChartData('readBytes');
                s.chartData.wroteBytes = s.toChartData('wroteBytes');
            }
        })
    },
    render: function () {
        var s = this;
        s.load();
        setTimeout(function () {
            s.initCharts();
        }, 500)
    },
    chartData: {},
    toChartData: function (keyName) {
        var s = this;

        var datas = [];
        for (var i = 0; i < s.metrics.length; i++) {
            var proxy = s.metrics[i];

            var xys = [];
            for (var j = 0; j < proxy.metrics.length; j++) {
                var me = proxy.metrics[j];
                xys.push({
                    value: [(me.timestamp), me[keyName]],
                    name: new Date(me.timestamp)
                })
            }

            datas.push({
                name: proxy.client + "." + proxy.proxy,
                data: xys,
                type: 'line',
                smooth: true
            })
        }
        return datas
    },
    initCharts: function () {
        var s = this;
        var c1 = s.createCharts('readMsgs', s.chartData['readMsgs'], '个', '请求数据包数');
        var c2 = s.createCharts('wroteMsgs', s.chartData['wroteMsgs'], '个', '响应数据包数');
        var c3 = s.createCharts('channels', s.chartData['channels'], '个', '连接数');
        var c4 = s.createCharts('readBytes', s.chartData['readBytes'], 'B', '请求数据量');
        var c5 = s.createCharts('wroteBytes', s.chartData['wroteBytes'], 'B', '响应数据量');

        setInterval(function () {
            s.load();

            c1.setOption({
                series: s.chartData['readMsgs']
            });
            c2.setOption({
                series: s.chartData['wroteMsgs']
            });
            c3.setOption({
                series: s.chartData['channels']
            });
            c4.setOption({
                series: s.chartData['readBytes']
            });
            c5.setOption({
                series: s.chartData['wroteBytes']
            });
        }, 1000)


    },
    createCharts: function (name, data, suffix, title) {
        var s = this;

        var legend = [];
        for (var i = 0; i < data.length; i++) {
            legend.push(data[i].name)
        }

        var myChart = echarts.init(document.getElementById(name));
        var option = {
            title: {
                text: title
            },
            tooltip: {
                trigger: 'axis',
                formatter: function (params) {
                    return new Date(params[0].value[0]).format("yyyy-MM-dd hh:mm:ss")
                        + "<br>"
                        + params[0].seriesName + " : "
                        + params[0].value[1] + " " + suffix;
                },
                axisPointer: {
                    animation: false
                }
            },
            legend: {
                data: legend
            },
            xAxis: {
                type: 'time',
                splitLine: {
                    show: false
                },
                axisLabel: {
                    formatter: function (val) {
                        return new Date(val).format("hh:mm:ss");
                    }
                }
            },
            yAxis: {
                type: 'value',
                boundaryGap: [0, '100%'],
                splitLine: {
                    show: false
                },
                axisLabel: {
                    formatter: function (val) {
                        return val + ' ' + suffix;
                    }
                }

            },
            series: data
        };
        myChart.setOption(option);
        return myChart
    }
};

