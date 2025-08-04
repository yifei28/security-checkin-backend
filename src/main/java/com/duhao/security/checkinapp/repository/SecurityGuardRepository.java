package com.duhao.security.checkinapp.repository;

import com.duhao.security.checkinapp.entity.SecurityGuard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityGuardRepository extends JpaRepository<SecurityGuard, Long> {
    SecurityGuard findByPhoneNumber(String phoneNumber);
    SecurityGuard findByEmployeeId(String employeeId);
    SecurityGuard findByOpenId(String openid);
}
