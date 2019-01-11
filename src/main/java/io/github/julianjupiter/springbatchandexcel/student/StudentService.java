package io.github.julianjupiter.springbatchandexcel.student;

import java.util.List;

public interface StudentService {

    List<Student> findAll();

    void save(Student student);

    void saveAll(Iterable<Student> students);

}
