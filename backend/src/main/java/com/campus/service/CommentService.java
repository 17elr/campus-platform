package com.campus.service;

import com.campus.dto.CommentDTO;
import com.campus.vo.CommentVO;
import java.util.List;

public interface CommentService {
  List<CommentVO> list(Long itemId);
  Long create(Long itemId, CommentDTO input, String authorization);
  void delete(Long itemId, Long id, String authorization);
}
