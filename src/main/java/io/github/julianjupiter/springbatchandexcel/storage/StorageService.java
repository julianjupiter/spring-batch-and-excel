package io.github.julianjupiter.springbatchandexcel.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface StorageService {

    void init();

    void store(MultipartFile file);

    Stream<Path> loadAll();

    Path load(String filename);

    Resource loadAsResource(String filename);

    void deleteAll();

    void saveSpreadsheet(Spreadsheet spreadsheet);

    Optional<Spreadsheet> setSpreadsheet(MultipartFile file);

    List<Spreadsheet> findAll();

    Spreadsheet findBySpreadsheetName(String name);

}
