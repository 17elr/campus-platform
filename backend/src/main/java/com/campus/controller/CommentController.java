package com.campus.controller;

import com.campus.common.Result;
import com.campus.dto.CommentDTO;
import com.campus.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items/{itemId}/comments")
public class CommentController {

  private final CommentService service;

  public CommentController(CommentService s) {
    service = s;
  }

  @GetMapping
  public Result<?> list(@PathVariable Long itemId) {
    return Result.ok(service.list(itemId));
  }

  @PostMapping
  public Result<?> create(
    @PathVariable Long itemId,
    @Valid @RequestBody CommentDTO input,
    @RequestHeader(value = "Authorization", required = false) String header
  ) {
    return Result.ok(service.create(itemId, input, header));
  }

  @DeleteMapping("/{id}")
  public Result<?> delete(
    @PathVariable Long itemId,
    @PathVariable Long id,
    @RequestHeader(value = "Authorization", required = false) String header
  ) {
    service.delete(itemId, id, header);
    return Result.ok(null);
  }
}
