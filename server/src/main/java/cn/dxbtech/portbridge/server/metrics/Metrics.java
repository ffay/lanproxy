package cn.dxbtech.portbridge.server.metrics;

import java.io.Serializable;

public class Metrics implements Serializable {

    private static final long serialVersionUID = 1L;

    private int port;

    private long readBytes;

    private long wroteBytes;

    private long readMsgs;

    private long wroteMsgs;

    private int channels;

    private long timestamp;

    public long getReadBytes() {
        return readBytes;
    }

    public void setReadBytes(long readBytes) {
        this.readBytes = readBytes;
    }

    public long getWroteBytes() {
        return wroteBytes;
    }

    public void setWroteBytes(long wroteBytes) {
        this.wroteBytes = wroteBytes;
    }

    public long getReadMsgs() {
        return readMsgs;
    }

    public void setReadMsgs(long readMsgs) {
        this.readMsgs = readMsgs;
    }

    public long getWroteMsgs() {
        return wroteMsgs;
    }

    public void setWroteMsgs(long wroteMsgs) {
        this.wroteMsgs = wroteMsgs;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Metrics metrics = (Metrics) o;

        if (port != metrics.port) return false;
        if (readBytes != metrics.readBytes) return false;
        if (wroteBytes != metrics.wroteBytes) return false;
        if (readMsgs != metrics.readMsgs) return false;
        if (wroteMsgs != metrics.wroteMsgs) return false;
        return channels == metrics.channels;
    }

    @Override
    public int hashCode() {
        int result = port;
        result = 31 * result + (int) (readBytes ^ (readBytes >>> 32));
        result = 31 * result + (int) (wroteBytes ^ (wroteBytes >>> 32));
        result = 31 * result + (int) (readMsgs ^ (readMsgs >>> 32));
        result = 31 * result + (int) (wroteMsgs ^ (wroteMsgs >>> 32));
        result = 31 * result + channels;
        return result;
    }

    @Override
    public String toString() {
        return "Metrics{" + port + '}';
    }
}