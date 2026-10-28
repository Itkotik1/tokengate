package tokengate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tokengate.domain.LlmModel;
import tokengate.domain.TokenPackage;
import tokengate.repository.LlmModelRepository;
import tokengate.repository.TokenPackageRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final LlmModelRepository modelRepository;
    private final TokenPackageRepository packageRepository;

    // GET /api/models - получить все модели
    @GetMapping("/models")
    public List<LlmModel> getAllModels() {
        return modelRepository.findAll();
    }

    // POST /api/models - создать новую модель (201 Created)
    @PostMapping("/models")
    @ResponseStatus(HttpStatus.CREATED)
    public LlmModel createModel(@RequestBody LlmModel model) {
        return modelRepository.save(model);
    }

    // GET /api/packages - получить все пакеты
    @GetMapping("/packages")
    public List<TokenPackage> getAllPackages() {
        return packageRepository.findAll();
    }

    // POST /api/packages - создать пакет (201 Created)
    @PostMapping("/packages")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenPackage createPackage(@RequestBody TokenPackage pkg) {
        // В реальном проекте здесь нужно проверить, существует ли model_id
        return packageRepository.save(pkg);
    }
}