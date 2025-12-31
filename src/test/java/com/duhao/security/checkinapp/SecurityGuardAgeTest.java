package com.duhao.security.checkinapp;

import com.duhao.security.checkinapp.entity.SecurityGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

public class SecurityGuardAgeTest {

    @Test
    @DisplayName("测试年龄计算功能 - 基于生日自动计算")
    void testAgeCalculation() {
        // 创建一个1990年5月15日生的安保员
        SecurityGuard guard = new SecurityGuard();
        guard.setName("测试员工");
        guard.setBirthDate(LocalDate.of(1990, 5, 15));

        // 验证年龄计算（2025年应该是35岁）
        Integer age = guard.getAge();
        assertNotNull(age, "年龄不应为null");
        assertTrue(age >= 34 && age <= 35, "年龄应在34-35岁之间");

        System.out.println("员工：" + guard.getName());
        System.out.println("生日：" + guard.getBirthDate());
        System.out.println("年龄：" + age + "岁");
    }

    @Test
    @DisplayName("测试指定日期年龄计算")
    void testAgeAtSpecificDate() {
        SecurityGuard guard = new SecurityGuard();
        guard.setBirthDate(LocalDate.of(1990, 5, 15));

        // 测试2020年时的年龄
        Integer ageIn2020 = guard.getAgeAt(LocalDate.of(2020, 6, 1));
        assertEquals(30, ageIn2020, "2020年6月1日时应该30岁");

        // 测试生日前和生日后
        Integer ageBeforeBirthday = guard.getAgeAt(LocalDate.of(2020, 5, 14));
        Integer ageAfterBirthday = guard.getAgeAt(LocalDate.of(2020, 5, 16));

        assertEquals(29, ageBeforeBirthday, "生日前应该29岁");
        assertEquals(30, ageAfterBirthday, "生日后应该30岁");

        System.out.println("2020年5月14日（生日前）年龄：" + ageBeforeBirthday + "岁");
        System.out.println("2020年5月16日（生日后）年龄：" + ageAfterBirthday + "岁");
    }

    @Test
    @DisplayName("测试生日为空的情况")
    void testNullBirthDate() {
        SecurityGuard guard = new SecurityGuard();
        guard.setName("无生日员工");
        // 不设置生日

        assertNull(guard.getAge(), "生日为空时年龄应为null");
        assertNull(guard.getAgeAt(LocalDate.now()), "生日为空时指定日期年龄也应为null");

        System.out.println("无生日员工年龄：" + guard.getAge());
    }
}