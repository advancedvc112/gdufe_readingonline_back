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
            
            // 构建查询条件：书名模糊匹配，且未删除，只查询需要的5个字段
            LambdaQueryWrapper<GdufeLibraryEbookDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(GdufeLibraryEbookDO::getBookName, 
                              GdufeLibraryEbookDO::getBookIsbn,
                              GdufeLibraryEbookDO::getBookAuthor,
                              GdufeLibraryEbookDO::getBookPress,
                              GdufeLibraryEbookDO::getBookUrl,
                              GdufeLibraryEbookDO::getBookSource,
                              GdufeLibraryEbookDO::getBookPictureUrl)
                       .like(GdufeLibraryEbookDO::getBookName, name)
                       .eq(GdufeLibraryEbookDO::getIsDeleted, 0);
            
            // 执行分页查询
            IPage<GdufeLibraryEbookDO> pageResult = gdufeLibraryEbookMapper.selectPage(pageParam, queryWrapper);
            
            // 构建返回数据，只包含书名、ISBN、作者、来源平台、封面图片URL
            List<Map<String, Object>> bookList = new ArrayList<>();
            for (GdufeLibraryEbookDO book : pageResult.getRecords()) {
                Map<String, Object> bookInfo = new HashMap<>();
                bookInfo.put("bookName", book.getBookName());
                bookInfo.put("bookIsbn", book.getBookIsbn());
                bookInfo.put("bookAuthor", book.getBookAuthor());
                bookInfo.put("bookPress", book.getBookPress());
                bookInfo.put("bookUrl", book.getBookUrl());
                // 来源平台：0-畅想之星, 1-京东
                String bookSource = book.getBookSource() == 0 ? "畅想之星" : "京东";
                bookInfo.put("bookSource", bookSource);
                bookInfo.put("bookPictureUrl", book.getBookPictureUrl());
                bookList.add(bookInfo);
            }
            
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
            data.put("list", bookList);                              // 数据列表（只包含4个字段）
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
            // 使用 QueryWrapper 从数据库随机查询4本书
            LambdaQueryWrapper<GdufeLibraryEbookDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(GdufeLibraryEbookDO::getBookName,
                              GdufeLibraryEbookDO::getBookAuthor,
                              GdufeLibraryEbookDO::getBookIsbn,
                              GdufeLibraryEbookDO::getBookPress,
                              GdufeLibraryEbookDO::getBookUrl,
                              GdufeLibraryEbookDO::getBookSource,
                              GdufeLibraryEbookDO::getBookPictureUrl)
                       .eq(GdufeLibraryEbookDO::getIsDeleted, 0)
                       .last("ORDER BY RAND() LIMIT 4");
            
            List<GdufeLibraryEbookDO> randomBooks = gdufeLibraryEbookMapper.selectList(queryWrapper);
            
            // 构建返回数据，只包含书名、作者、ISBN、出版社、链接、来源、封面图片URL
            List<Map<String, Object>> bookList = new ArrayList<>();
            for (GdufeLibraryEbookDO book : randomBooks) {
                Map<String, Object> bookInfo = new HashMap<>();
                bookInfo.put("bookName", book.getBookName());
                bookInfo.put("bookAuthor", book.getBookAuthor());
                bookInfo.put("bookIsbn", book.getBookIsbn());
                bookInfo.put("bookPress", book.getBookPress());
                bookInfo.put("bookUrl", book.getBookUrl());
                bookInfo.put("bookSource", book.getBookSource());
                bookInfo.put("bookPictureUrl", book.getBookPictureUrl());
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
    
    @Override
    public Map<String, Object> getBookByIsbnAndSource(String isbn, Integer source) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 构建查询条件：ISBN精确匹配、来源匹配、未删除
            LambdaQueryWrapper<GdufeLibraryEbookDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(GdufeLibraryEbookDO::getBookName,
                              GdufeLibraryEbookDO::getBookIsbn,
                              GdufeLibraryEbookDO::getBookAuthor,
                              GdufeLibraryEbookDO::getBookPress,
                              GdufeLibraryEbookDO::getBookPublicationTime,
                              GdufeLibraryEbookDO::getBookUrl,
                              GdufeLibraryEbookDO::getBookBriefIntroduction,
                              GdufeLibraryEbookDO::getBookPictureUrl,
                              GdufeLibraryEbookDO::getBookSource)
                       .eq(GdufeLibraryEbookDO::getBookIsbn, isbn)
                       .eq(GdufeLibraryEbookDO::getBookSource, source)
                       .eq(GdufeLibraryEbookDO::getIsDeleted, 0);
            
            // 执行查询
            GdufeLibraryEbookDO book = gdufeLibraryEbookMapper.selectOne(queryWrapper);
            
            if (book == null) {
                result.put("code", 404);
                result.put("message", "未找到匹配的图书");
                result.put("data", null);
            } else {
                // 构建返回数据
                Map<String, Object> bookInfo = new HashMap<>();
                bookInfo.put("bookName", book.getBookName());
                bookInfo.put("bookIsbn", book.getBookIsbn());
                bookInfo.put("bookAuthor", book.getBookAuthor());
                bookInfo.put("bookPress", book.getBookPress());
                bookInfo.put("bookPublicationTime", book.getBookPublicationTime());
                bookInfo.put("bookUrl", book.getBookUrl());
                bookInfo.put("bookBriefIntroduction", book.getBookBriefIntroduction());
                bookInfo.put("bookPictureUrl", book.getBookPictureUrl());
                // 来源平台：0-畅想之星, 1-京东
                String bookSource = book.getBookSource() == 0 ? "畅想之星" : "京东";
                bookInfo.put("bookSource", bookSource);
                
                result.put("code", 200);
                result.put("message", "查询成功");
                result.put("data", bookInfo);
            }
            
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> searchBooksByCategory(Integer category, Integer page, Integer size) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 创建分页对象
            Page<GdufeLibraryEbookDO> pageParam = new Page<>(page, size);
            
            // 构建查询条件：主分类匹配，且未删除
            LambdaQueryWrapper<GdufeLibraryEbookDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(GdufeLibraryEbookDO::getBookName, 
                              GdufeLibraryEbookDO::getBookIsbn,
                              GdufeLibraryEbookDO::getBookAuthor,
                              GdufeLibraryEbookDO::getBookPress,
                              GdufeLibraryEbookDO::getBookUrl,
                              GdufeLibraryEbookDO::getBookPictureUrl,
                              GdufeLibraryEbookDO::getBookBriefIntroduction)
                       .eq(GdufeLibraryEbookDO::getBookPrimaryClassification, category)
                       .eq(GdufeLibraryEbookDO::getIsDeleted, 0);
            
            // 执行分页查询
            IPage<GdufeLibraryEbookDO> pageResult = gdufeLibraryEbookMapper.selectPage(pageParam, queryWrapper);
            
            // 构建返回数据
            List<Map<String, Object>> bookList = new ArrayList<>();
            for (GdufeLibraryEbookDO book : pageResult.getRecords()) {
                Map<String, Object> bookInfo = new HashMap<>();
                bookInfo.put("bookName", book.getBookName());
                bookInfo.put("bookIsbn", book.getBookIsbn());
                bookInfo.put("bookAuthor", book.getBookAuthor());
                bookInfo.put("bookPress", book.getBookPress());
                bookInfo.put("bookUrl", book.getBookUrl());
                bookInfo.put("bookPictureUrl", book.getBookPictureUrl());
                bookInfo.put("bookBriefIntroduction", book.getBookBriefIntroduction()); // 简介
                bookList.add(bookInfo);
            }
            
            // 构建返回结果
            result.put("code", 200);
            result.put("message", "查询成功");
            
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
            data.put("list", bookList);                              // 数据列表
            data.put("pageInfo", pageInfo);                          // 分页信息
            
            result.put("data", data);
            
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
            result.put("data", null);
        }
        
        return result;
    }
}
