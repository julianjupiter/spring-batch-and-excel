package io.github.julianjupiter.springbatchandexcel.storage;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpreadsheetRepository extends JpaRepository<Spreadsheet, Long> {
    Spreadsheet findByName(String name);
}
