package ma.ensah.plateforme_annotation.entites;

import jakarta.persistence.*;
import ma.ensah.plateforme_annotation.enums.Role;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)// l'id va s'incrementer dans la base de donnees
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(unique = true, nullable = false)
    private String login;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)//Stocke l'enum sous forme de chaîne
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean deleted = false;

    @OneToMany(mappedBy = "annotateur", cascade = CascadeType.ALL)
    private List<Annotation> annotations;

//    @ManyToMany(mappedBy = "annotators")
//    private List<Dataset> datasets = new ArrayList<>();

    @OneToMany(mappedBy = "annotateur")
    private List<Tache> taches = new ArrayList<>();

    public Utilisateur() {
        this.annotations = new ArrayList<>();
    }
    public Utilisateur(String nom, String prenom, String login, String password, Role role) {
        this.nom = nom;
        this.prenom = prenom;
        this.login = login;
        this.password = password;
        this.role = role;
        this.annotations = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getNom() {
        return nom;
    }

    public Role getRole() {
        return role;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getPassword() {
        return password;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setLogin(String login) {
        this.login = login;
    }
    public void setNom(String nom) {
        this.nom = nom;
    }
    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setRole(Role role) {
        this.role = role;
    }
    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public List<Tache> getTaches() {
        return taches;
    }

    public void setTaches(List<Tache> taches) {
        this.taches = taches;
    }


}