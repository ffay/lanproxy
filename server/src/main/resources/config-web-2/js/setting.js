var sysSettings = {
    settings: {
        "serverPort": "-1",
        "serverBind": "0.0.0.0",
        "configServerPort": "-1",
        "configServerBind": "0.0.0.0",
        "configAdminUsername": "admin",
        "artifactVersion": ""
    },
    load: function () {
        var s = this;
        layerLoading.load();
        $.ajax({
            async: false,
            url: "/info/props",
            success: function (data) {
                s.settings = data;
                layerLoading.finish()
            }
        })

    },
    render: function () {
        this.load();
        var s = this;

        for (key in s.settings) {
            $('#' + key).html(s.settings[key]);
        }
        $('#info-container').show(200);
    }
};