package com.ritesh.expensetracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ritesh.expensetracker.model.Users;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    boolean existsByEmail(String email);
}
