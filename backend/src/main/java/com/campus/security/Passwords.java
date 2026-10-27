package com.campus.security;

import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {

  public static String hash(String password) {
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return (
      "pbkdf2$" +
      Base64.getEncoder().encodeToString(salt) +
      "$" +
      Base64.getEncoder().encodeToString(derive(password, salt))
    );
  }

  private static byte[] derive(String password, byte[] salt) {
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        .generateSecret(
          new PBEKeySpec(password.toCharArray(), salt, 210000, 256)
        )
        .getEncoded();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static boolean matches(String password, String stored) {
    try {
      String[] p = stored.split("\\$");
      return (
        p.length == 3 &&
        p[0].equals("pbkdf2") &&
        MessageDigest.isEqual(
          derive(password, Base64.getDecoder().decode(p[1])),
          Base64.getDecoder().decode(p[2])
        )
      );
    } catch (Exception e) {
      return false;
    }
  }
}
