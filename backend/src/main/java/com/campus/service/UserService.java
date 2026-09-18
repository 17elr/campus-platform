package com.campus.service; import com.campus.entity.User; import java.util.*; public interface UserService { User login(String name,String password); User register(User u); List<User> list(); }
