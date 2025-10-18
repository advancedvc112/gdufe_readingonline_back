package com.gdufe.readingonline.controller.admin.controller;

import com.gdufe.readingonline.service.search.BookSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 图书搜索管理Controller
 * 
 * @author gdufe
 * @date 2024
 */
@RestController
@RequestMapping("/gdufeReadingOnline/search/books")
@CrossOrigin(origins = "*") // 允许跨域访问
public class BookSearchController {
    
    @Autowired
    private BookSearchService bookSearchService;

    /**
     * 按书名模糊搜索图书接口
     * 
     * @param name 搜索关键词
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 搜索结果
     */
    @PostMapping("/name")
    public ResponseEntity<Map<String, Object>> searchBooksByName(
            @RequestParam("name") String name,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        
        try {
            // 参数验证
            if (name == null || name.trim().isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("code", 400);
                errorResult.put("message", "搜索关键词不能为空");
                errorResult.put("data", null);
                return ResponseEntity.badRequest().body(errorResult);
            }
            
            // 页码和大小验证
            if (page < 1) {
                page = 1;
            }
            if (size < 1 || size > 100) {
                size = 10; // 限制每页最大100条
            }
            
            // 调用Service进行搜索
            Map<String, Object> result = bookSearchService.searchBooksByName(name.trim(), page, size);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 500);
            errorResult.put("message", "搜索失败：" + e.getMessage());
            errorResult.put("data", null);
            return ResponseEntity.status(500).body(errorResult);
        }
    }
}
