package ma.ensah.plateforme_annotation.entites;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Tache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dateLimit;

    private int taille;

    private int progress = 0;

    @ManyToOne
    private Dataset dataset;

    @ManyToOne
    private Utilisateur annotateur;

    @OneToMany(mappedBy = "tache", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Couple> completes = new ArrayList<>();

    private boolean deleted = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDateLimit() {
        return dateLimit;
    }

    public void setDateLimit(LocalDateTime dateLimit) {
        this.dateLimit = dateLimit;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Utilisateur getAnnotateur() {
        return annotateur;
    }

    public void setAnnotateur(Utilisateur annotateur) {
        this.annotateur = annotateur;
    }

    public List<Couple> getCompletes() {
        return completes;
    }

    public void setCompletes(List<Couple> completes) {
        this.completes = completes;
    }
    public int getTaille() {
        return taille;
    }

    public void setTaille(int taille) {
        this.taille = taille;
    }
    public int getProgress() {
        return progress;
    }
    public void setProgress(int progress) {
        this.progress = progress;
    }
    public boolean isDeleted() {
        return  deleted;
    }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

}