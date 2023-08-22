1. 添加依赖

   ```xml
   <dependency>
       <groupId>com.github.shyiko</groupId>
       <artifactId>mysql-binlog-connector-java</artifactId>
       <version>0.21.0</version>
   </dependency>
   ```

2. 配置application.yml

   ```yml
   # binlog listener
   binlog:
     datasource:
       url: ${incloud.common.db.url}
       username: ${incloud.iresource-common.db.username}
       password: ${incloud.iresource-common.db.password}
     dbTables: '{"iresource": "t_net_net, t_net_subnet"}'
   ```
   
3. 创建枚举类，存储步骤2配置信息

   ```java
   package com.inspur.incloud.iresource.model.binlog;
   
   import lombok.Data;
   import org.springframework.beans.factory.annotation.Value;
   import org.springframework.stereotype.Component;
   
   import javax.annotation.PostConstruct;
   import java.util.Map;
   
   /**
    * 监听配置信息
    * @author mark
    * @date 2023/7/26 10:50
    */
   @Data
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
   }
   ```

4. 创建实体类，存储数据库表字段属性，包括数据库名、表名、字段信息

   ```java
   import lombok.Data;
   
   /**
    * 数据库表字段属性
    * @author mark
    * @date 2023/7/26 10:51
    */
   @Data
   public class DbColumn {
   
       /**
        * 数据库
        */
       public String schema;
   
       /**
        * 数据库表
        */
       public String table;
   
       /**
        * 数据库字段顺序
        */
       public int inx;
   
       /**
        * 数据库字段名
        */
       public String colName;
   
       /**
        * 数据库字段类型
        */
       public String dataType;
   
       public DbColumn(String schema, String table, int idx, String colName, String dataType) {
           this.schema = schema;
           this.table = table;
           this.colName = colName;
           this.dataType = dataType;
           this.inx = idx;
       }
   }
   ```

5. 创建实体类，存储BinLog解析后数据条目信息

   ```java
   package com.inspur.incloud.iresource.model.binlog;
   
   import com.github.shyiko.mysql.binlog.event.EventType;
   import com.google.common.collect.Maps;
   import lombok.Data;
   
   import java.io.Serializable;
   import java.util.Map;
   
   /**
    * BinLog解析后数据条目
    * @author mark
    * @date 2023/7/26 10:56
    */
   @Data
   public class BinLogItem implements Serializable {
   
       private static final long serialVersionUID = -8409340009861935412L;
   
       // 数据库-表
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
   
           item.before = Maps.newHashMap();
           item.after = Maps.newHashMap();
   
           Map<String, Serializable> beOrAf = Maps.newHashMap();
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
   
           Map<String, Serializable> be = Maps.newHashMap();
           Map<String, Serializable> af = Maps.newHashMap();
           columnMap.forEach((key, dbColumn) -> {
               be.put(key, row.getKey()[dbColumn.inx]);
               af.put(key, row.getValue()[dbColumn.inx]);
           });
           item.before = be;
           item.after = af;
   
           return item;
       }
   
   }
   
   ```

6. 创建Binlog工具类

   ```java
   package com.inspur.incloud.iresource.util.binlog;
   
   import cn.hutool.core.util.StrUtil;
   import com.github.shyiko.mysql.binlog.event.EventType;
   import com.inspur.incloud.iresource.model.binlog.BinLogConstants;
   import com.inspur.incloud.iresource.model.binlog.BinLogItem;
   import com.inspur.incloud.iresource.model.binlog.DbColumn;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.stereotype.Component;
   
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
    * 监听工具
    *
    * @author zrj
    * @since 2021/7/27
    **/
   @Slf4j
   @Component
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
       public static Map<String, DbColumn> getColMap(BinLogConstants binLogConstants, String db, String table) throws ClassNotFoundException {
           try {
               Class.forName("com.mysql.jdbc.Driver");
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
           } catch (SQLException e) {
               log.error("load db conf error, db_table={}:{} ", db, table, e);
           }
           return null;
       }
   
       /**
        * 根据DBTable获取table
        */
       public static String getTable(String dbTable) {
           if (StrUtil.isEmpty(dbTable)) {
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
   ```

