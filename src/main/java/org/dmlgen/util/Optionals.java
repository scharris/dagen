package org.dmlgen.util;

import java.util.Optional;


public final class Optionals
{
   public static <T> Optional<T> opt(T t) { return Optional.of(t); }

   public static <T> Optional<T> optn(T t) { return Optional.ofNullable(t); }

   private Optionals() {}
}
