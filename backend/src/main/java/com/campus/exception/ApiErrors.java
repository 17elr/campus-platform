package com.campus.exception;

import com.campus.common.Result;
import org.springframework.dao.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiErrors {

  @ExceptionHandler(IllegalArgumentException.class)
  public Result<?> invalid(IllegalArgumentException e) {
    return Result.fail(e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Result<?> validation(MethodArgumentNotValidException e) {
    return Result.fail(
      e.getBindingResult().getFieldErrors().get(0).getDefaultMessage()
    );
  }

  @ExceptionHandler(DuplicateKeyException.class)
  public Result<?> duplicate() {
    return Result.fail("用户名已被使用");
  }

  @ExceptionHandler(DataAccessException.class)
  public Result<?> database() {
    return new Result<>(503, "数据库暂不可用，请联系管理员检查连接", null);
  }
}
