package org.tastefuljava.jedo.mapping;

public interface ValueMapperVisitor<R> {
    public R visitClassMapper(ClassMapper mapper);
    public R visitColumnMapper(ColumnMapper mapper);
    public R visitComponentMapper(ComponentMapper mapper);
    public R visitListMapper(ListMapper mapper);
    public R visitMapMapper(MapMapper mapper);
    public R visitReferenceMapper(ReferenceMapper mapper);
    public R visitSetMapper(SetMapper mapper);
}
