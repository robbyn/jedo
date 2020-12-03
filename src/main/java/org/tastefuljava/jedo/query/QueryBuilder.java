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
        tablePart.append("\nFROM ");
    }

    public RecordBuilder newRecord(String tableName) {
        int index = records.size();
        RecordBuilder rec = new RecordBuilder(this, index, columnCount, true);
        records.add(rec);
        addTable(index, tableName);
        return rec;
    }

    JoinBuilder newJoin(
            RecordBuilder left, boolean required, String tableName)  {
        int index = records.size();
        JoinBuilder rec = new JoinBuilder(left, required, index, columnCount);
        records.add(rec);
        addJoin(rec.notNull, index, tableName);
        return rec;
    }

    void addTable(int index, String tableName) {
        tablePart
                .append(tableName)
                .append(" AS R")
                .append(index);
    }

    void addJoin(boolean notNull, int index, String tableName) {
        tablePart.append(notNull ? "\nJOIN " : "\nLEFT OUTER JOIN ");
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
                .append(tablePart.toString());
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        appendTo(buf);
        return buf.toString();
    }
}
