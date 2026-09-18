package com.campus.common;
public record Paging(int page, int size) {
 public Paging { page=Math.max(1,page); size=Math.max(1,Math.min(50,size)); }
 public long offset(){return ((long)page-1)*size;}
 public String limit(){return "LIMIT "+size+" OFFSET "+offset();}
}
