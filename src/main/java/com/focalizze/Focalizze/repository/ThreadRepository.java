package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.ThreadClass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreadRepository extends JpaRepository<ThreadClass,Long> {
}
