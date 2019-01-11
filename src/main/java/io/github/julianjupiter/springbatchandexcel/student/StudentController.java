package io.github.julianjupiter.springbatchandexcel.student;

import io.github.julianjupiter.springbatchandexcel.storage.ExcelReader;
import io.github.julianjupiter.springbatchandexcel.storage.Spreadsheet;
import io.github.julianjupiter.springbatchandexcel.storage.StorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

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
    public String uploadExcel(MultipartFile file, RedirectAttributes redirectAttributes) throws Exception {
        storageService.store(file);
        Optional<Spreadsheet> spreadsheetOptional = storageService.setSpreadsheet(file);
        Spreadsheet spreadsheet = spreadsheetOptional.orElseThrow(() -> new Exception("Spreadsheet is null."));
        storageService.saveSpreadsheet(spreadsheet);
        redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/students/uploaded-excel";
    }

    @GetMapping("/uploaded-excel")
    public String loadUploadedExcels(Model model, @ModelAttribute("message") String message) {
        model.addAttribute("spreadsheets", storageService.findAll());
        model.addAttribute("message", message);

        return "student/uploadedExcel";
    }

    @PostMapping("/import")
    public String importStudents(@RequestParam String fileName, RedirectAttributes redirectAttributes) throws Exception {
        Path file = storageService.load(fileName);
        Resource resource = new FileSystemResource(file);
        Iterable<Student> students = ExcelReader.read(((FileSystemResource) resource).getPath(), Student.class);
        Spreadsheet spreadsheet = storageService.findBySpreadsheetName(fileName);
        spreadsheet.setImportedAt(LocalDateTime.now());
        studentService.saveAll(students);
        storageService.saveSpreadsheet(spreadsheet);
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
