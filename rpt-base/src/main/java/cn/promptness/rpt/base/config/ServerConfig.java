package cn.promptness.rpt.base.config;

import java.util.List;

public class ServerConfig {

    private String serverIp;
    private int serverPort;
    private int httpPort;
    private int httpsPort;
    private String domainCert;
    private String domainKey;
    private List<String> clientKey;

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public List<String> getClientKey() {
        return clientKey;
    }

    public void setClientKey(List<String> clientKey) {
        this.clientKey = clientKey;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public String getDomainCert() {
        return domainCert;
    }

    public void setDomainCert(String domainCert) {
        this.domainCert = domainCert;
    }

    public String getDomainKey() {
        return domainKey;
    }

    public void setDomainKey(String domainKey) {
        this.domainKey = domainKey;
    }
}
