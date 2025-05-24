package ma.ensah.plateforme_annotation.repositories;

import ma.ensah.plateforme_annotation.entites.Utilisateur;
import ma.ensah.plateforme_annotation.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Utilisateur, Long> {

    @Query("SELECT u FROM Utilisateur u WHERE u.login = :login AND u.deleted = false")
    Optional<Utilisateur> findByLogin(String login);

    @Query("SELECT u FROM Utilisateur u WHERE u.role = :role AND u.deleted = false")
    List<Utilisateur> findByRole(Role role);

    List<Utilisateur> findAllById(Iterable<Long> ids);
}
