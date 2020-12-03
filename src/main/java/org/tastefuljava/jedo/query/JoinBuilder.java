package org.tastefuljava.jedo.query;

public class JoinBuilder extends RecordBuilder {
    final RecordBuilder left;

    JoinBuilder(
            RecordBuilder left, boolean notNull, int index, int firstColumn) {
        super(left.query, index, firstColumn, notNull && left.notNull);
        this.left = left;
    }

    public void joinColumns(String left, String right) {
        query.joinColumns(this.left.index, left, index, right);
    }
}
