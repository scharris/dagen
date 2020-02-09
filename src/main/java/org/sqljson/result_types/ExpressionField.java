package org.sqljson.result_types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static java.util.Collections.unmodifiableList;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.specs.queries.FieldTypeOverride;


public class ExpressionField
{
    private String name;
    private @Nullable String fieldExpression;
    private List<FieldTypeOverride> fieldTypeOverrides;

    public ExpressionField(String name, @Nullable String fieldExpression, List<FieldTypeOverride> typeOverrides)
    {
        this.name = name;
        this.fieldExpression = fieldExpression;
        this.fieldTypeOverrides = unmodifiableList(new ArrayList<>(typeOverrides));
    }

    public String getName() { return name; }

    public @Nullable String getFieldExpression() { return fieldExpression; }

    public List<FieldTypeOverride> getFieldTypeOverrides() { return fieldTypeOverrides; }

    public @Nullable FieldTypeOverride getTypeOverride(String language)
    {
        return fieldTypeOverrides.stream().filter(to -> to.getLanguage().equals(language)).findAny().orElse(null);
    }


    @Override
    public boolean equals(@Nullable Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpressionField that = (ExpressionField) o;
        return Objects.equals(fieldExpression, that.fieldExpression) &&
            Objects.equals(name, that.name) &&
            Objects.equals(fieldTypeOverrides, that.fieldTypeOverrides);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(fieldExpression, name, fieldTypeOverrides);
    }

    @Override
    public String toString()
    {
        return "ExpressionField{" +
            "fieldExpression=" + fieldExpression +
            ", name=" + name +
            ", fieldTypeOverrides=" + fieldTypeOverrides +
            '}';
    }
}
