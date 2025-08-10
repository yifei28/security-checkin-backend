package com.duhao.security.checkinapp.repository;

import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.WorkSite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityGuardRepository extends JpaRepository<SecurityGuard, Long> {
    SecurityGuard findByPhoneNumber(String phoneNumber);
    SecurityGuard findByEmployeeId(String employeeId);
    SecurityGuard findByOpenId(String openid);
    List<SecurityGuard> findBySite(WorkSite site);
}
