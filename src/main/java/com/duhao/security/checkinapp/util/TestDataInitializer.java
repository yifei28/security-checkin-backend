package com.duhao.security.checkinapp.util;

import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.WorkStatus;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.entity.WorkSite;
import com.duhao.security.checkinapp.repository.CheckinRepository;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.repository.WorkSiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("test-data") // 只在特定profile下运行
public class TestDataInitializer {

    @Bean
    CommandLineRunner initTestData(
            WorkSiteRepository siteRepository,
            SecurityGuardRepository guardRepository,
            CheckinRepository checkinRepository) {

        return args -> {
            System.out.println("========== 开始插入测试数据 ==========");

            // 1. 创建测试站点
            WorkSite site1 = new WorkSite("办公大楼A座", 39.9088, 116.3974, 100.0);
            WorkSite site2 = new WorkSite("办公大楼B座", 39.9120, 116.4010, 150.0);
            WorkSite site3 = new WorkSite("科技园C区", 39.9150, 116.4050, 200.0);

            site1 = siteRepository.save(site1);
            site2 = siteRepository.save(site2);
            site3 = siteRepository.save(site3);
            System.out.println("✓ 创建了3个测试站点");

            // 2. 创建测试保安
            List<SecurityGuard> guards = new ArrayList<>();

            SecurityGuard guard1 = new SecurityGuard();
            guard1.setName("张三");
            guard1.setPhoneNumber("13800138001");
            guard1.setEmployeeId("20250101-1234567-ABC001");
            guard1.setSite(site1);
            guard1.setOpenId("wx_openid_001");
            guards.add(guardRepository.save(guard1));

            SecurityGuard guard2 = new SecurityGuard();
            guard2.setName("李四");
            guard2.setPhoneNumber("13800138002");
            guard2.setEmployeeId("20250102-1234567-ABC002");
            guard2.setSite(site1);
            guard2.setOpenId("wx_openid_002");
            guards.add(guardRepository.save(guard2));

            SecurityGuard guard3 = new SecurityGuard();
            guard3.setName("王五");
            guard3.setPhoneNumber("13800138003");
            guard3.setEmployeeId("20250103-1234567-ABC003");
            guard3.setSite(site2);
            guard3.setOpenId("wx_openid_003");
            guards.add(guardRepository.save(guard3));

            SecurityGuard guard4 = new SecurityGuard();
            guard4.setName("赵六");
            guard4.setPhoneNumber("13800138004");
            guard4.setEmployeeId("20250104-1234567-ABC004");
            guard4.setSite(site2);
            guard4.setOpenId("wx_openid_004");
            guards.add(guardRepository.save(guard4));

            SecurityGuard guard5 = new SecurityGuard();
            guard5.setName("钱七");
            guard5.setPhoneNumber("13800138005");
            guard5.setEmployeeId("20250105-1234567-ABC005");
            guard5.setSite(site3);
            guard5.setOpenId("wx_openid_005");
            guards.add(guardRepository.save(guard5));

            System.out.println("✓ 创建了5个测试保安");

            // 3. 创建多样化的工作片段记录（使用新模型）
            List<CheckinRecord> records = new ArrayList<>();

            // 今天的已完成工作片段
            CheckinRecord record1 = new CheckinRecord(guards.get(0), site1, 39.9088, 116.3974, "https://example.com/faces/zhang_success.jpg");
            record1.clockOut(39.9089, 116.3975, "https://example.com/faces/zhang_end.jpg");
            records.add(record1);

            CheckinRecord record2 = new CheckinRecord(guards.get(1), site1, 39.9089, 116.3975, "https://example.com/faces/li_success.jpg");
            record2.setStartTime(LocalDateTime.now().minusHours(2));
            record2.clockOut(39.9090, 116.3976, null);
            records.add(record2);

            CheckinRecord record3 = new CheckinRecord(guards.get(2), site2, 39.9121, 116.4011, null);
            record3.setStartTime(LocalDateTime.now().minusHours(3));
            record3.clockOut(39.9122, 116.4012, "https://example.com/faces/wang_end.jpg");
            records.add(record3);

            // 超时的工作片段
            CheckinRecord record4 = new CheckinRecord(guards.get(3), site2, 39.9200, 116.4100, "https://example.com/faces/zhao_failed.jpg");
            record4.setStartTime(LocalDateTime.now().minusHours(20));
            record4.timeout();
            records.add(record4);

            CheckinRecord record5 = new CheckinRecord(guards.get(4), site3, 39.9300, 116.4200, null);
            record5.setStartTime(LocalDateTime.now().minusHours(18));
            record5.timeout();
            records.add(record5);

            // 当前在岗的工作片段
            CheckinRecord record6 = new CheckinRecord(guards.get(0), site1, 39.9088, 116.3974, "https://example.com/faces/zhang_active.jpg");
            record6.setStartTime(LocalDateTime.now().minusHours(4));
            // 不调用clockOut，保持ACTIVE状态
            records.add(record6);

            // 昨天的工作片段记录
            CheckinRecord record7 = new CheckinRecord(guards.get(0), site1, 39.9087, 116.3973, "https://example.com/faces/zhang_yesterday.jpg");
            record7.setStartTime(LocalDateTime.now().minusDays(1).minusHours(8));
            record7.setEndTime(LocalDateTime.now().minusDays(1));
            record7.setStatus(WorkStatus.COMPLETED);
            record7.calculateDuration();
            records.add(record7);

            CheckinRecord record8 = new CheckinRecord(guards.get(1), site1, 39.9090, 116.3976, null);
            record8.setStartTime(LocalDateTime.now().minusDays(1).minusHours(10));
            record8.setEndTime(LocalDateTime.now().minusDays(1).minusHours(2));
            record8.setStatus(WorkStatus.COMPLETED);
            record8.calculateDuration();
            records.add(record8);

            CheckinRecord record9 = new CheckinRecord(guards.get(2), site2, 39.9119, 116.4009, "https://example.com/faces/wang_yesterday.jpg");
            record9.setStartTime(LocalDateTime.now().minusDays(1).minusHours(11));
            record9.setEndTime(LocalDateTime.now().minusDays(1).minusHours(3));
            record9.setStatus(WorkStatus.COMPLETED);
            record9.calculateDuration();
            records.add(record9);

            // 前天的工作片段记录
            CheckinRecord record10 = new CheckinRecord(guards.get(0), site1, 39.9086, 116.3972, null);
            record10.setStartTime(LocalDateTime.now().minusDays(2).minusHours(9));
            record10.setEndTime(LocalDateTime.now().minusDays(2));
            record10.setStatus(WorkStatus.COMPLETED);
            record10.calculateDuration();
            records.add(record10);

            CheckinRecord record11 = new CheckinRecord(guards.get(1), site1, 39.9091, 116.3977, "https://example.com/faces/li_2days_ago.jpg");
            record11.setStartTime(LocalDateTime.now().minusDays(2).minusHours(9));
            record11.setEndTime(LocalDateTime.now().minusDays(2).minusHours(1));
            record11.setStatus(WorkStatus.COMPLETED);
            record11.calculateDuration();
            records.add(record11);

            // 一周内的各种记录
            CheckinRecord record12 = new CheckinRecord(guards.get(0), site1, 39.9088, 116.3974, null);
            record12.setStartTime(LocalDateTime.now().minusDays(3).minusHours(8));
            record12.setEndTime(LocalDateTime.now().minusDays(3));
            record12.setStatus(WorkStatus.COMPLETED);
            record12.calculateDuration();
            records.add(record12);

            CheckinRecord record13 = new CheckinRecord(guards.get(0), site1, 39.9087, 116.3973, "https://example.com/faces/zhang_4days.jpg");
            record13.setStartTime(LocalDateTime.now().minusDays(4).minusHours(8));
            record13.setEndTime(LocalDateTime.now().minusDays(4));
            record13.setStatus(WorkStatus.COMPLETED);
            record13.calculateDuration();
            records.add(record13);

            CheckinRecord record14 = new CheckinRecord(guards.get(0), site1, 39.9089, 116.3975, null);
            record14.setStartTime(LocalDateTime.now().minusDays(5).minusHours(20));
            record14.timeout();
            records.add(record14);

            CheckinRecord record15 = new CheckinRecord(guards.get(1), site1, 39.9090, 116.3976, null);
            record15.setStartTime(LocalDateTime.now().minusDays(3).minusHours(8));
            record15.setEndTime(LocalDateTime.now().minusDays(3));
            record15.setStatus(WorkStatus.COMPLETED);
            record15.calculateDuration();
            records.add(record15);

            CheckinRecord record16 = new CheckinRecord(guards.get(1), site1, 39.9088, 116.3974, "https://example.com/faces/li_4days.jpg");
            record16.setStartTime(LocalDateTime.now().minusDays(4).minusHours(8));
            record16.setEndTime(LocalDateTime.now().minusDays(4));
            record16.setStatus(WorkStatus.COMPLETED);
            record16.calculateDuration();
            records.add(record16);

            // 保存所有工作片段记录
            checkinRepository.saveAll(records);
            System.out.println("✓ 创建了" + records.size() + "条测试工作片段记录");

            // 打印统计信息
            System.out.println("\n========== 测试数据统计 ==========");
            System.out.println("站点总数: " + siteRepository.count());
            System.out.println("保安总数: " + guardRepository.count());
            System.out.println("工作片段总数: " + checkinRepository.count());

            // 按状态统计
            long activeCount = checkinRepository.findByStatus(WorkStatus.ACTIVE).size();
            long completedCount = checkinRepository.findByStatus(WorkStatus.COMPLETED).size();
            long timeoutCount = checkinRepository.findByStatus(WorkStatus.TIMEOUT).size();

            System.out.println("在岗中: " + activeCount + "条");
            System.out.println("已完成: " + completedCount + "条");
            System.out.println("超时: " + timeoutCount + "条");

            System.out.println("========== 测试数据插入完成 ==========\n");
        };
    }
}
