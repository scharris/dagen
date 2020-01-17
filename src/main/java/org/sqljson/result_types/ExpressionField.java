package org.sqljson.result_types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import static java.util.Collections.unmodifiableList;

import org.sqljson.specs.queries.FieldTypeOverride;


public class ExpressionField
{
    private String name;
    private Optional<String> fieldExpression;
    private List<FieldTypeOverride> fieldTypeOverrides;

    public ExpressionField(String name, Optional<String> fieldExpression, List<FieldTypeOverride> typeOverrides)
    {
        this.name = name;
        this.fieldExpression = fieldExpression;
        this.fieldTypeOverrides = unmodifiableList(new ArrayList<>(typeOverrides));
    }

    public String getName() { return name; }

    public Optional<String> getFieldExpression() { return fieldExpression; }

    public List<FieldTypeOverride> getFieldTypeOverrides() { return fieldTypeOverrides; }

    public Optional<FieldTypeOverride> getTypeOverride(String language)
    {
        return fieldTypeOverrides.stream().filter(to -> to.getLanguage().equals(language)).findAny();
    }


    @Override
    public boolean equals(Object o)
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
