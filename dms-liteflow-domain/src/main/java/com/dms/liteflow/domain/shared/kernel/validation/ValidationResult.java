package com.dms.liteflow.domain.shared.kernel.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证结果
 * <p>
 * 封装配置验证的结果和错误信息
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * 是否验证通过
     */
    private boolean valid;

    /**
     * 错误信息列表
     */
    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();

    /**
     * 警告信息列表
     */
    @Builder.Default
    private List<ValidationWarning> warnings = new ArrayList<>();

    /**
     * 创建成功的验证结果
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();
    }

    /**
     * 创建失败的验证结果
     */
    public static ValidationResult failure() {
        return ValidationResult.builder()
                .valid(false)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();
    }

    /**
     * 添加错误
     */
    public void addError(String field, String message) {
        this.errors.add(new ValidationError(field, message));
        this.valid = false;
    }

    /**
     * 添加警告
     */
    public void addWarning(String field, String message) {
        this.warnings.add(new ValidationWarning(field, message));
    }

    /**
     * 合并另一个验证结果
     */
    public void merge(ValidationResult other) {
        if (other == null) {
            return;
        }
        this.errors.addAll(other.getErrors());
        this.warnings.addAll(other.getWarnings());
        if (!other.isValid()) {
            this.valid = false;
        }
    }

    /**
     * 获取错误信息摘要
     */
    public String getErrorMessage() {
        if (errors.isEmpty()) {
            return "Validation passed";
        }
        StringBuilder sb = new StringBuilder("Validation failed:\n");
        for (ValidationError error : errors) {
            sb.append("  - ").append(error.getField()).append(": ").append(error.getMessage()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 验证错误
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String message;
    }

    /**
     * 验证警告
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationWarning {
        private String field;
        private String message;
    }
}
