package com.inspur.incloud.binlog.model;

import com.github.shyiko.mysql.binlog.event.EventType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * BinLog解析后数据条目
 * @author mark
 * @date 2023/7/26 10:56
 */
public class BinLogItem implements Serializable {

    private static final long serialVersionUID = -522609091805092787L;

    // 数据库-表，形如：iresource-t_net_net
    private String dbTable;

    // 事件类型
    private EventType eventType;

    // 存储字段-之前的值之后的值
    private Map<String, Serializable> before = null;
    private Map<String, Serializable> after = null;

    /**
     * 新增或者删除操作数据格式化
     */
    public static BinLogItem itemFromInsertOrDeleted(Serializable[] row, Map<String, DbColumn> columnMap, EventType eventType) {
        BinLogItem item = new BinLogItem();
        item.eventType = eventType;

        item.before = new HashMap<>();
        item.after = new HashMap<>();

        Map<String, Serializable> beOrAf = new HashMap<>();
        columnMap.forEach((key, column) -> beOrAf.put(key, row[column.inx]));

        // 写操作放after，删操作放before
        if (EventType.isWrite(eventType)) {
            item.after = beOrAf;
        }
        if (EventType.isDelete(eventType)) {
            item.before = beOrAf;
        }

        return item;
    }

    /**
     * 更新操作数据格式化
     */
    public static BinLogItem itemFromUpdate(Map.Entry<Serializable[], Serializable[]> row,
                                            Map<String, DbColumn> columnMap, EventType eventType) {
        BinLogItem item = new BinLogItem();
        item.eventType = eventType;

        Map<String, Serializable> be = new HashMap<>();
        Map<String, Serializable> af = new HashMap<>();
        columnMap.forEach((key, dbColumn) -> {
            be.put(key, row.getKey()[dbColumn.inx]);
            af.put(key, row.getValue()[dbColumn.inx]);
        });
        item.before = be;
        item.after = af;

        return item;
    }

    public String getDbTable() {
        return dbTable;
    }

    public void setDbTable(String dbTable) {
        this.dbTable = dbTable;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Map<String, Serializable> getBefore() {
        return before;
    }

    public void setBefore(Map<String, Serializable> before) {
        this.before = before;
    }

    public Map<String, Serializable> getAfter() {
        return after;
    }

    public void setAfter(Map<String, Serializable> after) {
        this.after = after;
    }
}
