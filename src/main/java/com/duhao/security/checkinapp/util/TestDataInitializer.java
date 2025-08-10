package com.duhao.security.checkinapp.util;

import com.duhao.security.checkinapp.entity.CheckinRecord;
import com.duhao.security.checkinapp.entity.CheckinStatus;
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
            
            // 3. 创建多样化的签到记录
            List<CheckinRecord> records = new ArrayList<>();
            
            // 今天的成功签到
            records.add(new CheckinRecord(
                guards.get(0), site1, 39.9088, 116.3974,
                LocalDateTime.now(), 
                "https://example.com/faces/zhang_success.jpg",
                CheckinStatus.SUCCESS, null
            ));
            
            records.add(new CheckinRecord(
                guards.get(1), site1, 39.9089, 116.3975,
                LocalDateTime.now().minusHours(2),
                "https://example.com/faces/li_success.jpg",
                CheckinStatus.SUCCESS, null
            ));
            
            records.add(new CheckinRecord(
                guards.get(2), site2, 39.9121, 116.4011,
                LocalDateTime.now().minusHours(3),
                null,
                CheckinStatus.SUCCESS, null
            ));
            
            // 失败的签到（位置超出范围）
            records.add(new CheckinRecord(
                guards.get(3), site2, 39.9200, 116.4100,
                LocalDateTime.now().minusHours(1),
                "https://example.com/faces/zhao_failed.jpg",
                CheckinStatus.FAILED, "签到位置超出允许范围（实际距离：850米）"
            ));
            
            records.add(new CheckinRecord(
                guards.get(4), site3, 39.9300, 116.4200,
                LocalDateTime.now().minusMinutes(30),
                null,
                CheckinStatus.FAILED, "签到位置超出允许范围（实际距离：1500米）"
            ));
            
            // 待处理的签到
            records.add(new CheckinRecord(
                guards.get(0), site1, 39.9088, 116.3974,
                LocalDateTime.now().minusHours(4),
                "https://example.com/faces/zhang_pending.jpg",
                CheckinStatus.PENDING, "人脸识别中，请稍候"
            ));
            
            // 昨天的签到记录
            records.add(new CheckinRecord(
                guards.get(0), site1, 39.9087, 116.3973,
                LocalDateTime.now().minusDays(1),
                "https://example.com/faces/zhang_yesterday.jpg",
                CheckinStatus.SUCCESS, null
            ));
            
            records.add(new CheckinRecord(
                guards.get(1), site1, 39.9090, 116.3976,
                LocalDateTime.now().minusDays(1).minusHours(2),
                null,
                CheckinStatus.SUCCESS, null
            ));
            
            records.add(new CheckinRecord(
                guards.get(2), site2, 39.9119, 116.4009,
                LocalDateTime.now().minusDays(1).minusHours(3),
                "https://example.com/faces/wang_yesterday.jpg",
                CheckinStatus.SUCCESS, null
            ));
            
            // 前天的签到记录
            records.add(new CheckinRecord(
                guards.get(0), site1, 39.9086, 116.3972,
                LocalDateTime.now().minusDays(2),
                null,
                CheckinStatus.SUCCESS, null
            ));
            
            records.add(new CheckinRecord(
                guards.get(1), site1, 39.9091, 116.3977,
                LocalDateTime.now().minusDays(2).minusHours(1),
                "https://example.com/faces/li_2days_ago.jpg",
                CheckinStatus.SUCCESS, null
            ));
            
            // 一周内的各种记录
            records.add(new CheckinRecord(
                guards.get(0), site1, 39.9088, 116.3974,
                LocalDateTime.now().minusDays(3),
                null,
                CheckinStatus.SUCCESS, null
            ));
            
            records.add(new CheckinRecord(
                guards.get(0), site1, 39.9087, 116.3973,
                LocalDateTime.now().minusDays(4),
                "https://example.com/faces/zhang_4days.jpg",
                CheckinStatus.SUCCESS, null
            ));
            
            records.add(new CheckinRecord(
                guards.get(0), site1, 39.9089, 116.3975,
                LocalDateTime.now().minusDays(5),
                null,
                CheckinStatus.FAILED, "当前时间不在签到时间段内"
            ));
            
            records.add(new CheckinRecord(
                guards.get(1), site1, 39.9090, 116.3976,
                LocalDateTime.now().minusDays(3),
                null,
                CheckinStatus.SUCCESS, null
            ));
            
            records.add(new CheckinRecord(
                guards.get(1), site1, 39.9088, 116.3974,
                LocalDateTime.now().minusDays(4),
                "https://example.com/faces/li_4days.jpg",
                CheckinStatus.SUCCESS, null
            ));
            
            // 保存所有签到记录
            checkinRepository.saveAll(records);
            System.out.println("✓ 创建了" + records.size() + "条测试签到记录");
            
            // 打印统计信息
            System.out.println("\n========== 测试数据统计 ==========");
            System.out.println("站点总数: " + siteRepository.count());
            System.out.println("保安总数: " + guardRepository.count());
            System.out.println("签到记录总数: " + checkinRepository.count());
            
            // 按状态统计
            long successCount = checkinRepository.findByStatus(CheckinStatus.SUCCESS).size();
            long failedCount = checkinRepository.findByStatus(CheckinStatus.FAILED).size();
            long pendingCount = checkinRepository.findByStatus(CheckinStatus.PENDING).size();
            
            System.out.println("成功签到: " + successCount + "条");
            System.out.println("失败签到: " + failedCount + "条");
            System.out.println("待处理签到: " + pendingCount + "条");
            
            System.out.println("========== 测试数据插入完成 ==========\n");
        };
    }
}