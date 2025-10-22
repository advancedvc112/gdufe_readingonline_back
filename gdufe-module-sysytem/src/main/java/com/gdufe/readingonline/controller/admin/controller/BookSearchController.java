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
 * @author gdufe-advancedvc112
 * @date 2025
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
    @GetMapping("/name")
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

    /**
     * 随机获取4本图书接口
     * 
     * @return 随机图书列表（包含书名、作者、ISBN、简介）
     */
    @GetMapping("/random")
    public ResponseEntity<Map<String, Object>> getRandomBooks() {
        try {
            // 调用Service获取随机图书
            Map<String, Object> result = bookSearchService.getRandomBooks();
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 500);
            errorResult.put("message", "获取随机图书失败：" + e.getMessage());
            errorResult.put("data", null);
            return ResponseEntity.status(500).body(errorResult);
        }
    }
    
    /**
     * 根据ISBN和来源精确查询图书
     * 
     * @param isbn 图书ISBN号
     * @param source 图书来源（0-畅想之星, 1-京东）
     * @return 图书详细信息
     */
    @GetMapping("/detail")
    public ResponseEntity<Map<String, Object>> getBookDetail(
            @RequestParam("isbn") String isbn,
            @RequestParam("source") Integer source) {
        
        try {
            // 参数验证
            if (isbn == null || isbn.trim().isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("code", 400);
                errorResult.put("message", "ISBN不能为空");
                errorResult.put("data", null);
                return ResponseEntity.badRequest().body(errorResult);
            }
            
            if (source == null || (source != 0 && source != 1)) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("code", 400);
                errorResult.put("message", "来源参数错误，必须为0（畅想之星）或1（京东）");
                errorResult.put("data", null);
                return ResponseEntity.badRequest().body(errorResult);
            }
            
            // 调用Service进行精确查询
            Map<String, Object> result = bookSearchService.getBookByIsbnAndSource(isbn.trim(), source);
            
            // 根据结果code返回相应的HTTP状态码
            Integer code = (Integer) result.get("code");
            if (code == 404) {
                return ResponseEntity.status(404).body(result);
            } else if (code == 500) {
                return ResponseEntity.status(500).body(result);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 500);
            errorResult.put("message", "查询失败：" + e.getMessage());
            errorResult.put("data", null);
            return ResponseEntity.status(500).body(errorResult);
        }
    }
}
