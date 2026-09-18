package com.campus.controller;

import com.campus.common.Result;
import com.campus.security.Tokens;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
public class ImageController {
 private final Tokens tokens;
 private final Path directory=Path.of("uploads").toAbsolutePath();
 public ImageController(Tokens tokens){this.tokens=tokens;}
 @PostMapping public Result<?> upload(@RequestHeader("Authorization") String auth,@RequestParam("file") MultipartFile file) throws java.io.IOException {
  if(!auth.startsWith("Bearer "))return Result.fail("请先登录");
  tokens.verify(auth.substring(7));
  if(file.isEmpty()||file.getSize()>5*1024*1024)return Result.fail("图片大小须在 5MB 以内");
  try(var stream=ImageIO.createImageInputStream(file.getInputStream())){
   var readers=ImageIO.getImageReaders(stream);
   if(!readers.hasNext())return Result.fail("请选择 JPG 或 PNG 图片");
   var reader=readers.next();
   try{
    reader.setInput(stream);
    String format=reader.getFormatName();
    if(!format.equalsIgnoreCase("JPEG")&&!format.equalsIgnoreCase("PNG"))return Result.fail("请选择 JPG 或 PNG 图片");
    if((long)reader.getWidth(0)*reader.getHeight(0)>20000000)return Result.fail("图片分辨率过大");
    var image=reader.read(0);
    Files.createDirectories(directory);
    String name=UUID.randomUUID()+".png";
    ImageIO.write(image,"png",directory.resolve(name).toFile());
    return Result.ok("/api/images/"+name);
   }finally{reader.dispose();}
  }
 }
 @GetMapping("/{name}") public ResponseEntity<byte[]> get(@PathVariable String name) throws java.io.IOException {
  if(!name.matches("[a-f0-9-]{36}\\.png"))return ResponseEntity.notFound().build();
  Path file=directory.resolve(name);
  if(!Files.isRegularFile(file))return ResponseEntity.notFound().build();
  return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(Files.readAllBytes(file));
 }
}
