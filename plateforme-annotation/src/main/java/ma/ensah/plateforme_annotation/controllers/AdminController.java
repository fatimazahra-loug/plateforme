package ma.ensah.plateforme_annotation.controllers;

import com.opencsv.CSVWriter;
import jakarta.transaction.Transactional;
import ma.ensah.plateforme_annotation.entites.*;
import ma.ensah.plateforme_annotation.enums.Role;
import ma.ensah.plateforme_annotation.repositories.*;
import ma.ensah.plateforme_annotation.services.DatasetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminController {
    @Autowired
    DatasetRepository datasetRepository;

    @Autowired
    AnnotationRepository annotationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private TacheRepository tacheRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView adminDashboard() {
        ModelAndView modelAndView = new ModelAndView("dashboard");
        modelAndView.addObject("username", "admin");
        modelAndView.addObject("datasets", datasetService.getAllDatasets());
        return modelAndView;
    }

    @GetMapping("/admin/dataset-create")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView showCreateDatasetForm() {
        ModelAndView modelAndView = new ModelAndView("dataset-create");
        modelAndView.addObject("dataset", new Dataset());
        return modelAndView;
    }

    @PostMapping("/admin/save-dataset")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView saveDataset(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("file") MultipartFile file,
            @RequestParam("classes") String classes)
    {
        Dataset dataset = new Dataset();
        dataset.setName(name);
        dataset.setDescription(description);
        if (!file.isEmpty()) {
            try {
                String uploadDir = "uploads/";
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path filePath = Paths.get(uploadDir + fileName);
                Files.write(filePath, file.getBytes());
                dataset.setFilePath(filePath.toString());
            } catch (Exception e) {
                e.printStackTrace();
                return new ModelAndView("redirect:/admin/create-dataset?error=fileUploadFailed");
            }
        }

        List<String> classList = Arrays.stream(classes.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        dataset.setClasses(String.valueOf(classList));

        datasetRepository.save(dataset);
        return new ModelAndView("redirect:/admin/dashboard");
    }


    @GetMapping("/admin/dataset-list")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView showDatasetList() {
        ModelAndView modelAndView = new ModelAndView("dataset-list");
        List<Dataset> datasets = datasetRepository.findAll();
        // calculer l'avancement
        for (Dataset dataset : datasets) {
            List<Tache> taches = dataset.getTaches();
            if (taches.isEmpty()) {
                dataset.setProgress(0);
                continue;
            }
            // total of couples + couples annotes
            long totalCompletes = taches.stream()
                    .flatMap(tache -> tache.getCompletes().stream())
                    .count();
            long completedCompletes = taches.stream()
                    .flatMap(tache -> tache.getCompletes().stream())
                    .filter(complete -> !complete.getAnnotations().isEmpty())
                    .count();

            // calculer pourcentage
            int progress = totalCompletes > 0 ? (int) ((completedCompletes * 100) / totalCompletes) : 0;
            dataset.setProgress(progress);
        }
        modelAndView.addObject("datasets", datasets);
        return modelAndView;
    }


    @GetMapping("/admin/annotator-manage/{datasetId}")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView showAssignAnnotators(@PathVariable Long datasetId) {
        ModelAndView modelAndView = new ModelAndView("annotator-manage");
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + datasetId));
        List<Utilisateur> allAnnotators = userRepository.findByRole(Role.Annotateur);
        modelAndView.addObject("dataset", dataset);
        modelAndView.addObject("annotators", allAnnotators);
        return modelAndView;
    }

    @PostMapping("/admin/annotator-manage/{datasetId}")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView assignAnnotators(@PathVariable Long datasetId, @RequestParam("annotatorIds") List<Long> annotatorIds) {
        ModelAndView modelAndView = new ModelAndView("redirect:/admin/dataset-list");
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + datasetId));
        List<Utilisateur> selectedAnnotators = userRepository.findAllById(annotatorIds);
        dataset.setAnnotators(selectedAnnotators);
        datasetRepository.save(dataset);
        // un fois les annotateurs sont selectionnes les taches se divisent
        datasetService.distributeAnnotationTasks(dataset, selectedAnnotators);
        return modelAndView;
    }

    @GetMapping("/admin/dataset-details/{datasetId}")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView showDatasetDetails(@PathVariable Long datasetId, @RequestParam(value = "startIndex", defaultValue = "0") int startIndex) {
        ModelAndView modelAndView = new ModelAndView("dataset-details");
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + datasetId));

        // Calculer l'avancement du dataset
        List<Tache> taches = dataset.getTaches();
        if (taches == null || taches.isEmpty()) {
            dataset.setProgress(0);
            System.out.println("No Taches found for dataset: " + datasetId);
        } else {
            List<Couple> allCouples = taches.stream()
                    .flatMap(tache -> tache.getCompletes().stream())
                    .distinct()
                    .collect(Collectors.toList());
            long totalCouples = allCouples.size();
            long annotatedCouples = allCouples.stream()
                    .filter(c -> !annotationRepository.findByComplete(c).isEmpty())
                    .count();
            int progress = totalCouples > 0 ? (int) ((annotatedCouples * 100) / totalCouples) : 0;
            dataset.setProgress(progress);
            System.out.println("Dataset " + datasetId + " has " + totalCouples + " couples, " + annotatedCouples + " annotated");
        }

        // Fetch all couples from Tache entities with distinct
        List<Couple> allCouples = dataset.getTaches().stream()
                .flatMap(tache -> tache.getCompletes().stream())
                .filter(couple -> couple != null)
                .distinct()
                .collect(Collectors.toList());
        System.out.println("Fetched " + allCouples.size() + " couples for dataset: " + datasetId);

        int couplesPerPage = 10;

        int start = Math.max(0, startIndex);
        int end = Math.min(start + couplesPerPage, allCouples.size());
        List<Couple> visibleCouples = allCouples.subList(start, end);

        // Create a map of annotator ID to task ID
        Map<Long, Long> annotatorToTaskId = taches.stream()
                .collect(Collectors.toMap(
                        t -> t.getAnnotateur().getId(),
                        Tache::getId,
                        (existing, replacement) -> existing
                ));

        List<Utilisateur> annotators = taches.stream()
                .map(Tache::getAnnotateur)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        modelAndView.addObject("dataset", dataset);
        modelAndView.addObject("annotators", annotators);
        modelAndView.addObject("annotatorToTaskId", annotatorToTaskId);
        modelAndView.addObject("couples", visibleCouples);
        modelAndView.addObject("startIndex", start);
        modelAndView.addObject("totalCouples", allCouples.size());
        modelAndView.addObject("couplesPerPage", couplesPerPage);
        return modelAndView;
    }

    @GetMapping("/admin/annotator-gestion")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView showAnnotatorGestion() {
        ModelAndView modelAndView = new ModelAndView("annotator-gestion");
        List<Utilisateur> annotators = userRepository.findByRole(Role.Annotateur);
        modelAndView.addObject("annotators", annotators);
        return modelAndView;
    }
    @GetMapping("/admin/annotator-add")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView showAddAnnotatorForm() {
        ModelAndView modelAndView = new ModelAndView("annotator-add");
        modelAndView.addObject("annotator", new Utilisateur());
        return modelAndView;
    }
    @PostMapping("/admin/save-annotator")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView saveAnnotator(@ModelAttribute Utilisateur annotator) {

        String password = generateRandomPassword(12);

        String encryptedPassword = passwordEncoder.encode(password);
        annotator.setPassword(encryptedPassword);
        annotator.setRole(Role.Annotateur);
        userRepository.save(annotator);

        System.out.println("New annotator created: " + annotator.getNom() + " " + annotator.getPrenom() + " with login: " + annotator.getLogin() + " and password: " + password);
        return new ModelAndView("redirect:/admin/annotator-gestion");
    }
    //methode pour generer le password
    private String generateRandomPassword(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            password.append(characters.charAt(random.nextInt(characters.length())));
        }
        return password.toString();
    }

    @GetMapping("/admin/annotator-edit/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView showEditAnnotatorForm(@PathVariable Long id) {
        ModelAndView modelAndView = new ModelAndView("annotator-edit");
        Utilisateur annotator = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annotator not found: " + id));
        modelAndView.addObject("annotator", annotator);
        return modelAndView;
    }

    @PostMapping("/admin/update-annotator")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView updateAnnotator(@ModelAttribute Utilisateur updatedAnnotator) {
        Utilisateur existingAnnotator = userRepository.findById(updatedAnnotator.getId())
                .orElseThrow(() -> new IllegalArgumentException("Annotator not found: " + updatedAnnotator.getId()));

        existingAnnotator.setNom(updatedAnnotator.getNom());
        existingAnnotator.setPrenom(updatedAnnotator.getPrenom());
        existingAnnotator.setLogin(updatedAnnotator.getLogin());
        existingAnnotator.setPassword(existingAnnotator.getPassword());
        existingAnnotator.setRole(Role.Annotateur);

        userRepository.save(existingAnnotator);
        System.out.println("Annotator updated: " + existingAnnotator.getNom() + " " + existingAnnotator.getPrenom() + " with login: " + existingAnnotator.getLogin());
        return new ModelAndView("redirect:/admin/annotator-gestion");
    }

    @GetMapping("/admin/annotator-delete/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView deleteAnnotator(@PathVariable Long id) {
        Utilisateur annotator = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annotator not found: " + id));
        if (annotator.isDeleted()) {
            throw new IllegalArgumentException("Annotator is already logically deleted: " + id);
        }
        annotator.setDeleted(true);
        userRepository.save(annotator);
        System.out.println("Annotator logically deleted: " + annotator.getNom() + " " + annotator.getPrenom());
        return new ModelAndView("redirect:/admin/annotator-gestion");
    }

    @Transactional
    @GetMapping("/admin/remove-from-task/{taskId}/{annotatorId}")
    @PreAuthorize("hasRole('Admin')")
    public ModelAndView removeAnnotatorFromTask(@PathVariable Long taskId, @PathVariable Long annotatorId) {
        try {
            Utilisateur annotatorToRemove = userRepository.findById(annotatorId)
                    .orElseThrow(() -> new IllegalArgumentException("Annotator not found: " + annotatorId));
            Tache task = tacheRepository.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

            if (!task.getAnnotateur().getId().equals(annotatorId)) {
                throw new IllegalStateException("Annotator " + annotatorId + " is not assigned to task " + taskId);
            }

            Dataset dataset = task.getDataset();
            Long datasetId = dataset.getId();

            //les annotateurs qui sont affectes dataset
            List<Utilisateur> remainingAnnotators = dataset.getAnnotators().stream()
                    .filter(a -> !a.getId().equals(annotatorId))
                    .collect(Collectors.toList());

            // couple de texte de la tache
            List<Couple> allCouples = new ArrayList<>(task.getCompletes());
            List<Couple> annotatedCouples = new ArrayList<>();
            List<Couple> unannotatedCouples = new ArrayList<>();

            for (Couple couple : allCouples) {
                if (!couple.getAnnotations().isEmpty()) {
                    annotatedCouples.add(couple);
                } else {
                    unannotatedCouples.add(couple);
                }
            }

            // diviser les couples de texte non annotes sur les annotateurs restes
            if (!unannotatedCouples.isEmpty() && !remainingAnnotators.isEmpty()) {
                int couplesPerAnnotator = unannotatedCouples.size() / remainingAnnotators.size();
                int extraCouples = unannotatedCouples.size() % remainingAnnotators.size();

                int startIndex = 0;
                for (int i = 0; i < remainingAnnotators.size(); i++) {
                    Utilisateur annotator = remainingAnnotators.get(i);
                    int endIndex = startIndex + couplesPerAnnotator + (i < extraCouples ? 1 : 0);
                    if (startIndex >= unannotatedCouples.size()) break;

                    List<Couple> assignedCouples = unannotatedCouples.subList(startIndex, Math.min(endIndex, unannotatedCouples.size()));

                    Tache targetTache = tacheRepository.findByAnnotateurAndDatasetAndDeletedFalse(annotator, dataset)
                            .orElseGet(() -> {
                                Tache newTache = new Tache();
                                newTache.setDataset(dataset);
                                newTache.setAnnotateur(annotator);
                                newTache.setDateLimit(task.getDateLimit());
                                newTache.setTaille(0);
                                newTache.setProgress(0);
                                newTache.setDeleted(false);
                                return tacheRepository.save(newTache);
                            });

                    for (Couple couple : assignedCouples) {
                        couple.setTache(targetTache);
                        coupleRepository.save(couple);
                        targetTache.getCompletes().add(couple);
                    }
                    targetTache.setTaille(targetTache.getCompletes().size());
                    tacheRepository.save(targetTache);

                    startIndex = endIndex;
                }
            }

            //si il ny a pas d'annotateurs
            if (!unannotatedCouples.isEmpty() && remainingAnnotators.isEmpty()) {
                for (Couple couple : unannotatedCouples) {
                    couple.setTache(null);
                    coupleRepository.save(couple);
                }
            }

            for (Couple couple : annotatedCouples) {
                couple.setTache(null);
                coupleRepository.save(couple);
            }

            task.getCompletes().clear();
            tacheRepository.save(task);

            dataset.getAnnotators().removeIf(a -> a.getId().equals(annotatorId));
            datasetRepository.save(dataset);
            tacheRepository.delete(task);
            return new ModelAndView("redirect:/admin/dataset-details/" + datasetId + "?startIndex=0");
        } catch (Exception e) {
            e.printStackTrace();
            Long datasetId = tacheRepository.findById(taskId)
                    .map(t -> t.getDataset().getId())
                    .orElse(0L);
            return new ModelAndView("redirect:/admin/dataset-details/" + datasetId + "?error=" +
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/admin/export-couples-to-csv")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<byte[]> exportCouplesToCsv() {
        List<Couple> allCouples = coupleRepository.findAll();

        StringWriter stringWriter = new StringWriter();
        try (CSVWriter writer = new CSVWriter(stringWriter)) {
            String[] header = {"Couple ID", "T1", "T2", "Annotation"};
            writer.writeNext(header);
            for (Couple couple : allCouples) {
                List<Annotation> annotations = couple.getAnnotations();
                String classesChoises = annotations.isEmpty() ? "No annotation" : annotations.get(0).getClassesChoises();
                String[] data = {
                        String.valueOf(couple.getId()),
                        couple.getT1(),
                        couple.getT2(),
                        classesChoises
                };
                writer.writeNext(data);
            }
        } catch (IOException e) {
            System.err.println("Error generating CSV: " + e.getMessage());
            throw new RuntimeException("Failed to generate CSV", e);
        }

        byte[] csvBytes = stringWriter.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=couples_with_annotations.csv");
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv");

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }
}




