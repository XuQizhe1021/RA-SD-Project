package com.hqtraining.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentRegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 50, message = "用户名长度不能超过50")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 50, message = "密码长度需为6到50位")
        String password,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 100, message = "姓名长度不能超过100")
        String displayName,

        @Size(max = 10, message = "性别长度不能超过10")
        String gender,

        @NotBlank(message = "所属企业不能为空")
        @Size(max = 150, message = "所属企业长度不能超过150")
        String companyName,

        @Size(max = 100, message = "岗位名称长度不能超过100")
        String jobTitle,

        @Size(max = 50, message = "学历长度不能超过50")
        String educationLevel,

        @Size(max = 50, message = "技术级别长度不能超过50")
        String techLevel,

        @NotBlank(message = "手机号不能为空")
        @Size(max = 30, message = "手机号长度不能超过30")
        String phone,

        @Size(max = 100, message = "邮箱长度不能超过100")
        String email
) {
}
