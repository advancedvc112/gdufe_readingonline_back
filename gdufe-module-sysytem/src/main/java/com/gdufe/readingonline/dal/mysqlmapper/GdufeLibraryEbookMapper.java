package com.gdufe.readingonline.dal.mysqlmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdufe.readingonline.dal.dataobject.GdufeLibraryEbookDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 图书馆电子书Mapper接口
 * 
 * @author gdufe
 * @date 2024
 */
@Mapper
public interface GdufeLibraryEbookMapper extends BaseMapper<GdufeLibraryEbookDO> {
    
    /**
     * 根据ISBN查询电子书
     * 
     * @param bookIsbn ISBN号
     * @return 电子书信息
     */
    GdufeLibraryEbookDO selectByBookIsbn(@Param("bookIsbn") String bookIsbn);
    
    /**
     * 批量插入或更新电子书数据（基于ISBN）
     * 如果ISBN已存在则更新，否则插入
     * 
     * @param ebookList 电子书数据列表
     * @return 影响的记录数
     */
    int batchInsertOrUpdate(@Param("list") List<GdufeLibraryEbookDO> ebookList);
}
