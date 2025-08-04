package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.dto.CheckinResult;
import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;


@RestController
@RequestMapping(value="/api", produces = "application/json; charset=UTF-8")
public class CheckinController {

    private final CheckinRepository checkinRepository;
    private final SecurityGuardRepository guardRepository;

    @Autowired
    public CheckinController(CheckinRepository repository, SecurityGuardRepository guardRepository) {
        this.checkinRepository = repository;
        this.guardRepository = guardRepository;
    }

    @PostMapping(path="/checkin/validate")
    public ResponseEntity<CheckinResult> prepareCheckin(@RequestBody CheckinRecord record) {
        CheckinResult checkinResult = validateCheckin(record);
        if (!checkinResult.isSuccess()){
            return ResponseEntity.badRequest().body(checkinResult);
        }
        return ResponseEntity.ok().body(checkinResult);
    }

    @PostMapping(path="/checkin")
    public ResponseEntity<CheckinResult> checkin(@RequestBody CheckinRecord record) {
        CheckinResult checkinResult = validateCheckin(record);
        if (!checkinResult.isSuccess()) {
            return ResponseEntity.badRequest().body(checkinResult);
        }
        record.setPeriod(getCurrentPeriod(LocalDateTime.now()));
        record.setDate(LocalDate.now());
        record.setTimestamp(LocalDateTime.now());
        checkinRepository.save(record);
        return ResponseEntity.ok().body(checkinResult);
    }

    private CheckinResult validateCheckin(CheckinRecord record){
        // 1. 查询是否存在这个保安
        SecurityGuard guard = guardRepository.findByEmployeeId(record.getEmployeeId());

        if (guard == null) {
            return CheckinResult.fail("没有找到信息，请确认姓名和手机号或请联系管理员登记信息");
        }

        // 2. 检查是否分配单位
        WorkSite site = guard.getSite();
        if (site == null) {
            return CheckinResult.fail("尚未分配工作单位，无法签到");
        }

        // 3. 检查是否在单位附近
        if (record.getLatitude() == null || record.getLongitude() == null) {
            return CheckinResult.fail("位置不准确，请检查定位");
        }

        if (!site.isInRange(record.getLatitude(), record.getLongitude())) {
            return CheckinResult.fail("当前位置距离单位过远，无法签到");
        }

        // 4. 检查是否在正确的时间
        String period = getCurrentPeriod(LocalDateTime.now());
        if (period == null) {
            return CheckinResult.fail("当前时间不在签到时间段内");
        }

        // 5. 检查是否已经签过到
        LocalDate today = LocalDate.now();
        CheckinRecord existedRecord = checkinRepository.findByEmployeeIdAndPeriodAndDate(record.getEmployeeId(), period, today);
        if (existedRecord != null) {
            return CheckinResult.fail("您今天在这个时间段已经签过到了");
        }

        return CheckinResult.ok();
    }


    private boolean isBetweenInclusive(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && !time.isAfter(end);
    }

    private String getCurrentPeriod(LocalDateTime checkinTime) {
        LocalTime time = checkinTime.toLocalTime();
        if (isBetweenInclusive(time, LocalTime.of(8, 0), LocalTime.of(11, 0))) {
            return "上午";
        } else if (isBetweenInclusive(time, LocalTime.of(13, 0), LocalTime.of(15, 0))) {
            return "下午";
        }
        return null;
    }

    @GetMapping(path="/checkin")
    public List<CheckinRecord> getAll() {
        return checkinRepository.findAll();
    }
}