package com.swp391.backend.repository;

import com.swp391.backend.entity.IntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, Long> {

    /**
     * @deprecated Use {@link #findByStudentGroup_GroupIdAndIntegrationType_IntegrationTypeId(Long, Integer)}
     *             instead.
     *             Kept for backward compatibility with existing unit tests.
     */
    @Deprecated
    Optional<IntegrationConfig> findByStudentGroup_GroupId(Long groupId);

    /**
     * Find the integration config for a specific group and integration type.
     * Respects the UNIQUE(group_id, integration_type_id) constraint.
     *
     * @param groupId           the group ID
     * @param integrationTypeId use
     *                          {@link com.swp391.backend.utils.IntegrationTypeIds}
     */
    Optional<IntegrationConfig> findByStudentGroup_GroupIdAndIntegrationType_IntegrationTypeId(Long groupId, Integer integrationTypeId);
}
