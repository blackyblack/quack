package nrs.util;

import java.util.List;

public final class ListUtils
{
  private ListUtils() {} //never
  
  public static <T> void addNonNull(List<T> list, T value)
  {
    if(value == null) return;
    list.add(value);
  }
}