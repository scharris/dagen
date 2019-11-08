package io.sqljson.util;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;


public final class StringFuns
{
   private static final String SPACES = "                                                                              ";

   public static String makeNameNotInSet(String baseName, Set<String> existingNames)
   {
      return makeNameNotInSet(baseName, existingNames, "");
   }

   public static String makeNameNotInSet(String baseName, Set<String> existingNames, String suffixSep)
   {
      if ( !existingNames.contains(baseName) )
         return baseName;
      else
      {
         int i = 1;
         while ( existingNames.contains(baseName + suffixSep + i) ) ++i;
         return baseName + suffixSep + i;
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
   public static String upperCamelCase(String name)
   {
      StringBuilder res = new StringBuilder();
      for (String word : name.split("[_ -]"))
      {
         res.append(Character.toUpperCase(word.charAt(0)));
         res.append(word.substring(1).toLowerCase());
      }
      return res.toString();
   }

   public static String lowerCamelCase(String name)
   {
      StringBuilder res = new StringBuilder();
      for (String word : name.split("[_ -]"))
      {
         if ( res.length() == 0 )
            res.append(word.toLowerCase());
         else
         {
            res.append(Character.toUpperCase(word.charAt(0)));
            res.append(word.substring(1).toLowerCase());
         }
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

   public static boolean matches(Optional<Pattern> pat, String s)
   {
      return pat.map(p -> p.matcher(s).matches()).orElse(false);
   }

   /**
    * Substitute a value in place of all occurrences of a variable in the passed expression.
    * @param varColonExpr An expression consisting of a variable name followed by an expression
    *                     using the variable, in the form var:expr. Only the first colon is considered
    *                     as the separator, and any whitespace is trimmed from both the variable name
    *                     and expression sides prior to interpretation.
    * @param varValue The value to be substituted into the expression.
    * @return The expression value with the actual variable value replacing all occurrences of the variable.
    */
   public static String substituteVarValue(String varColonExpr, String varValue)
   {
      int firstColonIx = varColonExpr.indexOf(':');
      if ( firstColonIx == -1 )
         throw new RuntimeException("improper filter format for filter '" + varColonExpr + "'.");
      String var = varColonExpr.substring(0, firstColonIx).trim();
      String expr = varColonExpr.substring(firstColonIx+1).trim();
      return expr.replace(var, varValue);
   }



   private StringFuns() {}
}
