package com.gdufe.readingonline.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书馆电子书数据对象
 * 
 * @author gdufe
 * @date 2024
 */
@Data
@TableName("gdufe_library_ebook")
public class GdufeLibraryEbookDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 书名
     */
    private String bookName;
    
    /**
     * 图书条码
     */
    private String bookIsbn;
    
    /**
     * 作者
     */
    private String bookAuthor;
    
    /**
     * 图书出版社
     */
    private String bookPress;
    
    /**
     * 图书出版时间
     */
    private LocalDate bookPublicationTime;
    
    /**
     * 图书链接
     */
    private String bookUrl;
    
    /**
     * 图书简介
     */
    private String bookBriefIntroduction;
    
    /**
     * 图书中图法分类
     */
    private String bookChineseLibraryClassification;
    
    /**
     * 图书学科分类
     */
    private String bookSubjectClassification;
    
    /**
     * 图书一级分类: 0-空白, 1-社会科学, 2-小说, 3-教材教辅, 4-科学新知, 5-文学, 6-经济管理, 7-少儿, 8-进口原版, 9-计算机, 10-生活休闲, 11-成功励志, 12-历史考古, 13-艺术摄影, 14-外语学习, 15-政治军事, 16-人物传记, 17-家教育儿, 18-中外名著, 19-漫画杂志, 20-职场进阶, 21-互联网+, 22-影视原著
     */
    private Integer bookPrimaryClassification;
    
    /**
     * 图书二级分类
     */
    private String bookSecondaryClassification;
    
    /**
     * 图书封面图
     */
    private String bookPictureUrl;
    
    /**
     * 书籍来源（0:畅想之星, 1:京东）
     */
    private Integer bookSource;
    
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
}
