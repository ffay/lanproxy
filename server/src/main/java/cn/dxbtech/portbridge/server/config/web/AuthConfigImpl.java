package cn.dxbtech.portbridge.server.config.web;

import cn.dxbtech.portbridge.server.config.ProxyConfig;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaders.Names.AUTHORIZATION;

public class AuthConfigImpl implements HttpRequestHandler.AuthConfig {

    private final String configAuth;
    private final List<String> logoutUrls;

    public AuthConfigImpl() {
        logoutUrls = Arrays.asList("/logout", "logout");
        String configAdminUsername = ProxyConfig.getInstance().getConfigAdminUsername();
        String configAdminPassword = ProxyConfig.getInstance().getConfigAdminPassword();
        configAuth = "Basic " + new String(Base64.getEncoder().encode((configAdminUsername + ":" + configAdminPassword).getBytes()));
    }

    @Override
    public boolean logout(String url) {
        return logoutUrls.contains(url);
    }

    @Override
    public boolean valid(FullHttpRequest request) {
        String auth = request.headers().get(AUTHORIZATION);
        if (!StringUtil.isNullOrEmpty(auth)) {
            if (configAuth.equals(auth)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String errorInfo() {
        return "Auth required";
    }
}
