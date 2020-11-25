package org.sqljson.dbmd;

import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;


class RelMetadataBuilder
{
   private final RelId relId;

   private final RelMetadata.RelType relType;

   private final List<Field> fields;

   public RelMetadataBuilder
      (
         RelId relId,
         RelMetadata.RelType relType
      )
   {
      this.relId = requireNonNull(relId);
      this.relType = requireNonNull(relType);
      this.fields = new ArrayList<>();
   }

   public RelId getRelId() { return relId; }

   public void addField(Field f) { fields.add(f); }

   public RelMetadata build()
   {
      return new RelMetadata(relId, relType, fields);
   }
}

