package tokengate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tokengate.domain.LlmModel;
import tokengate.domain.TokenPackage;
import tokengate.repository.LlmModelRepository;
import tokengate.repository.TokenPackageRepository;
import java.util.stream.Collectors;
import java.util.Comparator;
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
    // GET /api/packages - получить пакеты (с фильтрацией и сортировкой)
    @GetMapping("/packages")
    public List<TokenPackage> getAllPackages(
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false, defaultValue = "id") String sortBy) {

        List<TokenPackage> packages = packageRepository.findAll();

        // Фильтрация через Stream API
        if (modelName != null && !modelName.isEmpty()) {
            packages = packages.stream()
                    .filter(p -> p.getModel().getName().toLowerCase().contains(modelName.toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (minPrice != null) {
            packages = packages.stream().filter(p -> p.getPrice() >= minPrice).collect(Collectors.toList());
        }
        if (maxPrice != null) {
            packages = packages.stream().filter(p -> p.getPrice() <= maxPrice).collect(Collectors.toList());
        }

        // Сортировка
        if ("price_asc".equals(sortBy)) {
            packages.sort(Comparator.comparing(TokenPackage::getPrice));
        } else if ("price_desc".equals(sortBy)) {
            packages.sort(Comparator.comparing(TokenPackage::getPrice).reversed());
        } else if ("name".equals(sortBy)) {
            packages.sort(Comparator.comparing(p -> p.getModel().getName()));
        }

        return packages;
    }

    // POST /api/packages - создать пакет (201 Created)
    @PostMapping("/packages")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenPackage createPackage(@RequestBody TokenPackage pkg) {
        // В реальном проекте здесь нужно проверить, существует ли model_id
        return packageRepository.save(pkg);
    }
}