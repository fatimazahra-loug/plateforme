package ma.ensah.plateforme_annotation.controllers;

import ma.ensah.plateforme_annotation.entites.*;
import ma.ensah.plateforme_annotation.repositories.AnnotationRepository;
import ma.ensah.plateforme_annotation.repositories.TacheRepository;
import ma.ensah.plateforme_annotation.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/annotateur")
public class AnnotateurController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AnnotationRepository annotationRepository;
    @Autowired
    private TacheRepository tacheRepository;

    @GetMapping("/home")
    @PreAuthorize("hasRole('Annotateur')")
    public ModelAndView annotatorDashboard() {
        ModelAndView modelAndView = new ModelAndView("home");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        Utilisateur annotator = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Annotator not found: " + login));

        List<Tache> tasks = annotator.getTaches().stream()
                .filter(tache -> !tache.isDeleted())
                .peek(tache -> {
                    long totalCouples = tache.getCompletes().stream().count();
                    long completedCouples = tache.getCompletes().stream()
                            .filter(complete -> !complete.getAnnotations().isEmpty())
                            .count();
                    tache.setProgress(totalCouples > 0 ? (int) ((completedCouples * 100) / totalCouples) : 0);
                    tacheRepository.save(tache);
                })
                .collect(Collectors.toList());

        modelAndView.addObject("username", login);
        modelAndView.addObject("role", "Annotateur");
        modelAndView.addObject("message", "Bienvenue sur votre tableau de bord !");
        modelAndView.addObject("tasks", tasks);
        return modelAndView;
    }

    @GetMapping("/annotate/{id}")
    @PreAuthorize("hasRole('Annotateur')")
    public ModelAndView showAnnotatePage(@PathVariable Long id, @RequestParam(value = "coupleIndex", required = false) Integer coupleIndex) {
        ModelAndView modelAndView = new ModelAndView("annotate");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String login = auth.getName();
        Utilisateur annotator = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Annotator not found: " + login));

        Tache task = annotator.getTaches().stream()
                .filter(t -> t.getId().equals(id) && !t.isDeleted())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));

        List<Couple> couples = task.getCompletes();
        if (couples.isEmpty()) {
            throw new IllegalStateException("No couples available for task: " + id);
        }

        if (coupleIndex == null) {
            //on trouve le dernier couple annote pour l'annotateur
            List<Annotation> annotations = annotationRepository.findByAnnotateurAndCompleteIn(annotator, couples);
            if (!annotations.isEmpty()) {
                Annotation lastAnnotation = annotations.get(annotations.size() - 1);
                Couple lastCouple = lastAnnotation.getComplete();
                coupleIndex = couples.indexOf(lastCouple);
                coupleIndex = Math.min(coupleIndex + 1, couples.size() - 1);
            } else {
                coupleIndex = 0;
            }
        }

        coupleIndex = Math.max(0, Math.min(coupleIndex, couples.size() - 1));
        Couple currentCouple = couples.get(coupleIndex);
        List<String> classes = Arrays.stream(task.getDataset().getClasses().replaceAll("[\\[\\]]", "").split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        modelAndView.addObject("username", login);
        modelAndView.addObject("role", "Annotateur");
        modelAndView.addObject("task", task);
        modelAndView.addObject("datasetDescription", task.getDataset().getDescription());
        modelAndView.addObject("currentCouple", currentCouple);
        modelAndView.addObject("coupleIndex", coupleIndex);
        modelAndView.addObject("totalCouples", couples.size());
        modelAndView.addObject("classes", classes);

        return modelAndView;
    }

    @PostMapping("/annotate/{id}/validate")
    @PreAuthorize("hasRole('Annotateur')")
    public ModelAndView validateAnnotation(
            @PathVariable Long id,
            @RequestParam("coupleIndex") int coupleIndex,
            @RequestParam("selectedClass") String selectedClass) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String login = auth.getName();
        Utilisateur annotator = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Annotator not found: " + login));

        Tache task = annotator.getTaches().stream()
                .filter(t -> t.getId().equals(id) && !t.isDeleted())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));

        List<Couple> couples = task.getCompletes();
        if (couples.isEmpty()) {
            throw new IllegalStateException("No couples available for task: " + id);
        }
        coupleIndex = Math.max(0, Math.min(coupleIndex, couples.size() - 1));
        Couple currentCouple = couples.get(coupleIndex);

        Annotation annotation = new Annotation();
        annotation.setClassesChoises(selectedClass);
        annotation.setComplete(currentCouple);
        annotation.setAnnotateur(annotator);
        annotationRepository.save(annotation);

        int nextIndex = coupleIndex + 1;
        if (nextIndex >= couples.size()) {
            nextIndex = coupleIndex;
        }
        return new ModelAndView("redirect:/annotateur/annotate/" + id + "?coupleIndex=" + nextIndex);
    }


}