package com.gdufe.readingonline.service.topic.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gdufe.readingonline.controller.admin.vo.TopicCreateRequestVO;
import com.gdufe.readingonline.controller.admin.vo.TopicCreateResponseVO;
import com.gdufe.readingonline.controller.admin.vo.TopicDetailResponseVO;
import com.gdufe.readingonline.controller.admin.vo.TopicListItemVO;
import com.gdufe.readingonline.controller.admin.vo.TopicUpdateRequestVO;
import com.gdufe.readingonline.dal.dataobject.GdufeTopicEbookDO;
import com.gdufe.readingonline.dal.mysqlmapper.GdufeTopicEbookMapper;
import com.gdufe.readingonline.service.topic.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 图书专题Service实现类
 * 
 * @author gdufe
 * @date 2025
 */
@Service
public class TopicServiceImpl implements TopicService {
    
    private static final Logger logger = LoggerFactory.getLogger(TopicServiceImpl.class);
    
    @Autowired
    private GdufeTopicEbookMapper topicEbookMapper;
    
    /**
     * 创建图书专题
     * 
     * @param requestVO 创建请求VO
     * @return 创建响应VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TopicCreateResponseVO createTopic(TopicCreateRequestVO requestVO) {
        try {
            logger.info("开始创建图书专题，创建人：{}", requestVO.getCreator());
            
            // 创建实体对象
            GdufeTopicEbookDO topicDO = new GdufeTopicEbookDO();
            topicDO.setBookTopicHtml(requestVO.getHtmlContent()); // 保持HTML文本完整
            topicDO.setBookTopicTitle(requestVO.getTopicTitle()); // 专题标题
            topicDO.setBookTopicBriefIntroduction(requestVO.getTopicBriefIntroduction()); // 专题简介
            topicDO.setBookTopicCatagory(requestVO.getTopicCategory()); // 专题分类
            topicDO.setBookTopicCreator(requestVO.getCreator());
            topicDO.setBookTopicStatus(0); // 默认为草稿状态
            topicDO.setIsDeleted(0); // 未删除
            topicDO.setCreateTime(LocalDateTime.now());
            topicDO.setUpdateTime(LocalDateTime.now());
            
            // 保存到数据库
            int insertResult = topicEbookMapper.insert(topicDO);
            
            if (insertResult > 0) {
                logger.info("图书专题创建成功，专题ID：{}", topicDO.getId());
                
                // 构建响应对象
                TopicCreateResponseVO responseVO = new TopicCreateResponseVO();
                responseVO.setTopicId(topicDO.getId());
                responseVO.setCreateTime(topicDO.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                return responseVO;
            } else {
                logger.error("图书专题创建失败，数据库插入返回0");
                throw new RuntimeException("图书专题创建失败");
            }
            
        } catch (Exception e) {
            logger.error("创建图书专题时发生异常：{}", e.getMessage(), e);
            throw new RuntimeException("创建图书专题失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据专题ID获取专题详情
     * 
     * @param topicId 专题ID
     * @return 专题详情响应VO
     */
    @Override
    public TopicDetailResponseVO getTopicById(Long topicId) {
        try {
            logger.info("开始查询专题详情，专题ID：{}", topicId);
            
            // 构建查询条件
            QueryWrapper<GdufeTopicEbookDO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", topicId)
                       .eq("is_deleted", 0); // 只查询未删除的专题
            
            // 查询数据库
            GdufeTopicEbookDO topicDO = topicEbookMapper.selectOne(queryWrapper);
            
            if (topicDO == null) {
                logger.warn("专题不存在，专题ID：{}", topicId);
                throw new RuntimeException("专题不存在");
            }
            
            logger.info("查询到专题详情，专题ID：{}", topicDO.getId());
            
            // 构建响应对象
            TopicDetailResponseVO responseVO = new TopicDetailResponseVO();
            responseVO.setHtmlContent(topicDO.getBookTopicHtml());
            
            // 格式化发布时间（使用创建时间作为发布时间）
            if (topicDO.getCreateTime() != null) {
                responseVO.setPublishTime(topicDO.getCreateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            
            // 格式化结束时间
            if (topicDO.getEndTime() != null) {
                responseVO.setEndTime(topicDO.getEndTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else {
                responseVO.setEndTime(null); // 如果没有结束时间，返回null
            }
            
            return responseVO;
            
        } catch (Exception e) {
            logger.error("查询专题详情时发生异常：{}", e.getMessage(), e);
            throw new RuntimeException("查询专题详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取最近发布的专题列表（按发布时间倒序，最多返回4条）
     * 
     * @return 专题列表
     */
    @Override
    public List<TopicListItemVO> getRecentTopics() {
        try {
            logger.info("开始查询最近发布的专题列表");
            
            // 构建查询条件：只查询未删除的专题，按创建时间倒序排序，限制4条
            QueryWrapper<GdufeTopicEbookDO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_deleted", 0) // 只查询未删除的专题
                       .orderByDesc("create_time") // 按创建时间倒序排序
                       .last("LIMIT 4"); // 限制返回4条记录
            
            // 查询数据库
            List<GdufeTopicEbookDO> topicDOList = topicEbookMapper.selectList(queryWrapper);
            
            logger.info("查询到{}条专题记录", topicDOList != null ? topicDOList.size() : 0);
            
            // 转换为VO列表
            List<TopicListItemVO> resultList = new ArrayList<>();
            if (topicDOList != null && !topicDOList.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                
                for (GdufeTopicEbookDO topicDO : topicDOList) {
                    TopicListItemVO itemVO = new TopicListItemVO();
                    itemVO.setTopicId(topicDO.getId());
                    itemVO.setTopicTitle(topicDO.getBookTopicTitle());
                    itemVO.setTopicBriefIntroduction(topicDO.getBookTopicBriefIntroduction());
                    itemVO.setTopicCategory(topicDO.getBookTopicCatagory());
                    itemVO.setHtmlContent(topicDO.getBookTopicHtml());
                    
                    // 格式化发布时间（使用创建时间作为发布时间）
                    if (topicDO.getCreateTime() != null) {
                        itemVO.setPublishTime(topicDO.getCreateTime().format(formatter));
                    }
                    
                    // 格式化结束时间
                    if (topicDO.getEndTime() != null) {
                        itemVO.setEndTime(topicDO.getEndTime().format(formatter));
                    }
                    
                    resultList.add(itemVO);
                }
            }
            
            logger.info("最近专题列表查询成功，返回{}条记录", resultList.size());
            return resultList;
            
        } catch (Exception e) {
            logger.error("查询最近专题列表时发生异常：{}", e.getMessage(), e);
            throw new RuntimeException("查询最近专题列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新图书专题
     * 
     * @param requestVO 更新请求VO
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTopic(TopicUpdateRequestVO requestVO) {
        try {
            logger.info("开始更新图书专题，专题ID：{}", requestVO.getTopicId());
            
            // 先检查专题是否存在
            QueryWrapper<GdufeTopicEbookDO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", requestVO.getTopicId())
                       .eq("is_deleted", 0); // 只查询未删除的专题
            
            GdufeTopicEbookDO existingTopic = topicEbookMapper.selectOne(queryWrapper);
            if (existingTopic == null) {
                logger.warn("专题不存在或已删除，专题ID：{}", requestVO.getTopicId());
                throw new RuntimeException("专题不存在或已删除");
            }
            
            // 构建更新条件
            UpdateWrapper<GdufeTopicEbookDO> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", requestVO.getTopicId())
                        .eq("is_deleted", 0); // 只更新未删除的专题
            
            // 构建更新实体
            GdufeTopicEbookDO updateDO = new GdufeTopicEbookDO();
            updateDO.setBookTopicHtml(requestVO.getHtmlContent());
            updateDO.setBookTopicTitle(requestVO.getTopicTitle());
            updateDO.setBookTopicBriefIntroduction(requestVO.getTopicBriefIntroduction());
            updateDO.setBookTopicCatagory(requestVO.getTopicCategory());
            updateDO.setBookTopicCreator(requestVO.getCreator());
            updateDO.setUpdateTime(LocalDateTime.now());
            
            // 执行更新
            int updateResult = topicEbookMapper.update(updateDO, updateWrapper);
            
            if (updateResult > 0) {
                logger.info("图书专题更新成功，专题ID：{}", requestVO.getTopicId());
                return true;
            } else {
                logger.error("图书专题更新失败，数据库更新返回0，专题ID：{}", requestVO.getTopicId());
                throw new RuntimeException("图书专题更新失败");
            }
            
        } catch (Exception e) {
            logger.error("更新图书专题时发生异常：{}", e.getMessage(), e);
            throw new RuntimeException("更新图书专题失败：" + e.getMessage());
        }
    }
    
}
