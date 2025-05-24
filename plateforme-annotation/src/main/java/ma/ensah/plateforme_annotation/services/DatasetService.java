package ma.ensah.plateforme_annotation.services;

import ma.ensah.plateforme_annotation.entites.Couple;
import ma.ensah.plateforme_annotation.entites.Dataset;
import ma.ensah.plateforme_annotation.entites.Tache;
import ma.ensah.plateforme_annotation.entites.Utilisateur;
import ma.ensah.plateforme_annotation.repositories.DatasetRepository;
import ma.ensah.plateforme_annotation.repositories.TacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DatasetService {

    @Autowired
    private TacheRepository tacheRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @Transactional
    public void distributeAnnotationTasks(Dataset dataset, List<Utilisateur> annotators) {
        // Clear existing tasks
        List<Tache> existingTaches = dataset.getTaches();
        if (existingTaches != null) {
            for (Tache tache : existingTaches) {
                tacheRepository.delete(tache);
            }
        }
        dataset.getTaches().clear();
        datasetRepository.save(dataset);

        // Read couples from CSV
        List<String[]> couplesToAnnotate = readCouplesFromCSV(dataset.getFilePath());
        System.out.println("Dataset ID: " + dataset.getId() + " - Read " + couplesToAnnotate.size() + " couples from CSV: " + dataset.getFilePath());
        if (couplesToAnnotate.isEmpty()) {
            System.out.println("No couples found in CSV for dataset: " + dataset.getId());
            return;
        }

        // Distribute tasks among annotators
        int tasksPerAnnotator = couplesToAnnotate.size() / annotators.size();
        int extraTasks = couplesToAnnotate.size() % annotators.size();

        List<Tache> newTaches = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < annotators.size(); i++) {
            Utilisateur annotator = annotators.get(i);// va prendre le premier annotateur dans la liste
            int tasksForThisAnnotator = tasksPerAnnotator + (i < extraTasks ? 1 : 0);
            Tache tache = new Tache();
            tache.setDataset(dataset);
            tache.setAnnotateur(annotator);
            tache.setDateLimit(LocalDateTime.now().plusDays(30)); // Default deadline
            tache.setProgress(0); // Initialize progress to 0
            tacheRepository.save(tache);
            System.out.println("Saved Tache with ID: " + tache.getId() + " for annotator " + annotator.getId());

            // Create couples for this task
            List<Couple> couples = new ArrayList<>();
            for (int j = 0; j < tasksForThisAnnotator; j++) {
                if (index < couplesToAnnotate.size()) {
                    Couple couple = new Couple();
                    String t1 = couplesToAnnotate.get(index)[0];
                    String t2 = couplesToAnnotate.get(index)[1];
                    couple.setT1(t1.length() > 1000 ? t1.substring(0, 1000) : t1);
                    couple.setT2(t2.length() > 1000 ? t2.substring(0, 1000) : t2);
                    couple.setTache(tache);
                    couples.add(couple);
                    index++;
                }
            }

            // Set taille and save couples
            tache.setCompletes(couples);
            tache.setTaille(couples.size());
            tacheRepository.save(tache);
            System.out.println("Created Tache for annotator " + annotator.getId() + " with taille: " + couples.size() + " couples");
            newTaches.add(tache);
        }

        // Update dataset with new tasks
        dataset.setTaches(newTaches);
        datasetRepository.save(dataset);
        System.out.println("Finished distributing tasks for dataset: " + dataset.getId());
    }

    private List<String[]> readCouplesFromCSV(String filePath) {
        List<String[]> couples = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    couples.add(new String[]{parts[0].trim(), parts[1].trim()});
                } else {
                    System.out.println("Skipping invalid CSV line: " + line);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading CSV file at " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
        return couples;
    }
    public List<Dataset> getAllDatasets() {
        return datasetRepository.findAll();
    }
}