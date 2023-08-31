package com.inspur.incloud.binlog.listener;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.NullEventDataDeserializer;
import com.inspur.incloud.binlog.constants.BinLogConstants;
import com.inspur.incloud.binlog.model.BinLogItem;
import com.inspur.incloud.binlog.model.DbColumn;
import com.inspur.incloud.binlog.util.BinLogUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.shyiko.mysql.binlog.event.EventType.isUpdate;

/**
 * 数据库监听器
 * @author mark
 * @date 2023/7/26 11:04
 */
@Component
public class EventListener implements BinaryLogClient.EventListener {

    private String dbTable;

    // 数据库表和字段集合的对应关系
    private final Map<String, Map<String, DbColumn>> dbTableColMap = new ConcurrentHashMap<>();

    // 获取日志
    static Log log = LogFactory.getFactory().getInstance(EventListener.class);

    @Resource
    private BinLogConstants binLogConstants;

    /**
     * Binlog消费者需重写该方法，用于消费解析后的Binlog日志
     *
     * @param item binlog解析结果
     */
    public void consume(BinLogItem item) {
        log.warn("this method need to be override.");
    }

    /**
     * 启动
     */
    public void start() throws Exception {
        BinaryLogClient binLogClient = new BinaryLogClient(
                binLogConstants.getHost(), binLogConstants.getPort(), binLogConstants.getUsername(), binLogConstants.getPassword());
        binLogClient.registerEventListener(this);
        binLogClient.registerLifecycleListener(new MyLifecycleListener());

        // 反序列化
        EventDeserializer eventDeserializer = new EventDeserializer();
        // 不去反序列化TABLE_MAP事件，插入、更新和删除相关事件以外的其他事件，降低性能损耗
        setNonDeserializeEvents(eventDeserializer);
        // 不去反序列化日期时间和字符串，降低性能损耗
        eventDeserializer.setCompatibilityMode(
                EventDeserializer.CompatibilityMode.DATE_AND_TIME_AS_LONG,
                EventDeserializer.CompatibilityMode.CHAR_AND_BINARY_AS_BYTE_ARRAY
        );
        binLogClient.setEventDeserializer(eventDeserializer);

        // 多副本时，需为各副本设置不同serverId
        final String hostAddress = InetAddress.getLocalHost().getHostAddress();
        final long serverId = ipToLong(hostAddress);
        binLogClient.setServerId(serverId);

        // 注册监听
        final Map<String, String> dbTablesMap = binLogConstants.getDbTablesMap();
        for (String db : dbTablesMap.keySet()) {
            String[] tableList = dbTablesMap.get(db).split(",");
            for (String table : tableList) {
                regListener(db, table);
            }
        }

        // 连接
        binLogClient.connect();
    }

    /**
     * 接收消息通知
     */
    @Override
    public void onEvent(Event event) {
        EventType eventType = event.getHeader().getEventType();

        if (eventType == EventType.TABLE_MAP) {
            TableMapEventData tableData = event.getData();
            String db = tableData.getDatabase();
            String table = tableData.getTable();
            dbTable = BinLogUtils.getDbTable(db, table);
        }

        // 只解析指定表的事件
        if (dbTable != null && !dbTableColMap.containsKey(dbTable)) {
            return;
        }

        if (isUpdate(eventType)) {
            UpdateRowsEventData data = event.getData();
            for (Map.Entry<Serializable[], Serializable[]> row : data.getRows()) {
                BinLogItem item = BinLogItem.itemFromUpdate(row, dbTableColMap.get(dbTable), eventType);
                item.setDbTable(dbTable);

                // 通知Binlog消费者
                consume(item);
            }
        }

    }

    /**
     * 注册监听
     */
    private void regListener(String db, String table) throws Exception {
        // 查询数据库表的字段集合
        Map<String, DbColumn> colMap = BinLogUtils.getColMap(binLogConstants, db, table);

        // 保存数据库表和字段集合的对应关系
        String dbTable = BinLogUtils.getDbTable(db, table);
        dbTableColMap.put(dbTable, colMap);
    }

    /**
     * BinLog生命周期
     */
    static class MyLifecycleListener implements BinaryLogClient.LifecycleListener {
        @Override
        public void onConnect(BinaryLogClient client) {

        }

        @Override
        public void onCommunicationFailure(BinaryLogClient client, Exception ex) {
            log.error("Communicate To BinLog Stream Failure!");
            log.error(ex.getMessage(), ex);
        }

        @Override
        public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) {
            log.error("Event Deserialization Failure!");
            log.error(ex.getMessage(), ex);
        }

        @Override
        public void onDisconnect(BinaryLogClient client) {
            log.info("BinLog DisConnected");
        }
    }

    /**
     * 不去反序列化TABLE_MAP事件，插入、更新和删除相关事件以外的其他事件，降低性能损耗
     */
    private void setNonDeserializeEvents(EventDeserializer eventDeserializer) {
        eventDeserializer.setEventDataDeserializer(EventType.UNKNOWN, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.START_V3, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.QUERY, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.STOP, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.ROTATE, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.INTVAR, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.LOAD, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.SLAVE, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.CREATE_FILE, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.APPEND_BLOCK, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.EXEC_LOAD, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.DELETE_FILE, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.NEW_LOAD, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.RAND, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.USER_VAR, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.FORMAT_DESCRIPTION, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.XID, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.BEGIN_LOAD_QUERY, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.EXECUTE_LOAD_QUERY, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.INCIDENT, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.HEARTBEAT, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.IGNORABLE, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.ROWS_QUERY, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.GTID, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.ANONYMOUS_GTID, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.PREVIOUS_GTIDS, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.TRANSACTION_CONTEXT, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.VIEW_CHANGE, new NullEventDataDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.XA_PREPARE, new NullEventDataDeserializer());
    }

    /**
     * ip地址转为long值
     */
    private long ipToLong(String str) {
        long[] ip = splitIp(str);
        final int IP0 = 24;
        final int IP1 = 16;
        final int IP2 = 8;
        final int IP3 = 3;
        if (ip != null) {
            return (ip[0] << IP0 ) + (ip[1] << IP1 ) + (ip[2] << IP2 ) + ip[IP3];
        } else {
            return 0;
        }
    }

    /**
     * 分割IP地址.
     */
    private long[] splitIp(String ip) {
        final int FOUR = 4;
        long[] ipArray = new long[FOUR];
        int position1 = ip.indexOf(".");
        int length = ip.length();
        final int SEVEN = 7;
        final int SIXTEEN = 16;
        final int THREE = 3;
        if (length >= SEVEN && length < SIXTEEN) {
            if (position1 > 0) {
                int position2 = ip.indexOf(".", position1 + 1);
                if (position2 > 0) {
                    int position3 = ip.indexOf(".", position2 + 1);
                    if (position3 > 0 && position3 < length - 1) {
                        try {
                            ipArray[0] = Long.parseLong(ip.substring(0, position1));
                            ipArray[1] = Long.parseLong(ip.substring(position1 + 1, position2));
                            ipArray[2] = Long.parseLong(ip.substring(position2 + 1, position3));
                            ipArray[THREE] = Long.parseLong(ip.substring(position3 + 1));
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
        return ipArray;
    }
}