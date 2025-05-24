package ma.ensah.plateforme_annotation.entites;

import jakarta.persistence.*;

@Entity
public class Annotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String classesChoises;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complete_id")
    private Couple complete;

    @ManyToOne
    private Utilisateur annotateur;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClassesChoises() {
        return classesChoises;
    }

    public void setClassesChoises(String classesChoises) {
        this.classesChoises = classesChoises;
    }

    public Couple getComplete() {
        return complete;
    }

    public void setComplete(Couple complete) {
        this.complete = complete;
    }

    public Utilisateur getAnnotateur() {
        return annotateur;
    }

    public void setAnnotateur(Utilisateur annotateur) {
        this.annotateur = annotateur;
    }
}