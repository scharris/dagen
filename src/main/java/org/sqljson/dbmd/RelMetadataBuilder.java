package org.sqljson.dbmd;

import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;

import org.checkerframework.checker.nullness.qual.Nullable;

class RelMetadataBuilder
{
   private final RelId relId;

   private final RelMetadata.RelType relType;

   private final @Nullable String relComment;

   private final List<Field> fields;

   public RelMetadataBuilder
      (
         RelId relId,
         RelMetadata.RelType relType,
         @Nullable String relComment
      )
   {
      this.relId = requireNonNull(relId);
      this.relType = requireNonNull(relType);
      this.relComment = relComment;
      this.fields = new ArrayList<>();
   }

   public RelId getRelId() { return relId; }

   public void addField(Field f) { fields.add(f); }

   public RelMetadata build()
   {
      return new RelMetadata(relId, relType, relComment, fields);
   }
}

