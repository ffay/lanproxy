String.prototype.format = function () {
    var str = this.toString();
    if (!arguments.length)
        return str;
    var args = typeof arguments[0],
        args = (("string" == args || "number" == args) ? arguments : arguments[0]);
    for (arg in args)
        str = str.replace(RegExp("\\{" + arg + "\\}", "gi"), args[arg]);
    return str;
};

Date.prototype.format = function (fmt) {
    var o = {
        "M+": this.getMonth() + 1,                 //月份
        "d+": this.getDate(),                    //日
        "h+": this.getHours(),                   //小时
        "m+": this.getMinutes(),                 //分
        "s+": this.getSeconds(),                 //秒
        "q+": Math.floor((this.getMonth() + 3) / 3), //季度
        "S": this.getMilliseconds()             //毫秒
    };
    if (/(y+)/.test(fmt)) {
        fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    }
    for (var k in o) {
        if (new RegExp("(" + k + ")").test(fmt)) {
            fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
        }
    }
    return fmt;
}

var layerLoading = {
    load: function () {
        this.loadingCount++;
        this.refresh()
    },
    finish: function () {
        this.loadingCount--;
        this.refresh()
    },
    refresh: function () {
        var s = this;
        if (s.loadingCount > 0) {
            if (s.loadingIndex === -1) {
                s.loadingIndex = layer.load(2);
            }
        } else {
            if (s.loadingIndex !== -1) {
                layer.close(s.loadingIndex);
            }
            s.loadingIndex = -1
        }
    },
    loadingCount: 0,
    loadingIndex: -1
};

