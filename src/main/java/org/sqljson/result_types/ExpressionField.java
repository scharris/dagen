package org.sqljson.result_types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import org.sqljson.specs.queries.FieldTypeOverride;


public class ExpressionField
{
    private Optional<String> fieldExpression = Optional.empty();
    private Optional<String> outputName = Optional.empty();
    private List<FieldTypeOverride> fieldTypeOverrides = emptyList();

    public ExpressionField(Optional<String> fieldExpression, Optional<String> outputName, List<FieldTypeOverride> typeOverrides)
    {
        this.fieldExpression = fieldExpression;
        this.outputName = outputName;
        this.fieldTypeOverrides = unmodifiableList(new ArrayList<>(typeOverrides));
    }

    public Optional<String> getFieldExpression() { return fieldExpression; }

    public Optional<String> getOutputName() { return outputName; }

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
            Objects.equals(outputName, that.outputName) &&
            Objects.equals(fieldTypeOverrides, that.fieldTypeOverrides);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(fieldExpression, outputName, fieldTypeOverrides);
    }

    @Override
    public String toString()
    {
        return "ExpressionField{" +
            "fieldExpression=" + fieldExpression +
            ", outputName=" + outputName +
            ", fieldTypeOverrides=" + fieldTypeOverrides +
            '}';
    }
}
