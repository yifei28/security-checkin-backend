package com.duhao.security.checkinapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

/**
 * 通用分页信息类
 */
public class PaginationInfo {
    private long total;
    private int page;
    @JsonProperty("pageSize")
    private int pageSize;
    @JsonProperty("totalPages")
    private int totalPages;

    public PaginationInfo() {}

    public PaginationInfo(long total, int page, int pageSize, int totalPages) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
    }

    /**
     * 从 Spring Data Page 对象创建分页信息
     * @param springPage Spring Data 分页对象
     * @return PaginationInfo
     */
    public static PaginationInfo fromPage(Page<?> springPage) {
        return new PaginationInfo(
            springPage.getTotalElements(),
            springPage.getNumber() + 1,  // Spring 从0开始，转为从1开始
            springPage.getSize(),
            springPage.getTotalPages()
        );
    }

    // Getters and setters
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
