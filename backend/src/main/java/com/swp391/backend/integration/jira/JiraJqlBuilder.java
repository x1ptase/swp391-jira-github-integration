package com.swp391.backend.integration.jira;

import com.swp391.backend.exception.BusinessException;
import org.springframework.stereotype.Component;


@Component
public class JiraJqlBuilder {


    public enum FilterType {
        ALL, SPRINT, VERSION, LABEL
    }


    public String buildJql(String projectKey, FilterType filterType,
            Long sprintId, String versionId, String label) {

        String base = "project = " + projectKey;

        return switch (filterType) {
            case ALL -> base;

            case SPRINT -> {
                if (sprintId == null) {
                    throw new BusinessException(
                            "sprintId is required when filterType=SPRINT", 400);
                }
                yield base + " AND Sprint = " + sprintId;
            }

            case VERSION -> {
                if (versionId == null || versionId.isBlank()) {
                    throw new BusinessException(
                            "versionId is required when filterType=VERSION", 400);
                }
                yield base + " AND fixVersion = " + quoteJqlValue(versionId);
            }

            case LABEL -> {
                if (label == null || label.isBlank()) {
                    throw new BusinessException(
                            "label is required when filterType=LABEL", 400);
                }
                yield base + " AND labels = " + quoteJqlValue(label);
            }
        };
    }


    static String quoteJqlValue(String value) {
        if (value == null) {
            return "\"\"";
        }
        String trimmed = value.trim();
        // Escape embedded double-quotes
        String escaped = trimmed.replace("\"", "\\\"");

        // Always quote strings (safer, handles spaces & special chars)
        return "\"" + escaped + "\"";
    }
}
