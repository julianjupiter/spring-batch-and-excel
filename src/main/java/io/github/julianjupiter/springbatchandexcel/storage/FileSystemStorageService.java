package io.github.julianjupiter.springbatchandexcel.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class FileSystemStorageService implements StorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemStorageService.class);
    private final Path fileStorageLocation;
    private final SpreadsheetRepository spreadsheetRepository;

    @Autowired
    public FileSystemStorageService(FileStorageProperties fileStorageProperties, SpreadsheetRepository spreadsheetRepository) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir());
        this.spreadsheetRepository = spreadsheetRepository;
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

    @Override
    public void store(MultipartFile file) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file " + filename);
            }

            if (filename.contains("..")) {
                throw new StorageException("Cannot store file with relative path outside current directory " + filename);
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, this.fileStorageLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + filename, e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.fileStorageLocation, 1)
                    .filter(path -> !path.equals(this.fileStorageLocation))
                    .map(this.fileStorageLocation::relativize);
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }
    }

    @Override
    public Path load(String filename) {
        return fileStorageLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(fileStorageLocation.toFile());
    }

    @Override
    public void saveSpreadsheet(Spreadsheet spreadsheet) {
        spreadsheetRepository.save(spreadsheet);
    }

    @Override
    public Optional<Spreadsheet> setSpreadsheet(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        try {
            Resource resource = loadAsResource(fileName);
            long lastModified = resource.lastModified();
            LocalDateTime localDateTime = Instant.ofEpochMilli(lastModified).atZone(ZoneId.systemDefault()).toLocalDateTime();

            Spreadsheet spreadsheet = new Spreadsheet();
            spreadsheet.setName(fileName);
            spreadsheet.setUploadedAt(localDateTime);

            return Optional.of(spreadsheet);
        } catch (Exception exception) {
            LOGGER.error(exception.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public List<Spreadsheet> findAll() {
        return spreadsheetRepository.findAll();
    }

    @Override
    public Spreadsheet findBySpreadsheetName(String name) {
        return spreadsheetRepository.findByName(name);
    }
}
