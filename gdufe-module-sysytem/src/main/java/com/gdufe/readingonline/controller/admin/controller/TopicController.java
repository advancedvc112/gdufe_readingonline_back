package com.gdufe.readingonline.controller.admin.controller;

import com.gdufe.readingonline.controller.admin.vo.TopicCreateRequestVO;
import com.gdufe.readingonline.controller.admin.vo.TopicCreateResponseVO;
import com.gdufe.readingonline.service.topic.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
}
