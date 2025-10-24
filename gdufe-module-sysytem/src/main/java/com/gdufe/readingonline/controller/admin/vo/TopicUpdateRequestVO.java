package com.gdufe.readingonline.controller.admin.vo;

import lombok.Data;

/**
 * 图书专题更新请求VO
 * 
 * @author gdufe
 * @date 2025
 */
@Data
public class TopicUpdateRequestVO {
    
    /**
     * 专题ID
     */
    private Long topicId;
    
    /**
     * 专题HTML文本
     */
    private String htmlContent;
    
    /**
     * 创建人
     */
    private String creator;
    
    /**
     * 专题标题
     */
    private String topicTitle;
    
    /**
     * 专题简介
     */
    private String topicBriefIntroduction;
    
    /**
     * 专题分类
     */
    private Integer topicCategory;
}

