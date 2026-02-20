package com.swp391.backend.repository;

import com.swp391.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByGithubUsernameIgnoreCase(String githubUsername);

    boolean existsByJiraAccountIdIgnoreCase(String jiraAccountId);

    @Query(
            "SELECT u FROM User u " +
                    "WHERE (:kw IS NULL OR :kw = '' OR " +
                    "LOWER(u.username) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
                    "LOWER(u.email) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
                    "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :kw, '%'))" +
                    ")"
    )
    Page<User> search(@Param("kw") String keyword, Pageable pageable);

    // Dùng cho dropdown assign lecturer: /api/admin/users?roleCode=LECTURER
    @Query(
            "SELECT u FROM User u " +
                    "WHERE (:roleCode IS NULL OR :roleCode = '' OR " +
                    "LOWER(u.role.roleCode) = LOWER(:roleCode)) " +
                    "AND (:kw IS NULL OR :kw = '' OR " +
                    "LOWER(u.username) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
                    "LOWER(u.email) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
                    "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :kw, '%'))" +
                    ")"
    )
    Page<User> searchWithRole(@Param("kw") String keyword,
                              @Param("roleCode") String roleCode,
                              Pageable pageable);
}
