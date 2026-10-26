package com.munot.hospital.repository;

import com.munot.hospital.entity.*;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Optional<Appointment> findByAppointmentCode(String code);
    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusNotIn(Long doctorId, LocalDate date, Collection<AppointmentStatus> statuses);
    boolean existsByPatientIdAndAppointmentDateAndStartTimeAndStatusNotIn(Long patientId, LocalDate date, LocalTime start, Collection<AppointmentStatus> statuses);
    List<Appointment> findByPatientIdOrderByAppointmentDateDescStartTimeDesc(Long patientId);
    List<Appointment> findByAppointmentDateOrderByStartTime(LocalDate date);
    List<Appointment> findByDoctorIdAndAppointmentDateOrderByStartTime(Long doctorId, LocalDate date);
    List<Appointment> findByDoctorIdAndAppointmentDateGreaterThanEqualAndStatusInOrderByAppointmentDateAscStartTimeAsc(Long doctorId, LocalDate from, Collection<AppointmentStatus> statuses);
    List<Appointment> findByAppointmentDateBeforeAndStatusIn(LocalDate date, Collection<AppointmentStatus> statuses);
    List<Appointment> findByAppointmentDateBetween(LocalDate from, LocalDate to);
    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);
    long countByAppointmentDate(LocalDate date);
    long countByPatientIdAndStatusInAndAppointmentDateGreaterThanEqual(Long patientId, Collection<AppointmentStatus> statuses, LocalDate from);
    List<Appointment> findByStatusAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(AppointmentStatus status, LocalDate from);
}
