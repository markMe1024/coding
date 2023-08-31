package com.inspur.incloud.binlog.constants;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 监听配置信息
 * @author mark
 * @date 2023/7/26 10:50
 */
@Component
public class BinLogConstants {

    private String host;

    private int port;

    @Value("${binlog.datasource.url}")
    private String url;

    @Value("${binlog.datasource.username}")
    private String username;

    @Value("${binlog.datasource.password}")
    private String password;

    @Value("#{${binlog.dbTables}}")
    private Map<String, String> dbTablesMap;

    @PostConstruct
    public void splitUrl() {
        this.host = url.split(":")[0];
        this.port = Integer.parseInt(url.split(":")[1]);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, String> getDbTablesMap() {
        return dbTablesMap;
    }

    public void setDbTablesMap(Map<String, String> dbTablesMap) {
        this.dbTablesMap = dbTablesMap;
    }
}
