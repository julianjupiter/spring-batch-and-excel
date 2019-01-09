package io.github.julianjupiter.springbatchandexcel.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.excel.RowMapper;
import org.springframework.batch.item.excel.mapping.BeanWrapperRowMapper;
import org.springframework.batch.item.excel.poi.PoiItemReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.nio.file.Path;

public class ExcelUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelUtils.class);

    public static <T> ItemReader<T> excelToItemReader(Path file, Class<T> clazz) throws Exception {
        PoiItemReader<T> reader = new PoiItemReader<>();
        reader.setLinesToSkip(1);
        Resource resource = new FileSystemResource(file);
        LOGGER.info("File " + ((FileSystemResource) resource).getPath() + " exists? " + resource.exists());
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

