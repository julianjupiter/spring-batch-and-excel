package io.github.julianjupiter.springbatchandexcel.storage;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.excel.RowMapper;
import org.springframework.batch.item.excel.mapping.BeanWrapperRowMapper;
import org.springframework.batch.item.excel.poi.PoiItemReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class ExcelReader {

	public static <T> ItemReader<T> excelToItemReader(String file, Class<T> clazz) throws Exception {
		PoiItemReader<T> reader = new PoiItemReader<>();
		reader.setLinesToSkip(1);
		Resource resource = new FileSystemResource(file);
		System.out.println("File " + ((FileSystemResource) resource).getPath() + " exists? " + resource.exists());
		reader.setResource(resource);
		reader.setRowMapper(excelRowMapper(clazz));

		return reader;
	}

	public static <T> ItemReader<T> excelToItemReader(String file, Class<T> clazz, RowMapper<T> rowMapper) throws Exception {
		PoiItemReader<T> reader = new PoiItemReader<>();
		reader.setLinesToSkip(1);
		Resource resource = new FileSystemResource(file);
		System.out.println("File " + ((FileSystemResource) resource).getPath() + " exists? " + resource.exists());
		reader.setResource(resource);
		reader.setRowMapper(rowMapper);

		return reader;
	}

	private static <T> RowMapper<T> excelRowMapper(Class<T> clazz) {
		BeanWrapperRowMapper<T> rowMapper = new BeanWrapperRowMapper<>();
		rowMapper.setTargetType(clazz);

		return rowMapper;
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> read(String workbookName, Class<T> tClass) throws Exception {
		String[] dirs = workbookName.split("/");
		String fileName = dirs[dirs.length - 1];
		String[] names = tClass.getName().split("\\.");
		String sheetName = names[names.length - 1];
		Workbook workbook = initializeForRead(workbookName);
		Sheet sheet = getSheetWithName(workbook, sheetName)
				.orElseThrow(() -> new NoSheetException("No sheet '" + sheetName +  "' was found from workbook '" + fileName + "'."));
		Class<?> clazz = Class.forName(tClass.getName());
		List<String> fieldNames = setupFieldsForClass(clazz);

		return (List<T>) IntStream.rangeClosed(1, sheet.getLastRowNum())
				.boxed()
				.map(rowCount -> setInstanceProps(clazz, fieldNames, sheet.getRow(rowCount)))
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private static <T> T setInstanceProps(Class<?> clazz, List<String> fieldNames, Row row) {
		try {
			T instance = (T) clazz.newInstance();
			AtomicInteger colCount = new AtomicInteger();
			StreamSupport.stream(row.spliterator(), false).forEach(cell -> {
				String fieldName = fieldNames.get(colCount.getAndIncrement());
				Method method;
				try {
					method = constructMethod(clazz, fieldName);
					CellType cellType = cell.getCellType();
					if (cellType == CellType.STRING) {
						String value = cell.getStringCellValue();
						Object[] values = new Object[1];
						values[0] = value;
						method.invoke(instance, values);
					} else if (cellType == CellType.NUMERIC) {
						Double num = cell.getNumericCellValue();
						Class<?> returnType = getGetterReturnClass(clazz, fieldName).get();
						if (returnType == int.class || returnType == Integer.class) {
							method.invoke(instance, num.intValue());
						} else if (returnType == double.class || returnType == Double.class) {
							method.invoke(instance, num);
						} else if (returnType == float.class || returnType == Float.class) {
							method.invoke(instance, num.floatValue());
						} else if (returnType == long.class || returnType == Long.class) {
							method.invoke(instance, num.longValue());
						} else if (returnType == Date.class) {
							Date date = HSSFDateUtil.getJavaDate(cell.getNumericCellValue());
							method.invoke(instance, date);
						} else if (returnType == LocalDate.class) {
							Date date = HSSFDateUtil.getJavaDate(cell.getNumericCellValue());
							Instant instant = date.toInstant();
							ZoneId defaultZoneId = ZoneId.systemDefault();
							LocalDate localDate = instant.atZone(defaultZoneId).toLocalDate();
							method.invoke(instance, localDate);
						} else if (returnType == LocalDateTime.class) {
							Date date = HSSFDateUtil.getJavaDate(cell.getNumericCellValue());
							Instant instant = date.toInstant();
							ZoneId defaultZoneId = ZoneId.systemDefault();
							LocalDateTime localDateTime = instant.atZone(defaultZoneId).toLocalDateTime();
							method.invoke(instance, localDateTime);
						}
					} else if (cellType == CellType.BOOLEAN) {
						boolean num = cell.getBooleanCellValue();
						Object[] values = new Object[1];
						values[0] = num;
						method.invoke(instance, values);
					}
				} catch (SecurityException | NoSuchMethodException | IllegalAccessException
						| InvocationTargetException exception) {
					exception.printStackTrace();
				}
			});
			
			return instance;
		} catch (InstantiationException | IllegalAccessException exception) {
			exception.printStackTrace();
		}
		
		return null;
	}

	private static Workbook initializeForRead(String workbookName) throws IOException {
		return WorkbookFactory.create(new FileInputStream(workbookName));
	}

	private static Optional<Sheet> getSheetWithName(Workbook workbook, String name) {
		return IntStream.range(0, workbook.getNumberOfSheets()).boxed()
				.filter(counter -> name.compareTo(workbook.getSheetName(counter)) == 0)
				.map(workbook::getSheetAt).findFirst();
	}

	private static List<String> setupFieldsForClass(Class<?> clazz) {
		Field[] fields = clazz.getDeclaredFields();

		return Arrays.stream(fields).map(field -> field.getName()).collect(Collectors.toList());
	}

	private static Method constructMethod(Class<?> clazz, String fieldName)
			throws SecurityException, NoSuchMethodException {
		return clazz.getMethod("set" + capitalize(fieldName), getGetterReturnClass(clazz, fieldName).get());
	}

	private static Optional<Class<?>> getGetterReturnClass(Class<?> clazz, String fieldName) {
		String methodName = "get" + capitalize(fieldName);
		String methodIsName = "is" + capitalize(fieldName);
		Class<?> returnType = Arrays.stream(clazz.getMethods())
				.filter(method -> method.getName().equals(methodName) || method.getName().equals(methodIsName))
				.map(method -> method.getReturnType()).findFirst().get();

		return Optional.of(returnType);
	}

	public static String capitalize(String string) {
		return Character.toUpperCase(string.charAt(0)) + string.substring(1);
	}

}

class NoSheetException extends Exception {
	public NoSheetException(String message) {
		super(message);
	}

	public NoSheetException(String message, Throwable cause) {
		super(message, cause);
	}
}


