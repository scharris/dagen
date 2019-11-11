package org.sqljson;


public class DatabaseObjectsNotFoundException extends RuntimeException
{
   public DatabaseObjectsNotFoundException(String message)
   {
      super(message);
   }
}
