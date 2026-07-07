package com.hqtraining.backend.service;

import com.hqtraining.backend.dto.LoginResponse;
import com.hqtraining.backend.dto.UserInfoResponse;
import com.hqtraining.backend.model.MenuItem;
import com.hqtraining.backend.model.UserAccount;
import com.hqtraining.backend.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final Map<String, UserAccount> accounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.accounts = Map.of(
                "manager01",
                new UserAccount(
                        1L,
                        "manager01",
                        passwordEncoder.encode("123456"),
                        "培训经理",
                        "manager@hq.local",
                        List.of("MANAGER")
                ),
                "executor01",
                new UserAccount(
                        2L,
                        "executor01",
                        passwordEncoder.encode("123456"),
                        "执行人-李工",
                        "executor@hq.local",
                        List.of("EXECUTOR")
                ),
                "staff01",
                new UserAccount(
                        3L,
                        "staff01",
                        passwordEncoder.encode("123456"),
                        "现场工作人员",
                        "staff@hq.local",
                        List.of("SITE_STAFF")
                ),
                "student01",
                new UserAccount(
                        4L,
                        "student01",
                        passwordEncoder.encode("123456"),
                        "演示学员",
                        "student@hq.local",
                        List.of("STUDENT")
                )
        );
    }

    public LoginResponse login(String username, String password) {
        UserAccount account = accounts.get(username);
        if (account == null || !passwordEncoder.matches(password, account.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        String token = jwtTokenProvider.generateToken(account.id(), account.username(), account.roles());
        return new LoginResponse(token, toUserInfo(account));
    }

    public UserInfoResponse getCurrentUser(String authorizationHeader) {
        Claims claims = parseClaims(authorizationHeader);
        String username = claims.getSubject();
        UserAccount account = Optional.ofNullable(accounts.get(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
        return toUserInfo(account);
    }

    public List<MenuItem> getMenus(String authorizationHeader) {
        UserInfoResponse user = getCurrentUser(authorizationHeader);
        List<String> roles = user.roles();

        if (roles.contains("MANAGER")) {
            return List.of(
                    new MenuItem("首页概览", "/dashboard", "HomeFilled"),
                    new MenuItem("培训申请", "/applications", "EditPen"),
                    new MenuItem("课程管理", "/courses", "Reading"),
                    new MenuItem("统计报表", "/statistics", "DataAnalysis")
            );
        }

        if (roles.contains("EXECUTOR")) {
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

        if (roles.contains("SITE_STAFF")) {
            return List.of(
                    new MenuItem("首页概览", "/dashboard", "HomeFilled"),
                    new MenuItem("签到管理", "/attendance", "Calendar"),
                    new MenuItem("收费管理", "/payments", "Money")
            );
        }

        return List.of(
                new MenuItem("首页概览", "/dashboard", "HomeFilled"),
                new MenuItem("通知发布", "/notices", "Document"),
                new MenuItem("报名管理", "/enrollments", "Ticket"),
                new MenuItem("评价管理", "/evaluations", "School")
        );
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

    private UserInfoResponse toUserInfo(UserAccount account) {
        return new UserInfoResponse(account.id(), account.username(), account.displayName(), account.roles());
    }
}
