package org.sqljson.util;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.sqljson.DatabaseObjectsNotFoundException;
import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.dbmd.Field;
import org.sqljson.dbmd.RelId;
import org.sqljson.dbmd.RelMetadata;


public final class DatabaseUtils
{
   public static void verifyTableFieldsExist
   (
      String table, // maybe qualified
      Optional<String> defaultSchema,
      List<String> fieldNames,
      DatabaseMetadata dbmd
   )
      throws DatabaseObjectsNotFoundException
   {
      RelId relId = dbmd.identifyTable(table, defaultSchema);

      RelMetadata tableMetadata = dbmd.getRelationMetadata(relId).orElseThrow(() ->
         new DatabaseObjectsNotFoundException("Table " + relId.toString() + " not found.")
      );

      verifyTableFieldsExist(fieldNames, tableMetadata, dbmd);
   }

   public static void verifyTableFieldsExist
      (
         List<String> fieldNames,
         RelMetadata tableMetadata,
         DatabaseMetadata dbmd
      )
      throws DatabaseObjectsNotFoundException
   {
      Set<String> dbmdTableFields = tableMetadata.getFields().stream().map(Field::getName).collect(toSet());

      List<String> missingTableInputFields =
         fieldNames.stream()
            .filter(fieldName -> !dbmdTableFields.contains(dbmd.normalizeName(fieldName)))
            .collect(toList());

      if ( !missingTableInputFields.isEmpty() )
         throw new DatabaseObjectsNotFoundException(
            "Field(s) not found in table " + tableMetadata.getRelationId() + ": " + missingTableInputFields + "."
         );
   }

   private DatabaseUtils() {}
}
