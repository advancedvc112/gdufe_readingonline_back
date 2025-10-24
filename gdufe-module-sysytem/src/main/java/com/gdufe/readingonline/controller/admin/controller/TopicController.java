package com.gdufe.readingonline.controller.admin.controller;

import com.gdufe.readingonline.controller.admin.vo.TopicCreateRequestVO;
import com.gdufe.readingonline.controller.admin.vo.TopicCreateResponseVO;
import com.gdufe.readingonline.controller.admin.vo.TopicDetailResponseVO;
import com.gdufe.readingonline.controller.admin.vo.TopicListItemVO;
import com.gdufe.readingonline.controller.admin.vo.TopicUpdateRequestVO;
import com.gdufe.readingonline.service.topic.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图书专题管理Controller
 * 
 * @author gdufe
 * @date 2025
 */
@RestController
@RequestMapping("/gdufeReadingOnline/topic")
@CrossOrigin(origins = "*") // 允许跨域访问
public class TopicController {
    
    @Autowired
    private TopicService topicService;
    
    /**
     * 创建图书专题接口
     * 
     * @param requestVO 创建请求VO（包含HTML文本和创建人）
     * @return 创建结果
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createTopic(
            @RequestBody TopicCreateRequestVO requestVO) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 参数验证
            if (requestVO.getHtmlContent() == null || requestVO.getHtmlContent().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "HTML文本不能为空");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            if (requestVO.getCreator() == null || requestVO.getCreator().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "创建人不能为空");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            if (requestVO.getTopicTitle() == null || requestVO.getTopicTitle().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "专题标题不能为空");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            if (requestVO.getTopicBriefIntroduction() == null || requestVO.getTopicBriefIntroduction().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "专题简介不能为空");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            if (requestVO.getTopicCategory() == null) {
                result.put("code", 400);
                result.put("message", "专题分类不能为空");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            // 调用Service创建专题
            TopicCreateResponseVO responseVO = topicService.createTopic(requestVO);
            
            // 构建成功响应
            result.put("code", 200);
            result.put("message", "图书专题创建成功");
            result.put("data", responseVO);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            // 构建失败响应
            result.put("code", 500);
            result.put("message", "图书专题创建失败：" + e.getMessage());
            result.put("data", null);
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 根据专题ID获取专题详情
     * 
     * @param topicId 专题ID
     * @return 专题详情（包含HTML文本、发布时间、结束时间）
     */
    @GetMapping("/detail/{topicId}")
    public ResponseEntity<Map<String, Object>> getTopicDetail(
            @PathVariable("topicId") Long topicId) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 参数验证
            if (topicId == null) {
                result.put("code", 400);
                result.put("message", "专题ID不能为空");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            // 调用Service查询专题详情
            TopicDetailResponseVO responseVO = topicService.getTopicById(topicId);
            
            // 构建成功响应
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", responseVO);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            // 构建失败响应
            result.put("code", 500);
            result.put("message", "查询专题详情失败：" + e.getMessage());
            result.put("data", null);
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 获取最近发布的专题列表
     * 按发布时间倒序排序，最多返回4条记录
     * 
     * @return 专题列表
     */
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentTopics() {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 调用Service查询最近专题列表
            List<TopicListItemVO> topicList = topicService.getRecentTopics();
            
            // 构建成功响应
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", topicList);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            // 构建失败响应
            result.put("code", 500);
            result.put("message", "查询最近专题列表失败：" + e.getMessage());
            result.put("data", null);
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 更新专题详情接口
     * 
     * @param requestVO 更新请求VO（包含专题ID、HTML文本、创建人等）
     * @return 更新结果
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateTopic(
            @RequestBody TopicUpdateRequestVO requestVO) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 参数验证
            if (requestVO.getTopicId() == null) {
                result.put("code", 400);
                result.put("message", "专题ID不能为空");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            if (requestVO.getHtmlContent() == null || requestVO.getHtmlContent().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "HTML文本不能为空");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            if (requestVO.getCreator() == null || requestVO.getCreator().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "创建人不能为空");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            if (requestVO.getTopicTitle() == null || requestVO.getTopicTitle().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "专题标题不能为空");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            if (requestVO.getTopicBriefIntroduction() == null || requestVO.getTopicBriefIntroduction().trim().isEmpty()) {
                result.put("code", 400);
                result.put("message", "专题简介不能为空");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            if (requestVO.getTopicCategory() == null) {
                result.put("code", 400);
                result.put("message", "专题分类不能为空");
                result.put("data", null);
                return ResponseEntity.badRequest().body(result);
            }
            
            // 调用Service更新专题
            boolean updateSuccess = topicService.updateTopic(requestVO);
            
            // 构建成功响应
            result.put("code", 200);
            result.put("message", "图书专题更新成功");
            result.put("data", updateSuccess);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            // 构建失败响应
            result.put("code", 500);
            result.put("message", "图书专题更新失败：" + e.getMessage());
            result.put("data", null);
            
            return ResponseEntity.status(500).body(result);
        }
    }
}
