package ma.ensah.plateforme_annotation.repositories;

import ma.ensah.plateforme_annotation.entites.Dataset;
import ma.ensah.plateforme_annotation.entites.Tache;
import ma.ensah.plateforme_annotation.entites.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TacheRepository extends JpaRepository<Tache, Long> {
    Optional<Tache> findByAnnotateurAndDatasetAndDeletedFalse(Utilisateur annotateur, Dataset dataset);
}
