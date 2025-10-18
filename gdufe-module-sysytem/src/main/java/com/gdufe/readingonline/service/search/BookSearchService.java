package com.gdufe.readingonline.service.search;

import java.util.Map;

/**
 * 图书搜索服务接口
 * 
 * @author gdufe-advancedvc112
 * @date 2024
 */
public interface BookSearchService {
    
    /**
     * 按书名模糊搜索图书
     * 
     * @param name 搜索关键词
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 搜索结果，包含分页信息
     */
    Map<String, Object> searchBooksByName(String name, Integer page, Integer size);
}
