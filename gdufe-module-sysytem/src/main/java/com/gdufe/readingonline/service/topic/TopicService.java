package com.gdufe.readingonline.service.topic;

import com.gdufe.readingonline.controller.admin.vo.TopicCreateRequestVO;
import com.gdufe.readingonline.controller.admin.vo.TopicCreateResponseVO;
import com.gdufe.readingonline.controller.admin.vo.TopicDetailResponseVO;
import com.gdufe.readingonline.controller.admin.vo.TopicListItemVO;

import java.util.List;

/**
 * 图书专题Service接口
 * 
 * @author gdufe
 * @date 2025
 */
public interface TopicService {
    
    /**
     * 创建图书专题
     * 
     * @param requestVO 创建请求VO
     * @return 创建响应VO
     */
    TopicCreateResponseVO createTopic(TopicCreateRequestVO requestVO);
    
    /**
     * 根据专题编号获取专题详情
     * 
     * @param topicNo 专题编号
     * @return 专题详情响应VO
     */
    TopicDetailResponseVO getTopicByNo(String topicNo);
    
    /**
     * 获取最近发布的专题列表（按发布时间倒序，最多返回4条）
     * 
     * @return 专题列表
     */
    List<TopicListItemVO> getRecentTopics();
}
