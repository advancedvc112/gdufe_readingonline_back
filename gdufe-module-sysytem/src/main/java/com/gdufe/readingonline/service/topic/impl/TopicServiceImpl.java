package com.gdufe.readingonline.service.topic.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gdufe.readingonline.controller.admin.vo.TopicCreateRequestVO;
import com.gdufe.readingonline.controller.admin.vo.TopicCreateResponseVO;
import com.gdufe.readingonline.controller.admin.vo.TopicDetailResponseVO;
import com.gdufe.readingonline.controller.admin.vo.TopicListItemVO;
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
import java.util.UUID;

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
            
            // 生成专题编号：当前时间年月日 + 6位UUID
            String topicNo = generateTopicNo();
            logger.info("生成专题编号：{}", topicNo);
            
            // 创建实体对象
            GdufeTopicEbookDO topicDO = new GdufeTopicEbookDO();
            topicDO.setBookTopicNo(topicNo);
            topicDO.setBookTopicHtml(requestVO.getHtmlContent()); // 保持HTML文本完整
            topicDO.setBookTopicCreator(requestVO.getCreator());
            topicDO.setBookTopicStatus(0); // 默认为草稿状态
            topicDO.setIsDeleted(0); // 未删除
            topicDO.setCreateTime(LocalDateTime.now());
            topicDO.setUpdateTime(LocalDateTime.now());
            
            // 保存到数据库
            int insertResult = topicEbookMapper.insert(topicDO);
            
            if (insertResult > 0) {
                logger.info("图书专题创建成功，专题ID：{}，专题编号：{}", topicDO.getId(), topicNo);
                
                // 构建响应对象
                TopicCreateResponseVO responseVO = new TopicCreateResponseVO();
                responseVO.setTopicNo(topicNo);
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
     * 根据专题编号获取专题详情
     * 
     * @param topicNo 专题编号
     * @return 专题详情响应VO
     */
    @Override
    public TopicDetailResponseVO getTopicByNo(String topicNo) {
        try {
            logger.info("开始查询专题详情，专题编号：{}", topicNo);
            
            // 构建查询条件
            QueryWrapper<GdufeTopicEbookDO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("book_topic_no", topicNo)
                       .eq("is_deleted", 0); // 只查询未删除的专题
            
            // 查询数据库
            GdufeTopicEbookDO topicDO = topicEbookMapper.selectOne(queryWrapper);
            
            if (topicDO == null) {
                logger.warn("专题不存在，专题编号：{}", topicNo);
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
                    itemVO.setTopicNo(topicDO.getBookTopicNo());
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
     * 生成专题编号：当前时间年月日 + 6位UUID
     * 格式：yyyyMMdd + 6位UUID
     * 
     * @return 专题编号
     */
    private String generateTopicNo() {
        // 获取当前日期，格式：yyyyMMdd
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 生成6位UUID（取UUID的前6位）
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        
        return dateStr + uuid;
    }
}
