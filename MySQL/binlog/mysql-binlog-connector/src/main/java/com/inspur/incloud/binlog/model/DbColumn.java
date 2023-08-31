package com.inspur.incloud.binlog.model;

/**
 * 数据库表字段属性
 * @author mark
 * @date 2023/7/26 10:51
 */
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

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public int getInx() {
        return inx;
    }

    public void setInx(int inx) {
        this.inx = inx;
    }

    public String getColName() {
        return colName;
    }

    public void setColName(String colName) {
        this.colName = colName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
}
