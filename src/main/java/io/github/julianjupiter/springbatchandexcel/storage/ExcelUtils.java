package io.github.julianjupiter.springbatchandexcel.storage;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.excel.RowMapper;
import org.springframework.batch.item.excel.mapping.BeanWrapperRowMapper;
import org.springframework.batch.item.excel.poi.PoiItemReader;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;

import java.nio.file.Path;

public class ExcelUtils {
    public static <T> ItemReader<T> excelToItemReader(Path file, Class<T> clazz) throws Exception {
        PoiItemReader<T> reader = new PoiItemReader<>();
        reader.setLinesToSkip(1);
        System.out.println("File Name: " + file.toString());
        Resource resource = new FileUrlResource(file.toString());
        System.out.println("File exists? " + resource.exists());
        reader.setResource(resource);
        reader.setRowMapper(excelRowMapper(clazz));
        return reader;
    }

    private static <T> RowMapper<T> excelRowMapper(Class<T> clazz) {
        BeanWrapperRowMapper<T> rowMapper = new BeanWrapperRowMapper<>();
        rowMapper.setTargetType(clazz);
        return rowMapper;
    }
}

