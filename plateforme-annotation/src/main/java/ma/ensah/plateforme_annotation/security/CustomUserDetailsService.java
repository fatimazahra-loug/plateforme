package ma.ensah.plateforme_annotation.security;

import ma.ensah.plateforme_annotation.entites.Utilisateur;
import ma.ensah.plateforme_annotation.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Service;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    public CustomUserDetailsService() {
        System.out.println("CustomUserDetailsService instantiated");
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Utilisateur user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with login: " + login));
        System.out.println("Loaded user: " + user.getLogin() + ", role: " + user.getRole() + ", password: " + user.getPassword());

        String role = "ROLE_" + user.getRole().name();
        System.out.println("Role: " + role);
        return new User(
                user.getLogin(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }

    public static class CustomSuccessHandler implements AuthenticationSuccessHandler {

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Authentication authentication) throws IOException {
            Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

            if (roles.contains("ROLE_Admin")) {
                response.sendRedirect("/admin/dashboard");
            }
            else if (roles.contains("ROLE_Annotateur")) {
                response.sendRedirect("/annotateur/home");
            }
        }


    }
}