package com.swp391.backend.repository;

import com.swp391.backend.entity.IntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, Long> {

    @Deprecated
    Optional<IntegrationConfig> findByStudentGroup_GroupId(Long groupId);

    Optional<IntegrationConfig> findByStudentGroup_GroupIdAndIntegrationType_IntegrationTypeId(Long groupId, Integer integrationTypeId);
}
