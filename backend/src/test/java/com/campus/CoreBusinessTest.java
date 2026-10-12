package com.campus;

import com.campus.entity.User;
import com.campus.mapper.UserMapper;
import com.campus.security.Passwords;
import com.campus.security.Tokens;
import com.campus.service.CommissionService;
import com.campus.service.impl.UserServiceImpl;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CoreBusinessTest {
    @Test
    void registrationCannotChooseAdminRoleOrExposePassword() {
        UserMapper mapper = mock(UserMapper.class);
        User input = new User();
        input.setUsername("student-test");
        input.setPassword("TestPassword123");
        input.setNickname("测试学生");
        input.setRole("ADMIN");
        input.setStatus(0);
        when(mapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            User stored = invocation.getArgument(0);
            assertEquals("STUDENT", stored.getRole());
            assertEquals(1, stored.getStatus());
            assertNotEquals(input.getPassword(), stored.getPassword());
            assertTrue(Passwords.matches(input.getPassword(), stored.getPassword()));
            return 1;
        }).when(mapper).insert(any(User.class));

        User result = new UserServiceImpl(mapper).register(input);
        assertNull(result.getPassword());
        verify(mapper).insert(any(User.class));
    }

    @Test
    void loginRejectsDisabledAccountsAndWrongPasswords() {
        UserMapper mapper = mock(UserMapper.class);
        User user = new User();
        user.setStatus(0);
        user.setPassword(Passwords.hash("correct123"));
        when(mapper.selectOne(any())).thenReturn(user);
        UserServiceImpl service = new UserServiceImpl(mapper);

        assertNull(service.login("student", "correct123"));
        user.setStatus(1);
        assertNull(service.login("student", "wrong123"));
        verify(mapper, never()).updateById(any(User.class));
    }

    @Test
    void legacyPasswordIsMigratedOnlyAfterSuccessfulLogin() {
        UserMapper mapper = mock(UserMapper.class);
        User user = new User();
        user.setStatus(1);
        user.setPassword("legacy123");
        when(mapper.selectOne(any())).thenReturn(user);
        doAnswer(invocation -> {
            User stored = invocation.getArgument(0);
            assertTrue(stored.getPassword().startsWith("pbkdf2$"));
            assertTrue(Passwords.matches("legacy123", stored.getPassword()));
            return 1;
        }).when(mapper).updateById(any(User.class));
        UserServiceImpl service = new UserServiceImpl(mapper);

        assertNull(service.login("student", "wrong123"));
        verify(mapper, never()).updateById(any(User.class));
        assertNull(service.login("student", "legacy123").getPassword());
        verify(mapper).updateById(any(User.class));
    }

    @Test
    void tokenRejectsRevocationDisabledAccountsAndMalformedInput() {
        UserMapper mapper = mock(UserMapper.class);
        User user = new User();
        user.setId(7L);
        user.setStatus(1);
        when(mapper.selectById(7L)).thenReturn(user);
        Tokens tokens = new Tokens(mapper);
        String token = tokens.issue(7L);

        assertEquals(7L, tokens.verify(token));
        user.setStatus(0);
        assertThrows(IllegalArgumentException.class, () -> tokens.verify(token));
        user.setStatus(1);
        tokens.revoke(token);
        assertThrows(IllegalArgumentException.class, () -> tokens.verify(token));
        assertThrows(IllegalArgumentException.class, () -> tokens.verify("not-a-jwt"));
    }

    @Test
    void commissionRoundsHalfUpAndNeverExceedsCap() {
        CommissionService service = new CommissionService(mock(JdbcTemplate.class));
        var rule = new CommissionService.Rule(1L, BigDecimal.ZERO, null,
                new BigDecimal("0.03"), new BigDecimal("5.00"));

        assertEquals(new BigDecimal("0.02"), service.fee(new BigDecimal("0.50"), rule));
        assertEquals(new BigDecimal("5.00"), service.fee(new BigDecimal("1000.00"), rule));
        assertEquals(new BigDecimal("0.00"), service.fee(BigDecimal.ZERO, rule));
    }
}
