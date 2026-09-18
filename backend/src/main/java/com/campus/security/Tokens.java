package com.campus.security;
import io.jsonwebtoken.Jwts; import javax.crypto.SecretKey; import org.springframework.stereotype.Component; import java.util.Date;
@Component public class Tokens {
 private final com.campus.mapper.UserMapper users;
 public Tokens(com.campus.mapper.UserMapper users){this.users=users;}
 private final java.util.Set<String> revoked=java.util.concurrent.ConcurrentHashMap.newKeySet();
 public void revoke(String token){revoked.add(token);}
 private final SecretKey key=Jwts.SIG.HS256.key().build();
 public String issue(Long id){return Jwts.builder().subject(id.toString()).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+7200000)).signWith(key).compact();}
 public Long verify(String token){try{if(revoked.contains(token))throw new IllegalArgumentException();Long id=Long.valueOf(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject());var user=users.selectById(id);if(user==null||!Integer.valueOf(1).equals(user.getStatus()))throw new IllegalArgumentException();return id;}catch(Exception e){throw new IllegalArgumentException("登录失效或账号已停用，请重新登录");}}
}
