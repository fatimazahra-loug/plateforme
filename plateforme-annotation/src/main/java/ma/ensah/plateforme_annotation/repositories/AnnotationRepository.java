package ma.ensah.plateforme_annotation.repositories;

import ma.ensah.plateforme_annotation.entites.Annotation;
import ma.ensah.plateforme_annotation.entites.Couple;
import ma.ensah.plateforme_annotation.entites.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AnnotationRepository extends JpaRepository<Annotation, Long> {


    List<Annotation> findByAnnotateurAndCompleteIn(Utilisateur annotator, List<Couple> couples);

    List<Annotation> findByComplete(Couple couple);


}