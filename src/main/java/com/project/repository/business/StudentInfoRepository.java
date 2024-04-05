package com.project.repository.business;

import com.project.entity.concretes.business.StudentInfo;
import com.project.payload.response.business.StudentInfoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.stream.DoubleStream;

public interface StudentInfoRepository extends JpaRepository<StudentInfo, Long> {
    List<StudentInfo> getAllByStudentId_Id(Long studentId);

    boolean existsByIdEquals(Long id);

    @Query("SELECT (count (s)>0) from StudentInfo s WHERE s.student.id= ?1")
    boolean existsByStudent_IdEquals(Long studentId);

    @Query("SELECT s FROM StudentInfo s WHERE s.student.id= ?1")
    List<StudentInfo> findByStudent_IdEquals(Long studentId);

    @Query("SELECT s FROM StudentInfo s WHERE s.teacher.username= ?1")
    Page<StudentInfo> findByTeacherId_UsernameEquals(String userName, Pageable pageable);

    @Query("SELECT s FROM StudentInfo s WHERE s.student.username= ?1")
    Page<StudentInfo> findByStudentId_UsernameEquals(String userName, Pageable pageable);
}
