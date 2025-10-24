package com.gdufe.readingonline.controller.admin.vo;

import lombok.Data;

/**
 * 图书专题列表项VO
 * 
 * @author gdufe
 * @date 2025
 */
@Data
public class TopicListItemVO {
    
    /**
     * 专题ID
     */
    private Long topicId;
    
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
    
    /**
     * 专题HTML文本
     */
    private String htmlContent;
    
    /**
     * 发布时间
     */
    private String publishTime;
    
    /**
     * 结束时间
     */
    private String endTime;
}

