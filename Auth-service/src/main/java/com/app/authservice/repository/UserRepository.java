package com.app.authservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.authservice.entity.Role;
import com.app.authservice.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUserId(Long userId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByRole(Role role);

    @Query("select u from User u where lower(u.username) like lower(concat('%', :keyword, '%'))")
    List<User> searchByUsername(@Param("keyword") String keyword);

    @Query("""
            select u from User u
            where (:keyword = ''
               or lower(u.username) like lower(concat('%', :keyword, '%'))
               or lower(u.email) like lower(concat('%', :keyword, '%'))
               or lower(u.fullName) like lower(concat('%', :keyword, '%')))
              and (:role is null or u.role = :role)
              and (:active is null or u.isActive = :active)
            order by u.createdAt desc
            """)
    List<User> adminSearchUsers(
            @Param("keyword") String keyword,
            @Param("role") Role role,
            @Param("active") Boolean active);

    long countByIsActive(boolean active);

    long countByRole(Role role);

    long countByRoleAndIsActive(Role role, boolean active);

    void deleteByUserId(Long userId);
}
