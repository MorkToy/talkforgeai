package com.talkforgeai.backend.persona.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkforgeai.backend.persona.domain.PersonaEntity;
import com.talkforgeai.backend.persona.domain.PropertyEntity;
import com.talkforgeai.backend.persona.repository.PersonaRepository;
import com.talkforgeai.backend.storage.FileStorageService;
import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PersonaImportService {
    public static final Logger LOGGER = LoggerFactory.getLogger(PersonaImportService.class);
    private final ResourcePatternResolver resourcePatternResolver;

    private final FileStorageService fileStorageService;

    private final ResourceLoader resourceLoader;

    private final ResourcePatternResolver resourceResolver;

    private final PersonaRepository personaRepository;

    public PersonaImportService(FileStorageService fileStorageService, ResourceLoader resourceLoader, PersonaRepository personaRepository, ResourcePatternResolver resourceResolver) {
        this.resourceLoader = resourceLoader;
        this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
        this.fileStorageService = fileStorageService;
        this.personaRepository = personaRepository;
        this.resourceResolver = resourceResolver;
    }

    @Transactional
    public void importPersona() {
        ObjectMapper mapper = new ObjectMapper();
        List<PersonaEntity> personas = new ArrayList<>();

        try {
            LOGGER.info("Importing persona files.");

            Path personaImportDirectory = fileStorageService.getPersonaImportDirectory();

            // Read persona json files from import folder
            importPersonaFromImportDirectory(personaImportDirectory, mapper, personas);

            // Read persona json files from classpath:persona-import/
            importPersonaFromResource(mapper, personas);

            savePersona(personas);
        } catch (IOException e) {
            LOGGER.error("Error during import of persona.", e);
        }

        LOGGER.info("Copying images from import directory.");
        copyPersonaImagesFromImportDirectory();
        copyPersonaImagesFromResources();

    }

    private void savePersona(List<PersonaEntity> personas) {
        personas.forEach(personaEntity -> {
            boolean isExistsPersona = personaRepository.existsByName(personaEntity.getName());

            if (isExistsPersona) {
                LOGGER.info("Persona {} exists. Import skipped.", personaEntity.getName());
            } else {
                personaRepository.save(personaEntity);
            }
        });
    }

    private void importPersonaFromResource(ObjectMapper mapper, List<PersonaEntity> personas) throws IOException {
        Resource[] resources = resourcePatternResolver.getResources("classpath:persona-import/*.json");
        for (Resource resource : resources) {
            LOGGER.info("Importing Persona from " + resource.getFilename());

            InputStream inputStream = resource.getInputStream();
            PersonaEntity persona = getPersonaEntity(mapper, inputStream);
            personas.add(persona);
        }
    }

    private void importPersonaFromImportDirectory(Path personaImportDirectory, ObjectMapper mapper, List<PersonaEntity> personas) throws IOException {
        try (DirectoryStream<Path> directoryStream =
                     Files.newDirectoryStream(personaImportDirectory, "*.json")) {
            for (Path path : directoryStream) {
                LOGGER.info("Importing Persona from " + path.getFileName().toString());

                InputStream inputStream = Files.newInputStream(path);
                PersonaEntity persona = getPersonaEntity(mapper, inputStream);
                personas.add(persona);
            }
        }
    }

    private void copyPersonaImagesFromResources() {
        // The prefix "classpath*:" allows resources to be loaded from the classpath, matching the given pattern
        String personaImgPattern = "classpath*:persona-import/*.{jpg,png}";

        try {
            // Get Resource array for the static folder
            Resource[] imageResources = resourceResolver.getResources(personaImgPattern);

            // Iterate each image resource and copy to target destination
            for (Resource imageResource : imageResources) {
                if (imageResource.isReadable()) {
                    try (InputStream resourceStream = imageResource.getInputStream()) {
                        // You need to redefine your copyImage method to accept InputStream
                        String imageName = imageResource.getFilename();
                        copyImage(resourceStream, imageName);
                    } catch (IOException e) {
                        LOGGER.error("Failed to read image resource.", e);
                    }
                }
            }

        } catch (IOException e) {
            LOGGER.error("Failed to access persona resources.", e);
        }
    }

    private void copyPersonaImagesFromImportDirectory() {
        Path personaImportDirectory = fileStorageService.getPersonaImportDirectory();

        // Copy images from the personaImportDirectory
        try (var stream = Files.walk(personaImportDirectory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".png") || path.toString().endsWith(".jpg"))
                    .forEach(this::copyImage);
        } catch (IOException e) {
            LOGGER.error("Failed to access persona import directory.", e);
        }
    }

    private void copyImage(Path imagePath) {
        try {
            Path targetPath = fileStorageService.getPersonaDirectory().resolve(imagePath.getFileName());

            if (!Files.exists(targetPath)) {
                LOGGER.info("Copying image {}", imagePath);
                Files.copy(imagePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                LOGGER.info("Skipping copying image {}. File already exists in the target directory.", imagePath);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to copy image {}", imagePath, e);
        }
    }

    private void copyImage(InputStream imageStream, String imageName) {
        try {
            Path targetPath = fileStorageService.getPersonaDirectory().resolve(imageName);

            if (!Files.exists(targetPath)) {
                LOGGER.info("Copying image {}", imageName);
                Files.copy(imageStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                LOGGER.info("Skipping copying image {}. File already exists in the target directory.", imageName);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to copy image {}", imageName, e);
        }
    }

    @NotNull
    private PersonaEntity getPersonaEntity(ObjectMapper mapper, InputStream inputStream) throws IOException {
        PersonaEntity persona = mapper.readValue(inputStream, PersonaEntity.class);

        Map<String, PropertyEntity> properties = persona.getProperties();
        for (Map.Entry<String, PropertyEntity> entry : properties.entrySet()) {
            PropertyEntity propertyEntity = properties.get(entry.getKey());
            propertyEntity.setPropertyKey(entry.getKey());
        }
        return persona;
    }
}
