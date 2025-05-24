package ma.ensah.plateforme_annotation.entites;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String filePath;
    private String classes;

    @Transient
    private int progress; // va etre pas stocke dans BD

    @OneToMany(mappedBy = "dataset")
    private List<Tache> taches = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "dataset_utilisateur",
            joinColumns = @JoinColumn(name = "dataset_id"),
            inverseJoinColumns = @JoinColumn(name = "utilisateur_id")
    )
    private List<Utilisateur> annotators = new ArrayList<>();


    public String getClasses() {
        return classes;
    }

    public void setClasses(String classes) {
        this.classes = classes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<Tache> getTaches() {
        return taches;
    }

    public void setTaches(List<Tache> taches) {
        this.taches = taches;
    }

    public List<Utilisateur> getAnnotators() {
        return annotators;
    }

    public void setAnnotators(List<Utilisateur> annotators) {
        this.annotators = annotators;
    }

    public void addAnnotator(Utilisateur annotator) {
        this.annotators.add(annotator);
    }
    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}


