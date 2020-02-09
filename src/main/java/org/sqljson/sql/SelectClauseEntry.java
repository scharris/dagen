package org.sqljson.sql;

import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.specs.queries.FieldTypeOverride;


public class SelectClauseEntry
{
   public enum Source { NATIVE_FIELD, INLINE_PARENT, PARENT_REFERENCE, CHILD_COLLECTION, HIDDEN_PK }

   private final String valueExpression;
   private final String outputName;
   private final Source source;
   private final List<FieldTypeOverride> fieldTypeOverrides;

   public SelectClauseEntry(String valueExpression, String outputName, Source source)
   {
      this(valueExpression, outputName, source, emptyList());
   }

   public SelectClauseEntry
   (
      String valueExpression,
      String outputName,
      Source source,
      List<FieldTypeOverride> typeOverrides
   )
   {
      this.valueExpression = valueExpression;
      this.outputName = outputName;
      this.source = source;
      this.fieldTypeOverrides = unmodifiableList(new ArrayList<>(typeOverrides));
   }

   public String getValueExpression() { return valueExpression; }

   public String getName() { return outputName; }

   public Source getSource() { return source; }

   public List<FieldTypeOverride> getFieldTypeOverrides() { return fieldTypeOverrides; }

   public @Nullable FieldTypeOverride getGeneratedTypeOverride(String language)
   {
      return fieldTypeOverrides.stream().filter(to -> to.getLanguage().equals(language)).findAny().orElse(null);
   }
}
