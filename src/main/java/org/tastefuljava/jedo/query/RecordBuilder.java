package org.tastefuljava.jedo.query;

public class RecordBuilder {
    final QueryBuilder query;
    final int index;
    final int firstColumn;
    final boolean notNull;

    RecordBuilder(QueryBuilder qry, int index, int firstColumn,
            boolean notNull) {
        this.query = qry;
        this.index = index;
        this.firstColumn = firstColumn;
        this.notNull = notNull;
    }

    public void addColumn(String columnName) {
        query.addColumn(index, columnName);
    }

    public JoinBuilder newJoin(boolean required, String tableName) {
        return query.newJoin(this, required, tableName);
    }
}
