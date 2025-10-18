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
     * 批量插入电子书数据
     * 
     * @param ebookList 电子书数据列表
     * @return 插入成功的记录数
     */
    int batchInsert(@Param("list") List<GdufeLibraryEbookDO> ebookList);
    
    /**
     * 根据ISBN查询电子书
     * 
     * @param bookIsbn ISBN号
     * @return 电子书信息
     */
    GdufeLibraryEbookDO selectByBookIsbn(@Param("bookIsbn") String bookIsbn);
}
