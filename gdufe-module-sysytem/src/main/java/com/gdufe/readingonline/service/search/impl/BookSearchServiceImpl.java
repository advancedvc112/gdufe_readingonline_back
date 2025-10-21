package com.gdufe.readingonline.service.search.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdufe.readingonline.dal.dataobject.GdufeLibraryEbookDO;
import com.gdufe.readingonline.dal.mysqlmapper.GdufeLibraryEbookMapper;
import com.gdufe.readingonline.service.search.BookSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图书搜索服务实现类
 * 
 * @author gdufe
 * @date 2024
 */
@Service
public class BookSearchServiceImpl implements BookSearchService {
    
    @Autowired
    private GdufeLibraryEbookMapper gdufeLibraryEbookMapper;
    
    @Override
    public Map<String, Object> searchBooksByName(String name, Integer page, Integer size) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 创建分页对象
            Page<GdufeLibraryEbookDO> pageParam = new Page<>(page, size);
            
            // 构建查询条件：书名模糊匹配，且未删除
            LambdaQueryWrapper<GdufeLibraryEbookDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.like(GdufeLibraryEbookDO::getBookName, name)
                       .eq(GdufeLibraryEbookDO::getIsDeleted, 0)
                       .orderByDesc(GdufeLibraryEbookDO::getCreateTime); // 按创建时间倒序
            
            // 执行分页查询
            IPage<GdufeLibraryEbookDO> pageResult = gdufeLibraryEbookMapper.selectPage(pageParam, queryWrapper);
            
            // 构建返回结果
            result.put("code", 200);
            result.put("message", "搜索成功");
            
            // 分页信息
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("current", pageResult.getCurrent());        // 当前页
            pageInfo.put("size", pageResult.getSize());              // 每页大小
            pageInfo.put("total", pageResult.getTotal());            // 总记录数
            pageInfo.put("pages", pageResult.getPages());            // 总页数
            pageInfo.put("hasNext", pageResult.getCurrent() < pageResult.getPages());           // 是否有下一页
            pageInfo.put("hasPrevious", pageResult.getCurrent() > 1);   // 是否有上一页
            
            // 数据信息
            Map<String, Object> data = new HashMap<>();
            data.put("list", pageResult.getRecords());               // 数据列表
            data.put("pageInfo", pageInfo);                          // 分页信息
            
            result.put("data", data);
            
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "搜索失败：" + e.getMessage());
            result.put("data", null);
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> getRandomBooks() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 从数据库随机查询4本书
            List<GdufeLibraryEbookDO> randomBooks = gdufeLibraryEbookMapper.selectRandomBooks(4);
            
            // 构建返回数据，只包含书名、作者、ISBN、简介
            List<Map<String, Object>> bookList = new ArrayList<>();
            for (GdufeLibraryEbookDO book : randomBooks) {
                Map<String, Object> bookInfo = new HashMap<>();
                bookInfo.put("bookName", book.getBookName());
                bookInfo.put("bookAuthor", book.getBookAuthor());
                bookInfo.put("bookIsbn", book.getBookIsbn());
                bookInfo.put("bookBriefIntroduction", book.getBookBriefIntroduction());
                bookList.add(bookInfo);
            }
            
            // 构建返回结果
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", bookList);
            
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "获取失败：" + e.getMessage());
            result.put("data", null);
        }
        
        return result;
    }
}
