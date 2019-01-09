package io.github.julianjupiter.springbatchandexcel.student;

import io.github.julianjupiter.springbatchandexcel.storage.ExcelUtils;
import io.github.julianjupiter.springbatchandexcel.storage.StorageService;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/students")
public class StudentController {
    private StudentService studentService;
    private StorageService storageService;

    public StudentController(StudentService studentService, StorageService storageService) {
        this.studentService = studentService;
        this.storageService = storageService;
    }

    @GetMapping
    public String index(Model model, @ModelAttribute("message") String message) {
        model.addAttribute("students", studentService.findAll());
        model.addAttribute("message", message);

        return "student/index";
    }

    @GetMapping("/upload-excel")
    public String uploadExcel() {
        return "student/uploadExcel";
    }

    @PostMapping("/upload-excel")
    public String uploadExcel(MultipartFile file, RedirectAttributes redirectAttributes) {
        storageService.store(file);
        redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/students/uploaded-excel";
    }

    @GetMapping("/uploaded-excel")
    public String loadUploadedExcels(Model model, @ModelAttribute("message") String message) {
        model.addAttribute("excels", storageService.loadAll()
                .map(path -> {
                    ExcelDto excelDto = new ExcelDto();
                    excelDto.setFileName(path.getFileName().toString());
                    excelDto.setUri(MvcUriComponentsBuilder.fromMethodName(StudentController.class, "serveFile", path.getFileName().toString()).build().toString());
                    return excelDto;
                })
                .collect(Collectors.toList()));
        model.addAttribute("message", message);

        return "student/uploadedExcel";
    }

    @GetMapping("/uploaded-excel/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/import")
    public String importStudents(@RequestParam String fileName, RedirectAttributes redirectAttributes) throws Exception {
        ItemReader<Student> studentItemReader = ExcelUtils.excelToItemReader(storageService.load(fileName), Student.class);
        Student student = studentItemReader.read();
        if (student != null) {
            System.out.println("Student has data.");
            studentService.save(student);
        } else {
            System.out.println("Student is null");
            throw new Exception("Student is null");
        }

        redirectAttributes.addFlashAttribute("message", "You successfully imported students data from " + fileName + "!");

        return "redirect:/students";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception exception, Model model) {
        System.out.println(exception.getMessage());
        model.addAttribute("message", exception.getMessage());

        return "error/error";
    }
}
