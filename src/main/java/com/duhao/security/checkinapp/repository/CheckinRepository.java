package com.duhao.security.checkinapp.repository;

import com.duhao.security.checkinapp.entity.CheckinRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface CheckinRepository extends JpaRepository<CheckinRecord, Long> {
    CheckinRecord findByEmployeeIdAndPeriodAndDate(String employeeId, String period, LocalDate date);
}
