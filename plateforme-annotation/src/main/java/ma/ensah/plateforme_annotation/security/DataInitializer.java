package ma.ensah.plateforme_annotation.security;

import ma.ensah.plateforme_annotation.entites.Utilisateur;
import ma.ensah.plateforme_annotation.enums.Role;
import ma.ensah.plateforme_annotation.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initializeAdminUser() {
        return args -> {
            if (userRepository.findByLogin("admin").isEmpty()) {
                Utilisateur admin = new Utilisateur(
                        "Admin",
                        "Super",
                        "admin",
                        passwordEncoder.encode("admin"),
                        Role.Admin
                );


                System.out.println("Before saving - Login: " + admin.getLogin());
                userRepository.save(admin);
                System.out.println("Admin user created with login: admin, password: admin");
            } else {
                System.out.println("Admin user already exists, skipping creation.");
            }

        };
    }

}