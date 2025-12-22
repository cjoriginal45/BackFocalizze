package com.focalizze.Focalizze.repository;

import com.focalizze.Focalizze.models.Mention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Mention} entities.
 * Currently uses default JpaRepository methods for CRUD operations.
 * <p>
 * Interfaz de repositorio para gestionar entidades {@link Mention}.
 * Actualmente utiliza los métodos predeterminados de JpaRepository para operaciones CRUD.
 */
@Repository
public interface MentionRepository extends JpaRepository<Mention,Long> {

}
