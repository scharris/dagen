package org.sqljson.common.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;


public final class Nullables
{
   public static <T,U> @Nullable U applyIfPresent
      (
         @Nullable T t,
         Function<T,U> f
      )
   {
      return t == null ? null : f.apply(t);
   }

   public static <T,U> U applyOr
      (
         @Nullable T t,
         Function<T,U> f,
         U defaultVal
      )
   {
      return t == null ? defaultVal : f.apply(t);
   }

   public static <T> void ifPresent
      (
         @Nullable T t,
         Consumer<T> f
      )
   {
      if ( t != null )
         f.accept(t);
   }

   public static <T> T valueOr
      (
         @Nullable T t,
         T defaultVal
      )
   {
      return t != null ? t : defaultVal;
   }

   public static <T> T valueOrGet
      (
         @Nullable T t,
         Supplier<T> defaultValFn
      )
   {
      return t != null ? t : defaultValFn.get();
   }

   public static <T> T valueOrThrow
      (
         @Nullable T t,
         Supplier<? extends RuntimeException> errFn
      )
   {
      if ( t != null )
         return t;
      else
         throw errFn.get();
   }

   private Nullables() {}
}

