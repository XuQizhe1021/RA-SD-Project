package com.hqtraining.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InternalAccountCreateRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 50, message = "用户名长度不能超过50")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 50, message = "密码长度需为6到50位")
        String password,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 100, message = "姓名长度不能超过100")
        String displayName,

        @NotBlank(message = "角色不能为空")
        @Size(max = 50, message = "角色长度不能超过50")
        String roleCode,

        @Size(max = 30, message = "手机号长度不能超过30")
        String phone,

        @Size(max = 100, message = "邮箱长度不能超过100")
        String email
) {
}
