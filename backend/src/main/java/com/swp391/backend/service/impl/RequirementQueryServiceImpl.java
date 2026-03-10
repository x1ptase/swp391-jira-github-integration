package com.swp391.backend.service.impl;

import com.swp391.backend.dto.response.RequirementDashboardItemResponse;
import com.swp391.backend.dto.response.StoryCountProjection;
import com.swp391.backend.entity.Requirement;
import com.swp391.backend.repository.RequirementRepository;
import com.swp391.backend.repository.TaskRepository;
import com.swp391.backend.service.RequirementQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequirementQueryServiceImpl implements RequirementQueryService {

        private final RequirementRepository requirementRepository;
        private final TaskRepository taskRepository;

        @Override
        @Transactional(readOnly = true)
        public Page<RequirementDashboardItemResponse> getDashboardRequirements(
                        Long groupId,
                        Integer statusId,
                        Integer priorityId,
                        String keyword,
                        Pageable pageable) {

                // Normalize keyword: trim + empty -> null
                String normalizedKeyword = (keyword != null && !keyword.trim().isEmpty())
                                ? keyword.trim()
                                : null;

                // 1. Query page Requirement (chỉ EPIC) với filter
                Page<Requirement> reqPage = requirementRepository.searchDashboardRequirements(
                                groupId, statusId, priorityId, normalizedKeyword, pageable);

                List<Requirement> requirements = reqPage.getContent();

                if (requirements.isEmpty()) {
                        return new PageImpl<>(Collections.emptyList(), pageable, reqPage.getTotalElements());
                }

                // 2. Lấy requirementIds để batch query story counts (tránh N+1)
                List<Integer> requirementIds = requirements.stream()
                                .map(Requirement::getRequirementId)
                                .collect(Collectors.toList());

                // 3. Batch query: tổng story count và done story count
                Map<Integer, Long> storyCountMap = toMap(
                                taskRepository.countStoriesByRequirementIds(requirementIds));
                Map<Integer, Long> doneCountMap = toMap(
                                taskRepository.countDoneStoriesByRequirementIds(requirementIds));

                // 4. Map sang DTO response
                List<RequirementDashboardItemResponse> dtoList = requirements.stream()
                                .map(r -> toDto(r, storyCountMap, doneCountMap))
                                .collect(Collectors.toList());

                return new PageImpl<>(dtoList, pageable, reqPage.getTotalElements());
        }

        // -------------------------------------------------------------------------
        // Helpers
        // -------------------------------------------------------------------------

        /** Chuyển projection list thành Map<requirementId, count>. */
        private Map<Integer, Long> toMap(List<StoryCountProjection> projections) {
                return projections.stream()
                                .collect(Collectors.toMap(
                                                StoryCountProjection::getRequirementId,
                                                StoryCountProjection::getCount));
        }

        /** Build RequirementDashboardItemResponse từ entity + count maps. */
        private RequirementDashboardItemResponse toDto(
                        Requirement r,
                        Map<Integer, Long> storyCountMap,
                        Map<Integer, Long> doneCountMap) {

                long total = storyCountMap.getOrDefault(r.getRequirementId(), 0L);
                long done = doneCountMap.getOrDefault(r.getRequirementId(), 0L);

                return RequirementDashboardItemResponse.builder()
                                .requirementId(r.getRequirementId())
                                .groupId(r.getStudentGroup() != null ? r.getStudentGroup().getGroupId() : null)
                                .epicKey(r.getJiraIssueKey())
                                .summary(r.getTitle())
                                // status
                                .statusId(r.getStatus() != null ? r.getStatus().getStatusId() : null)
                                .statusCode(r.getStatus() != null ? r.getStatus().getCode() : null)
                                .statusRaw(r.getJiraStatusRaw())
                                // priority
                                .priorityId(r.getPriority() != null ? r.getPriority().getPriorityId() : null)
                                .priorityCode(r.getPriority() != null ? r.getPriority().getCode() : null)
                                .priorityRaw(r.getJiraPriorityRaw())
                                // jira updated
                                .updated(r.getJiraUpdatedAt())
                                // stories progress
                                .storiesCount(total)
                                .doneStoriesCount(done)
                                .progressDone(done)
                                .progressTotal(total)
                                .build();
        }
}
