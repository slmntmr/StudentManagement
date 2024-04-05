package com.project.service.user;

import com.project.entity.concretes.business.LessonProgram;
import com.project.entity.concretes.user.User;
import com.project.entity.enums.RoleType;
import com.project.payload.mappers.UserMapper;
import com.project.payload.messages.SuccessMessages;
import com.project.payload.request.business.ChooseLessonProgramWithId;
import com.project.payload.request.user.StudentRequest;
import com.project.payload.request.user.StudentRequestWithoutPassword;
import com.project.payload.response.business.ResponseMessage;
import com.project.payload.response.user.StudentResponse;
import com.project.repository.user.UserRepository;
import com.project.service.business.LessonProgramService;
import com.project.service.helper.MethodHelper;
import com.project.service.validator.DateTimeValidator;
import com.project.service.validator.UniquePropertyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final UserRepository userRepository;
    private final MethodHelper methodHelper;
    private final UniquePropertyValidator uniquePropertyValidator;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleService userRoleService;
    private final LessonProgramService lessonProgramService;
    private final DateTimeValidator dateTimeValidator;

    public ResponseMessage<StudentResponse> saveStudent(StudentRequest studentRequest) {

        User advisorTeacher =  methodHelper.isUserExist(studentRequest.getAdvisorTeacherId());
        //!!! student'a atanan rehber ogretmen gercekten advisor mi kontrolu
        methodHelper.checkAdvisor(advisorTeacher);
        //!!! unique kontrolu
        uniquePropertyValidator.checkDuplicate(studentRequest.getUsername(), studentRequest.getSsn(),
                studentRequest.getPhoneNumber(),studentRequest.getEmail());
        //!!! DTO --> POJO
        User student = userMapper.mapStudentRequestToUser(studentRequest);
        student.setAdvisorTeacherId(advisorTeacher.getId());
        student.setPassword(passwordEncoder.encode(studentRequest.getPassword()));
        student.setUserRole(userRoleService.getUserRole(RoleType.STUDENT));
        student.setActive(true);
        student.setIsAdvisor(Boolean.FALSE);
        student.setStudentNumber(getLastNumber());

        return ResponseMessage.<StudentResponse>builder()
                .object(userMapper.mapUserToStudentResponse(userRepository.save(student)))
                .message(SuccessMessages.STUDENT_SAVED)
                .build();
    }

    private int getLastNumber(){
        //DB de hıc ogrencı yoksa  ogrencı numarası olarak 1000 gonderıyoruz
        if( ! userRepository.findStudent(RoleType.STUDENT)){
            //ilk ogrenci ise 1000 sayisini geri gonderiyoruz
            return 1000;
        }
        // DB de ogrencı varsa son kullanılan numarayı 1 artırıp donduren method
        return userRepository.getMaxStudentNumber() + 1 ;
    }

    public ResponseEntity<String> updateStudent(StudentRequestWithoutPassword studentRequest,
                                                HttpServletRequest request) {

       String userName = (String) request.getAttribute("username");
       User student = userRepository.findByUsernameEquals(userName);
       //!!! unique kontrol
        uniquePropertyValidator.checkUniqueProperties(student, studentRequest);

        student.setMotherName(studentRequest.getMotherName());
        student.setFatherName(studentRequest.getFatherName());
        student.setBirthDay(studentRequest.getBirthDay());
        student.setEmail(studentRequest.getEmail());
        student.setPhoneNumber(studentRequest.getPhoneNumber());
        student.setBirthPlace(studentRequest.getBirthPlace());
        student.setGender(studentRequest.getGender());
        student.setName(studentRequest.getName());
        student.setSurname(studentRequest.getSurname());
        student.setSsn(student.getSsn());

        userRepository.save(student);
        String message = SuccessMessages.USER_UPDATE;

        return ResponseEntity.ok(message);

    }

    public ResponseMessage<StudentResponse> updateStudentForManagers(Long userId, StudentRequest studentRequest) {

        User user = methodHelper.isUserExist(userId);
        //!!! istekten gelen user in rolu STudent mi ??
        methodHelper.checkRole(user, RoleType.STUDENT);
        //!!! unique
        uniquePropertyValidator.checkUniqueProperties(user, studentRequest);

        user.setName(studentRequest.getName());
        user.setSurname(studentRequest.getSurname());
        user.setBirthDay(studentRequest.getBirthDay());
        user.setBirthPlace(studentRequest.getBirthPlace());
        user.setSsn(studentRequest.getSsn());
        user.setEmail(studentRequest.getEmail());
        user.setPhoneNumber(studentRequest.getPhoneNumber());
        user.setGender(studentRequest.getGender());
        user.setMotherName(studentRequest.getMotherName());
        user.setFatherName(studentRequest.getFatherName());
        user.setPassword(passwordEncoder.encode(studentRequest.getPassword()));
        user.setAdvisorTeacherId(studentRequest.getAdvisorTeacherId());
        //user.setUserRole(userRoleService.getUserRole(RoleType.STUDENT));
        //user.setActive(true);
        //user.setStudentNumber(user.getStudentNumber());

        return ResponseMessage.<StudentResponse>builder()
                .message(SuccessMessages.STUDENT_UPDATE)
                .object(userMapper.mapUserToStudentResponse(userRepository.save(user)))
                .httpStatus(HttpStatus.OK)
                .build();
    }

    public ResponseMessage changeStatusOfStudent(Long studentId, boolean status) {

        User student = methodHelper.isUserExist(studentId);
        methodHelper.checkRole(student, RoleType.STUDENT);

        student.setActive(status);
        userRepository.save(student);

        return ResponseMessage.builder()
                .message("Student is " + (status ? "active" : "passive"))
                .httpStatus(HttpStatus.OK)
                .build();
    }

    public ResponseMessage<StudentResponse> addLessonProgramToStudent(String userName,
                                                                      ChooseLessonProgramWithId chooseLessonProgramWithId) {
        // !!! username kontrolu
        User student = methodHelper.isUserExistByUsername(userName);
        // !!! talep edilen lessonProgramlar getiriliyor
        Set<LessonProgram> lessonProgramSet =
                lessonProgramService.getLessonProgramById(chooseLessonProgramWithId.getLessonProgramId());
        // !!! mevcuttaki lessonProgramlar getiriliyor
        Set<LessonProgram> studentCurrentLessonProgram =  student.getLessonsProgramList();
        // !!! talep edilen ile mevcutta bir cakisma var mi kontrolu
        dateTimeValidator.checkLessonPrograms(studentCurrentLessonProgram, lessonProgramSet);

        studentCurrentLessonProgram.addAll(lessonProgramSet);
        student.setLessonsProgramList(studentCurrentLessonProgram);

        User savedStudent = userRepository.save(student);

        return ResponseMessage.<StudentResponse>builder()
                .message(SuccessMessages.LESSON_PROGRAM_ADD_TO_STUDENT)
                .object(userMapper.mapUserToStudentResponse(savedStudent))
                .httpStatus(HttpStatus.OK)
                .build();


    }
}













