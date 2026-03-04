package com.swp391.backend.integration.jira;

import com.swp391.backend.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * Builds JQL queries for Jira Search API.
 * All user-supplied values are safely quoted to prevent injection.
 */
@Component
public class JiraJqlBuilder {

    /**
     * Supported filter types for Jira issue export.
     */
    public enum FilterType {
        ALL, SPRINT, VERSION, LABEL
    }

    /**
     * Builds a JQL string for the given filter configuration.
     *
     * @param projectKey the Jira project key (from saved config)
     * @param filterType the filter type (ALL / SPRINT / VERSION / LABEL)
     * @param sprintId   required when filterType = SPRINT
     * @param versionId  required when filterType = VERSION
     * @param label      required when filterType = LABEL
     * @return a valid JQL string
     */
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

    /**
     * Safely quotes a JQL value:
     * <ul>
     * <li>Trims whitespace</li>
     * <li>Escapes embedded double-quotes ( " → \" )</li>
     * <li>Wraps with "..." if value contains whitespace or special JQL chars</li>
     * </ul>
     *
     * @param value raw user-supplied value
     * @return safely quoted JQL value
     */
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
