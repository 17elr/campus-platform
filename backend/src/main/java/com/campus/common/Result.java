package com.campus.common;

public record Result<T>(int code, String message, T data) {
  public static <T> Result<T> ok(T d) {
    return new Result<>(200, "操作成功", d);
  }

  public static <T> Result<T> fail(String m) {
    return new Result<>(400, m, null);
  }
}
