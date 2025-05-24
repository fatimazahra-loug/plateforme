package ma.ensah.plateforme_annotation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EntityScan("ma.ensah.plateforme_annotation.entites")
@SpringBootApplication
public class PlateformeAnnotationApplication {

	public static void main(String[] args) {

		SpringApplication.run(PlateformeAnnotationApplication.class, args);
	}

}


