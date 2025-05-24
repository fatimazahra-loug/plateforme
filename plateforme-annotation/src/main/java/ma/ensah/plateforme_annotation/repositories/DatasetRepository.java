package ma.ensah.plateforme_annotation.repositories;

import ma.ensah.plateforme_annotation.entites.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {

}

