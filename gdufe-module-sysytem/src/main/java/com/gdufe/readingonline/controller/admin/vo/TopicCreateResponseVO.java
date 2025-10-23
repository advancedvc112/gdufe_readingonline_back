package com.gdufe.readingonline.controller.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图书专题创建响应VO
 * 
 * @author gdufe
 * @date 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicCreateResponseVO {
    
    /**
     * 专题编号
     */
    private String topicNo;
    
    /**
     * 专题ID
     */
    private Long topicId;
    
    /**
     * 创建时间
     */
    private String createTime;
}

