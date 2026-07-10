package com.hqtraining.backend.service;

import com.hqtraining.backend.dto.LoginResponse;
import com.hqtraining.backend.dto.UserInfoResponse;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.MenuItem;
import com.hqtraining.backend.model.UserAccount;
import com.hqtraining.backend.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AuthService {

    private static final RowMapper<UserAccount> USER_ROW_MAPPER = (rs, rowNum) -> new UserAccount(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("display_name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("account_type"),
            rs.getString("account_status"),
            List.of()
    );

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(JdbcTemplate jdbcTemplate, JwtTokenProvider jwtTokenProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public LoginResponse login(String username, String password) {
        UserAccount account = getUserByUsername(username);
        if (account == null || !passwordEncoder.matches(password, account.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        if (!"ACTIVE".equals(account.accountStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号已停用");
        }

        String token = jwtTokenProvider.generateToken(account.id(), account.username(), account.roles());
        updateLastLoginAt(account.id());
        return new LoginResponse(token, toUserInfo(account));
    }

    public UserInfoResponse getCurrentUser(String authorizationHeader) {
        return toUserInfo(loadCurrentAccount(authorizationHeader));
    }

    public List<MenuItem> getMenus(String authorizationHeader) {
        CurrentUser currentUser = requireCurrentUser(authorizationHeader);

        if (currentUser.hasRole("MANAGER")) {
            return List.of(
                    new MenuItem("首页概览", "/dashboard", "HomeFilled"),
                    new MenuItem("培训申请", "/applications", "EditPen"),
                    new MenuItem("课程管理", "/courses", "Reading"),
                    new MenuItem("讲师管理", "/lecturers", "UserFilled"),
                    new MenuItem("统计报表", "/statistics", "DataAnalysis")
            );
        }

        if (currentUser.hasRole("EXECUTOR")) {
            return List.of(
                    new MenuItem("首页概览", "/dashboard", "HomeFilled"),
                    new MenuItem("课程管理", "/courses", "Reading"),
                    new MenuItem("讲师管理", "/lecturers", "UserFilled"),
                    new MenuItem("学员管理", "/students", "User"),
                    new MenuItem("通知发布", "/notices", "Document"),
                    new MenuItem("报名管理", "/enrollments", "Ticket"),
                    new MenuItem("统计报表", "/statistics", "DataAnalysis")
            );
        }

        if (currentUser.hasRole("SITE_STAFF")) {
            return List.of(
                    new MenuItem("首页概览", "/dashboard", "HomeFilled"),
                    new MenuItem("签到管理", "/attendance", "Calendar"),
                    new MenuItem("收费管理", "/payments", "Money"),
                    new MenuItem("评价管理", "/evaluations", "School")
            );
        }

        return List.of(
                new MenuItem("首页概览", "/dashboard", "HomeFilled"),
                new MenuItem("通知发布", "/notices", "Document"),
                new MenuItem("报名管理", "/enrollments", "Ticket"),
                new MenuItem("收费管理", "/payments", "Money"),
                new MenuItem("评价管理", "/evaluations", "School")
        );
    }

    public CurrentUser requireCurrentUser(String authorizationHeader) {
        UserAccount account = loadCurrentAccount(authorizationHeader);
        return new CurrentUser(
                account.id(),
                account.username(),
                account.displayName(),
                account.accountType(),
                account.roles()
        );
    }

    public void requireAnyRole(CurrentUser currentUser, String... roleCodes) {
        for (String roleCode : roleCodes) {
            if (currentUser.hasRole(roleCode)) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权访问该资源");
    }

    private Claims parseClaims(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少认证信息");
        }
        String token = authorizationHeader.substring(7);
        try {
            return jwtTokenProvider.parseToken(token);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token 无效");
        }
    }

    private UserAccount loadCurrentAccount(String authorizationHeader) {
        Claims claims = parseClaims(authorizationHeader);
        String username = claims.getSubject();
        UserAccount account = getUserByUsername(username);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }
        if (!"ACTIVE".equals(account.accountStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号已停用");
        }
        return account;
    }

    private UserAccount getUserByUsername(String username) {
        List<UserAccount> accounts = jdbcTemplate.query(
                """
                SELECT id, username, password_hash, display_name, email, phone, account_type, account_status
                FROM user_account
                WHERE username = ?
                """,
                USER_ROW_MAPPER,
                username
        );
        if (accounts.isEmpty()) {
            return null;
        }
        UserAccount baseAccount = accounts.get(0);
        return new UserAccount(
                baseAccount.id(),
                baseAccount.username(),
                baseAccount.passwordHash(),
                baseAccount.displayName(),
                baseAccount.email(),
                baseAccount.phone(),
                baseAccount.accountType(),
                baseAccount.accountStatus(),
                getRoles(baseAccount.id())
        );
    }

    private List<String> getRoles(Long userId) {
        return jdbcTemplate.queryForList(
                """
                SELECT r.role_code
                FROM user_role ur
                INNER JOIN role r ON ur.role_id = r.id
                WHERE ur.user_id = ?
                ORDER BY r.id ASC
                """,
                String.class,
                userId
        );
    }

    private void updateLastLoginAt(Long userId) {
        jdbcTemplate.update(
                "UPDATE user_account SET last_login_at = NOW(), updated_at = NOW() WHERE id = ?",
                userId
        );
    }

    private UserInfoResponse toUserInfo(UserAccount account) {
        return new UserInfoResponse(account.id(), account.username(), account.displayName(), account.roles());
    }
}
