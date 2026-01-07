package com.duhao.security.checkinapp.repository;

import com.duhao.security.checkinapp.entity.CheckinLocation;
import com.duhao.security.checkinapp.entity.WorkSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CheckinLocationRepository extends JpaRepository<CheckinLocation, Long> {

    /**
     * 根据单位ID查询所有签到地点
     */
    List<CheckinLocation> findBySiteId(Long siteId);

    /**
     * 根据单位查询所有签到地点
     */
    List<CheckinLocation> findBySite(WorkSite site);

    /**
     * 根据单位ID删除所有签到地点
     */
    @Modifying
    @Query("DELETE FROM CheckinLocation c WHERE c.site.id = ?1")
    void deleteBySiteId(Long siteId);

    /**
     * 统计单位的签到地点数量
     */
    long countBySiteId(Long siteId);

    /**
     * 检查签到地点是否属于指定单位
     */
    boolean existsByIdAndSiteId(Long id, Long siteId);
}
