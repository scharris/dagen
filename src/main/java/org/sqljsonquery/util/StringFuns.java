package org.sqljsonquery.util;

import java.util.Set;


public final class StringFuns
{
   private static final String SPACES = "                                                                              ";

   public static String makeNameNotInSet(String baseName, Set<String> existingNames)
   {
      if ( !existingNames.contains(baseName) )
         return baseName;
      else
      {
         int i = 1;
         while ( existingNames.contains(baseName + i) ) ++i;
         return baseName + i;
      }
   }

   public static String lowercaseInitials(String name, String sep)
   {
      StringBuilder sb = new StringBuilder();

      for ( String word: name.split(sep) )
      {
         if ( word.length() > 0 )
            sb.append(Character.toLowerCase(word.charAt(0)));
      }

      return sb.toString();
   }

   /// Return camel-case form of the given string, where words separated by
   /// '_', ' ', or '-' characters are combined and the initial letter of the
   /// returned name is capitalized.
   public static String camelCase(String name)
   {
      StringBuilder res = new StringBuilder();
      for (String word : name.split("[_ -]"))
      {
         res.append(Character.toUpperCase(word.charAt(0)));
         res.append(word.substring(1).toLowerCase());
      }
      return res.toString();
   }

   public static String indentLines(String linesStr, int spacesCount)
   {
      return indentLines(linesStr, spacesCount, true);
   }

   public static String indentLines(String linesStr, int spacesCount, boolean indentFirstLine)
   {
      StringBuilder sb = new StringBuilder();

      CharSequence indention = SPACES.subSequence(0, Math.min(spacesCount, SPACES.length()));

      boolean pastFirst = false;
      for ( String line : linesStr.split("\n") )
      {
         if ( pastFirst ) sb.append('\n');
         if ( pastFirst || indentFirstLine ) sb.append(indention);
         sb.append(line);
         if ( !pastFirst ) pastFirst = true;
      }

      return sb.toString();
   }

   public static String unDoubleQuote(String s)
   {
      if ( s.startsWith("\"") && s.endsWith("\"") )
         return s.substring(1, s.length()-1);
      return s;
   }

   private StringFuns() {}
}
