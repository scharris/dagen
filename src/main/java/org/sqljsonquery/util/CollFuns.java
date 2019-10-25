package org.sqljsonquery.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class CollFuns {

   public static <X> Set<X> setMinus(Collection<X> xs1, Set<X> xs2)
   {
      Set<X> xs = new HashSet<X>(xs1);
      xs.removeAll(xs2);
      return xs;
   }

   private CollFuns() {}
}
