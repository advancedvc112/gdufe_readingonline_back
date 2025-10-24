package com.gdufe.readingonline.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图书专题数据对象
 * 
 * @author gdufe
 * @date 2025
 */
@Data
@TableName("gdufe_topic_ebook")
public class GdufeTopicEbookDO {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 专题HTML文本
     */
    private String bookTopicHtml;
    
    /**
     * 专题标题
     */
    private String bookTopicTitle;
    
    /**
     * 专题简介
     */
    private String bookTopicBriefIntroduction;
    
    /**
     * 专题分类
     */
    private Integer bookTopicCatagory;
    
    /**
     * 发布状态（0:草稿, 1:已发布, 2:已下线）
     */
    private Integer bookTopicStatus;
    
    /**
     * 创建人
     */
    private String bookTopicCreator;
    
    /**
     * 逻辑删除（0:未删除, 1:已删除）
     */
    private Integer isDeleted;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
}
