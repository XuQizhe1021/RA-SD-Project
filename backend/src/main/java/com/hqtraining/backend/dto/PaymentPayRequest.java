package com.hqtraining.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentPayRequest(
        @NotNull(message = "实收金额不能为空")
        @DecimalMin(value = "0.00", message = "实收金额不能小于0")
        BigDecimal paidAmount,

        @NotBlank(message = "收费方式不能为空")
        @Size(max = 30, message = "收费方式长度不能超过30")
        String paymentMethod,

        @Size(max = 100, message = "代缴人长度不能超过100")
        String payerName,

        @Size(max = 255, message = "收费备注长度不能超过255")
        String paymentRemark
) {
}
