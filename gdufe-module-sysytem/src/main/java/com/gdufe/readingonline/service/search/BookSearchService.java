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
    
    /**
     * 随机获取图书
     * 
     * @return 随机图书列表（包含书名、作者、ISBN、简介）
     */
    Map<String, Object> getRandomBooks();
    
    /**
     * 根据ISBN和来源精确查询图书
     * 
     * @param isbn 图书ISBN号
     * @param source 图书来源（0-畅想之星, 1-京东）
     * @return 图书详细信息
     */
    Map<String, Object> getBookByIsbnAndSource(String isbn, Integer source);
    
    /**
     * 按分类查询图书
     * 
     * @param category 图书主分类（0-22）
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 该分类的图书列表，包含分页信息
     */
    Map<String, Object> searchBooksByCategory(Integer category, Integer page, Integer size);
}
