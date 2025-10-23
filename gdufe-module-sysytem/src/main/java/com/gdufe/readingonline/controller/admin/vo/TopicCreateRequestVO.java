package com.gdufe.readingonline.controller.admin.vo;

import lombok.Data;

/**
 * 图书专题创建请求VO
 * 
 * @author gdufe
 * @date 2025
 */
@Data
public class TopicCreateRequestVO {
    
    /**
     * 专题HTML文本（前端传递的JSON格式的HTML文本）
     */
    private String htmlContent;
    
    /**
     * 创建人
     */
    private String creator;
}

