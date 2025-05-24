package ma.ensah.plateforme_annotation.services;

import ma.ensah.plateforme_annotation.entites.Utilisateur;
import ma.ensah.plateforme_annotation.enums.Role;
import ma.ensah.plateforme_annotation.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public void register(String nom, String prenom, String login, String password) throws IllegalArgumentException {
        if (userRepository.findByLogin(login).isPresent()) {
            throw new IllegalArgumentException("Nom d'utilisateur déjà pris !");
        }

        Utilisateur user = new Utilisateur();
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setLogin(login);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.Annotateur);
        userRepository.save(user);
    }
}