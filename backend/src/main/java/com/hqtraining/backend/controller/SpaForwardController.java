package com.hqtraining.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    // 让前端 history 路由在直接访问时也能回到同一个入口页面。
    @GetMapping({
            "/login",
            "/register/student",
            "/dashboard",
            "/accounts",
            "/applications",
            "/courses",
            "/lecturers",
            "/students",
            "/notices",
            "/enrollments",
            "/attendance",
            "/payments",
            "/evaluations",
            "/statistics"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
