package cn.dxbtech.portbridge.server.info.port;

/**
 * Created by dxb on 2018/4/30.
 */
public class PortInfo {
    private String name = "unknown";
    private String host;
    private Integer port;
    private String pid;
    private String information = "";
    private String type = "TCP";

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PortInfo portInfo = (PortInfo) o;

        if (name != null ? !name.equals(portInfo.name) : portInfo.name != null) return false;
        if (host != null ? !host.equals(portInfo.host) : portInfo.host != null) return false;
        if (port != null ? !port.equals(portInfo.port) : portInfo.port != null) return false;
        if (pid != null ? !pid.equals(portInfo.pid) : portInfo.pid != null) return false;
        if (information != null ? !information.equals(portInfo.information) : portInfo.information != null)
            return false;
        return type != null ? type.equals(portInfo.type) : portInfo.type == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + (port != null ? port.hashCode() : 0);
        result = 31 * result + (pid != null ? pid.hashCode() : 0);
        result = 31 * result + (information != null ? information.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
