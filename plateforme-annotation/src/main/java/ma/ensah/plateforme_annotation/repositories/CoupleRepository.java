package ma.ensah.plateforme_annotation.repositories;

import ma.ensah.plateforme_annotation.entites.Couple;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoupleRepository extends JpaRepository<Couple, Long> {
}
