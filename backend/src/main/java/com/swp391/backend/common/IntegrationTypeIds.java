package com.swp391.backend.common;

/**
 * Constants for IntegrationType IDs (as seeded in DB).
 * JIRA id = 1, GITHUB id = 2 (IDENTITY seed order).
 */
public final class IntegrationTypeIds {

    private IntegrationTypeIds() {
        // utility class – no instantiation
    }

    public static final int JIRA = 1;
    public static final int GITHUB = 2;
}
