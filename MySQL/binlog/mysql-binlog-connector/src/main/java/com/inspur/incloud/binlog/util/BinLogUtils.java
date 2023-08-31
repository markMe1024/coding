package com.inspur.incloud.binlog.util;

import com.github.shyiko.mysql.binlog.event.EventType;
import com.inspur.incloud.binlog.constants.BinLogConstants;
import com.inspur.incloud.binlog.model.BinLogItem;
import com.inspur.incloud.binlog.model.DbColumn;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.github.shyiko.mysql.binlog.event.EventType.isDelete;
import static com.github.shyiko.mysql.binlog.event.EventType.isUpdate;
import static com.github.shyiko.mysql.binlog.event.EventType.isWrite;

/**
 * Binlog工具类
 * @author mark
 * @date 2023/7/26 10:50
 **/
public class BinLogUtils {

    /**
     * 拼接dbTable
     */
    public static String getDbTable(String db, String table) {
        return db + "-" + table;
    }

    /**
     * 获取columns集合
     */
    public static Map<String, DbColumn> getColMap(BinLogConstants binLogConstants, String db, String table) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        // 保存当前注册的表的column信息
        Connection connection = DriverManager.getConnection(
                "jdbc:mysql://" + binLogConstants.getHost() + ":" + binLogConstants.getPort(), binLogConstants.getUsername(), binLogConstants.getPassword());
        // 执行sql
        String preSql = "SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, DATA_TYPE, ORDINAL_POSITION FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? and TABLE_NAME = ?";
        PreparedStatement ps = connection.prepareStatement(preSql);
        ps.setString(1, db);
        ps.setString(2, table);
        ResultSet rs = ps.executeQuery();
        Map<String, DbColumn> map = new HashMap<>(rs.getRow());
        while (rs.next()) {
            String schema = rs.getString("TABLE_SCHEMA");
            String tableName = rs.getString("TABLE_NAME");
            String column = rs.getString("COLUMN_NAME");
            int idx = rs.getInt("ORDINAL_POSITION");
            String dataType = rs.getString("DATA_TYPE");
            if (column != null && idx >= 1) {
                map.put(column, new DbColumn(schema, tableName, idx - 1, column, dataType)); // sql的位置从1开始
            }
        }
        ps.close();
        rs.close();
        return map;
    }

    /**
     * 根据DBTable获取table
     */
    public static String getTable(String dbTable) {
        if (dbTable == null || "".equals(dbTable)) {
            return "";
        }
        String[] split = dbTable.split("-");
        if (split.length == 2) {
            return split[1];
        }
        return "";
    }

    /**
     * 根据操作类型获取对应集合
     */
    public static Map<String, Serializable> getOptMap(BinLogItem binLogItem) {
        // 获取操作类型
        EventType eventType = binLogItem.getEventType();
        if (isWrite(eventType) || isUpdate(eventType)) {
            return binLogItem.getAfter();
        }
        if (isDelete(eventType)) {
            return binLogItem.getBefore();
        }
        return null;
    }

}