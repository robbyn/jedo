package org.tastefuljava.jedo.query;

public class JoinBuilder extends RecordBuilder {
    final RecordBuilder left;
    final boolean required;

    JoinBuilder(
            RecordBuilder left, boolean required, int index, int firstColumn) {
        super(left.query, index, firstColumn);
        this.left = left;
        this.required = required;
    }

    public void joinColumns(String left, String right) {
        query.joinColumns(this.left.index, left, index, right);
    }
}