7. 创建数据库监听器

   ```java
   package com.inspur.incloud.iresource.listener;
   
   import com.github.shyiko.mysql.binlog.BinaryLogClient;
   import com.github.shyiko.mysql.binlog.event.Event;
   import com.github.shyiko.mysql.binlog.event.EventType;
   import com.github.shyiko.mysql.binlog.event.TableMapEventData;
   import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
   import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
   import com.github.shyiko.mysql.binlog.event.deserialization.NullEventDataDeserializer;
   import com.inspur.incloud.common.util.IpUtil;
   import com.inspur.incloud.iresource.model.binlog.BinLogConstants;
   import com.inspur.incloud.iresource.model.binlog.BinLogItem;
   import com.inspur.incloud.iresource.model.binlog.DbColumn;
   import com.inspur.incloud.iresource.util.binlog.BinLogUtils;
   import lombok.extern.slf4j.Slf4j;
   
   import java.io.IOException;
   import java.io.Serializable;
   import java.net.InetAddress;
   import java.net.UnknownHostException;
   import java.util.Map;
   import java.util.concurrent.ConcurrentHashMap;
   
   import static com.github.shyiko.mysql.binlog.event.EventType.isUpdate;
   
   /**
    * 数据库监听器
    * @author mark
    * @date 2023/7/26 11:04
    */
   @Slf4j
   public class MysqlBinLogListener implements BinaryLogClient.EventListener {
   
       private String dbTable;
   
       private final BinLogConstants binLogConstants;
   
       private final BinaryLogClient binLogClient;
   
       // 数据库表和字段集合的对应关系
       private final Map<String, Map<String, DbColumn>> dbTableColMap = new ConcurrentHashMap<>();
   
       /**
        * 监听器初始化
        */
       public MysqlBinLogListener(BinLogConstants binLogConstants) {
           BinaryLogClient binLogClient = new BinaryLogClient(
                   binLogConstants.getHost(), binLogConstants.getPort(), binLogConstants.getUsername(), binLogConstants.getPassword());
           binLogClient.registerEventListener(this);
           binLogClient.registerLifecycleListener(new MyLifecycleListener());
   
           // 反序列化
           EventDeserializer eventDeserializer = new EventDeserializer();
           // 不去反序列化TABLE_MAP事件、更新相关的PRE_GA_UPDATE_ROWS、UPDATE_ROWS、EXT_UPDATE_ROWS三个事件之外的其他事件，降低性能损耗
           setNonDeserializeEvents(eventDeserializer);
           // 不去反序列化日期时间和字符串，降低性能损耗
           eventDeserializer.setCompatibilityMode(
                   EventDeserializer.CompatibilityMode.DATE_AND_TIME_AS_LONG,
                   EventDeserializer.CompatibilityMode.CHAR_AND_BINARY_AS_BYTE_ARRAY
           );
           binLogClient.setEventDeserializer(eventDeserializer);
   
           // 多副本时，需为各副本设置不同serverId
           try {
               final String hostAddress = InetAddress.getLocalHost().getHostAddress();
               final long serverId = IpUtil.ipToLong(hostAddress);
               binLogClient.setServerId(serverId);
           } catch (UnknownHostException e) {
               log.error(e.getMessage(), e);
           }
   
           this.binLogClient = binLogClient;
   
           this.binLogConstants = binLogConstants;
       }
   
       /**
        * 链接
        */
       public void connect() throws IOException {
           binLogClient.connect();
       }
   
       /**
        * 监听处理
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
               }
           }
   
       }
   
       /**
        * 注册监听
        */
       public void regListener(String db, String table) throws Exception {
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
               log.error("Communicate To BinLog Failure!");
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
        * 不去反序列化TABLE_MAP事件、更新相关的PRE_GA_UPDATE_ROWS、UPDATE_ROWS、EXT_UPDATE_ROWS三个事件之外的其他事件，降低性能损耗
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
           eventDeserializer.setEventDataDeserializer(EventType.PRE_GA_WRITE_ROWS, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.PRE_GA_DELETE_ROWS, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.WRITE_ROWS, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.DELETE_ROWS, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.INCIDENT, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.HEARTBEAT, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.IGNORABLE, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.ROWS_QUERY, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.EXT_WRITE_ROWS, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.EXT_DELETE_ROWS, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.GTID, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.ANONYMOUS_GTID, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.PREVIOUS_GTIDS, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.TRANSACTION_CONTEXT, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.VIEW_CHANGE, new NullEventDataDeserializer());
           eventDeserializer.setEventDataDeserializer(EventType.XA_PREPARE, new NullEventDataDeserializer());
       }
   }
   ```

8. 创建Binlog配置类

   ```java
   package com.inspur.incloud.iresource.config;
   
   import com.inspur.incloud.iresource.listener.MysqlBinLogListener;
   import com.inspur.incloud.iresource.model.binlog.BinLogConstants;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.boot.CommandLineRunner;
   import org.springframework.core.annotation.Order;
   import org.springframework.stereotype.Component;
   
   import javax.annotation.Resource;
   import java.io.IOException;
   import java.util.Arrays;
   import java.util.List;
   import java.util.Map;
   
   /**
    * BinLog配置类
    * @author mark
    * @date 2023/7/26 11:04
    **/
   @Slf4j
   @Component
   @Order(value = 1)
   public class BinLogConfig implements CommandLineRunner {
   
       @Resource
       private BinLogConstants binLogConstants;
   
       @Override
       public void run(String... args) throws IOException {
           // 初始化监听器
           MysqlBinLogListener mysqlBinLogListener = new MysqlBinLogListener(binLogConstants);
   
           // 注册监听
           final Map<String, String> dbTablesMap = binLogConstants.getDbTablesMap();
           for (String db : dbTablesMap.keySet()) {
               List<String> tableList = Arrays.asList(dbTablesMap.get(db).split(","));
               tableList.forEach(table -> {
                   try {
                       mysqlBinLogListener.regListener(db, table);
                   } catch (Exception e) {
                       log.error(e.getMessage(), e);
                   }
               });
           }
   
           // 连接
           mysqlBinLogListener.connect();
       }
   }
```
   
   

