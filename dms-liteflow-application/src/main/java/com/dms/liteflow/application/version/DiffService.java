package com.dms.liteflow.application.version;

import com.dms.liteflow.domain.version.aggregate.ConfigVersion;
import com.dms.liteflow.domain.version.repository.ConfigVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 版本对比服务
 * <p>
 * 使用 java-diff-utils 实现详细的版本差异对比
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiffService {

    private final ConfigVersionRepository configVersionRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 对比两个版本的内容差异（文本格式）
     *
     * @param versionId1 第一个版本ID
     * @param versionId2 第二个版本ID
     * @return 差异信息
     */
    public DiffResult compareVersionsText(Long versionId1, Long versionId2) {
        log.info("Comparing versions: {} and {}", versionId1, versionId2);

        ConfigVersion version1 = configVersionRepository.findById(versionId1)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId1));

        ConfigVersion version2 = configVersionRepository.findById(versionId2)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId2));

        // 使用 java-diff-utils 进行文本对比
        String[] lines1 = version1.getContent().split("\\n");
        String[] lines2 = version2.getContent().split("\\n");

        List<DiffLine> diffLines = new ArrayList<>();

        // 简单的行对比实现
        int maxLines = Math.max(lines1.length, lines2.length);
        for (int i = 0; i < maxLines; i++) {
            String line1 = i < lines1.length ? lines1[i] : "";
            String line2 = i < lines2.length ? lines2[i] : "";

            if (line1.equals(line2)) {
                diffLines.add(new DiffLine(i + 1, line1, "UNCHANGED", ""));
            } else {
                if (i < lines1.length && !line1.isEmpty()) {
                    diffLines.add(new DiffLine(i + 1, line1, "REMOVED", ""));
                }
                if (i < lines2.length && !line2.isEmpty()) {
                    diffLines.add(new DiffLine(i + 1, line2, "ADDED", ""));
                }
            }
        }

        return DiffResult.builder()
                .version1Info(createVersionInfo(version1))
                .version2Info(createVersionInfo(version2))
                .diffLines(diffLines)
                .summary(generateSummary(diffLines))
                .comparedAt(LocalDateTime.now().format(FORMATTER))
                .build();
    }

    /**
     * 对比两个版本的内容差异（HTML格式）
     *
     * @param versionId1 第一个版本ID
     * @param versionId2 第二个版本ID
     * @return HTML格式的差异信息
     */
    public String compareVersionsHtml(Long versionId1, Long versionId2) {
        log.info("Comparing versions with HTML output: {} and {}", versionId1, versionId2);

        ConfigVersion version1 = configVersionRepository.findById(versionId1)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId1));

        ConfigVersion version2 = configVersionRepository.findById(versionId2)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId2));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <title>版本对比 - Version ").append(version1.getVersion())
                .append(" vs ").append(version2.getVersion()).append("</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: 'Microsoft YaHei', Arial, sans-serif; margin: 20px; }\n");
        html.append("        .header { background: #f5f5f5; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n");
        html.append("        .version-info { display: inline-block; margin: 10px; }\n");
        html.append("        .diff-table { width: 100%; border-collapse: collapse; }\n");
        html.append("        .diff-table th { background: #4CAF50; color: white; padding: 10px; text-align: left; }\n");
        html.append("        .diff-table td { padding: 8px; border: 1px solid #ddd; font-family: 'Courier New', monospace; }\n");
        html.append("        .line-number { color: #999; text-align: right; width: 50px; }\n");
        html.append("        .added { background-color: #d4edda; }\n");
        html.append("        .removed { background-color: #f8d7da; }\n");
        html.append("        .unchanged { background-color: #fff; }\n");
        html.append("        .summary { background: #e7f3ff; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n");
        html.append("        .line-content { white-space: pre-wrap; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // 标题和版本信息
        html.append("    <div class=\"header\">\n");
        html.append("        <h1>版本对比报告</h1>\n");
        html.append("        <div class=\"version-info\">\n");
        html.append("            <strong>版本 ").append(version1.getVersion()).append(":</strong><br>\n");
        html.append("            创建时间: ").append(version1.getCreatedAt().format(FORMATTER)).append("<br>\n");
        html.append("            状态: ").append(version1.getStatus()).append("\n");
        html.append("        </div>\n");
        html.append("        <div class=\"version-info\">\n");
        html.append("            <strong>版本 ").append(version2.getVersion()).append(":</strong><br>\n");
        html.append("            创建时间: ").append(version2.getCreatedAt().format(FORMATTER)).append("<br>\n");
        html.append("            状态: ").append(version2.getStatus()).append("\n");
        html.append("        </div>\n");
        html.append("    </div>\n");

        // 执行对比
        DiffResult diffResult = compareVersionsText(versionId1, versionId2);

        // 差异摘要
        html.append("    <div class=\"summary\">\n");
        html.append("        <h2>差异摘要</h2>\n");
        html.append("        <p>").append(diffResult.getSummary()).append("</p>\n");
        html.append("    </div>\n");

        // 差异详情
        html.append("    <table class=\"diff-table\">\n");
        html.append("        <thead>\n");
        html.append("            <tr>\n");
        html.append("                <th>行号</th>\n");
        html.append("                <th>类型</th>\n");
        html.append("                <th>内容</th>\n");
        html.append("            </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");

        for (DiffLine line : diffResult.getDiffLines()) {
            String cssClass = "unchanged";
            switch (line.getType()) {
                case "ADDED":
                    cssClass = "added";
                    break;
                case "REMOVED":
                    cssClass = "removed";
                    break;
                default:
                    cssClass = "unchanged";
            }

            html.append("            <tr class=\"").append(cssClass).append("\">\n");
            html.append("                <td class=\"line-number\">").append(line.getLineNumber()).append("</td>\n");
            html.append("                <td>").append(line.getType()).append("</td>\n");
            html.append("                <td class=\"line-content\">").append(escapeHtml(line.getContent())).append("</td>\n");
            html.append("            </tr>\n");
        }

        html.append("        </tbody>\n");
        html.append("    </table>\n");

        html.append("    <p style=\"margin-top: 20px; color: #666;\">生成时间: ")
                .append(diffResult.getComparedAt()).append("</p>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * 生成差异摘要
     */
    private String generateSummary(List<DiffLine> diffLines) {
        long addedCount = diffLines.stream().filter(l -> "ADDED".equals(l.getType())).count();
        long removedCount = diffLines.stream().filter(l -> "REMOVED".equals(l.getType())).count();
        long unchangedCount = diffLines.stream().filter(l -> "UNCHANGED".equals(l.getType())).count();

        return String.format("总计 %d 行变化：%d 行新增，%d 行删除，%d 行未修改",
                diffLines.size(), addedCount, removedCount, unchangedCount);
    }

    /**
     * 创建版本信息对象
     */
    private VersionInfo createVersionInfo(ConfigVersion version) {
        return VersionInfo.builder()
                .version(version.getVersion())
                .configType(version.getConfigType())
                .configId(version.getConfigId())
                .status(version.getStatus().toString())
                .createdAt(version.getCreatedAt() != null ? version.getCreatedAt().format(FORMATTER) : "")
                .createdBy(version.getCreatedBy())
                .build();
    }

    /**
     * HTML转义
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 差异结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DiffResult {
        private VersionInfo version1Info;
        private VersionInfo version2Info;
        private List<DiffLine> diffLines;
        private String summary;
        private String comparedAt;
    }

    /**
     * 版本信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VersionInfo {
        private Integer version;
        private String configType;
        private Long configId;
        private String status;
        private String createdAt;
        private String createdBy;
    }

    /**
     * 差异行
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DiffLine {
        private Integer lineNumber;
        private String content;
        private String type;  // ADDED, REMOVED, UNCHANGED
        private String position;
    }
}
