package org.tastefuljava.jedo.query;

public class RecordBuilder {
    final QueryBuilder query;
    final int index;
    final int firstColumn;

    RecordBuilder(QueryBuilder qry, int index, int firstColumn) {
        this.query = qry;
        this.index = index;
        this.firstColumn = firstColumn;
    }

    public void addColumn(String columnName) {
        query.addColumn(index, columnName);
    }

    public JoinBuilder newJoin(boolean required, String tableName) {
        return query.newJoin(this, required, tableName);
    }
}
