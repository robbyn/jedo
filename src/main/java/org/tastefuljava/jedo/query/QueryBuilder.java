package org.tastefuljava.jedo.query;

import java.util.ArrayList;
import java.util.List;

public class QueryBuilder {
    private final List<RecordBuilder> records = new ArrayList<>();
    private final StringBuilder selectPart = new StringBuilder();
    private final StringBuilder tablePart = new StringBuilder();
    private int columnCount;
    private int joinColumnCount;

    public QueryBuilder() {
        selectPart.append("SELECT ");
    }

    public RecordBuilder newRecord(String tableName) {
        int index = records.size();
        addTable(index, tableName);
        RecordBuilder rec = new RecordBuilder(this, index, columnCount);
        records.add(rec);
        return rec;
    }

    JoinBuilder newJoin(
            RecordBuilder left, boolean required, String tableName)  {
        int index = records.size();
        addJoin(required, index, tableName);
        JoinBuilder rec = new JoinBuilder(left, required, index, columnCount);
        records.add(rec);
        return rec;
    }

    void addTable(int index, String tableName) {
        tablePart
                .append(tableName)
                .append(" AS R")
                .append(index);
    }

    void addJoin(boolean required, int index, String tableName) {
        if (!required) {
            tablePart.append(" LEFT OUTER");
        }
        tablePart.append(" JOIN ");
        addTable(index, tableName);
        joinColumnCount = 0;
    }

    void addColumn(int index, String columnName) {
        if (columnCount > 0) {
            selectPart.append(',');
        }
        selectPart
                .append('R')
                .append(index)
                .append('.')
                .append(columnName);
        ++columnCount;
    }

    void joinColumns(int leftIndex, String left, int rightIndex, String right) {
        tablePart
                .append(joinColumnCount == 0 ? " ON " : " AND ")
                .append('R')
                .append(leftIndex)
                .append('.')
                .append(left)
                .append('=')
                .append('R')
                .append(rightIndex)
                .append('.')
                .append(right);
        ++joinColumnCount;
    }

    public void appendTo(StringBuilder buf) {
        buf
                .append(selectPart.toString())
                .append(" FROM ")
                .append(tablePart.toString());
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        appendTo(buf);
        return buf.toString();
    }
}
