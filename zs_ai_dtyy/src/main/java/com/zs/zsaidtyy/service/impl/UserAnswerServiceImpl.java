package com.zs.zsaidtyy.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zs.zsaidtyy.common.ErrorCode;
import com.zs.zsaidtyy.constant.CommonConstant;
import com.zs.zsaidtyy.exception.ThrowUtils;
import com.zs.zsaidtyy.mapper.UserAnswerMapper;
import com.zs.zsaidtyy.model.dto.userAnswer.UserAnswerQueryRequest;
import com.zs.zsaidtyy.model.entity.App;
import com.zs.zsaidtyy.model.entity.UserAnswer;
import com.zs.zsaidtyy.model.entity.User;
import com.zs.zsaidtyy.model.vo.UserAnswerVO;
import com.zs.zsaidtyy.model.vo.UserVO;
import com.zs.zsaidtyy.service.AppService;
import com.zs.zsaidtyy.service.UserAnswerService;
import com.zs.zsaidtyy.service.UserService;
import com.zs.zsaidtyy.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户答案服务实现
 *
 * @author <a href="https://github.com/lizs">懒大王</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Service
@Slf4j
public class UserAnswerServiceImpl extends ServiceImpl<UserAnswerMapper, UserAnswer> implements UserAnswerService {

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    /**
     * 校验数据
     *
     * @param userAnswer
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validUserAnswer(UserAnswer userAnswer, boolean add) {
        ThrowUtils.throwIf(userAnswer == null, ErrorCode.PARAMS_ERROR);
        //  从对象中取值
        Long appId = userAnswer.getAppId();
        Long id = userAnswer.getId();
        // 创建数据时，参数不能为空
        if (add) {
            // todo 补充校验规则
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR,"appid非法");
            ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "id 不存在");
        }
        // 修改数据时，有参数则校验
        // todo 补充校验规则
        if (appId != null) {
            App app = appService.getById(appId);
            ThrowUtils.throwIf(app == null , ErrorCode.PARAMS_ERROR, "app不存在");
        }

    }

    /**
     * 获取查询条件
     *
     * @param userAnswerQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<UserAnswer> getQueryWrapper(UserAnswerQueryRequest userAnswerQueryRequest) {
        QueryWrapper<UserAnswer> queryWrapper = new QueryWrapper<>();
        if (userAnswerQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = userAnswerQueryRequest.getId();
        Long appId = userAnswerQueryRequest.getAppId();
        Integer appType = userAnswerQueryRequest.getAppType();
        Integer scoringStrategy = userAnswerQueryRequest.getScoringStrategy();
        String choices = userAnswerQueryRequest.getChoices();
        Long resultId = userAnswerQueryRequest.getResultId();
        String resultName = userAnswerQueryRequest.getResultName();
        String resultDesc = userAnswerQueryRequest.getResultDesc();
        String resultPicture = userAnswerQueryRequest.getResultPicture();
        Integer resultScore = userAnswerQueryRequest.getResultScore();
        Long userId = userAnswerQueryRequest.getUserId();
        Long notId = userAnswerQueryRequest.getNotId();
        String searchText = userAnswerQueryRequest.getSearchText();
        String sortField = userAnswerQueryRequest.getSortField();
        String sortOrder = userAnswerQueryRequest.getSortOrder();

        // 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("resultName", searchText).or().like("resultDesc", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(choices), "choices", choices);
        queryWrapper.like(StringUtils.isNotBlank(resultName), "resultName", resultName);
        queryWrapper.like(StringUtils.isNotBlank(resultDesc), "resultDesc", resultDesc);
        queryWrapper.like(StringUtils.isNotBlank(resultPicture), "resultPicture", resultPicture);
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(resultId), "resultId", resultId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(appId), "appId", appId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(appType), "appType", appType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(resultScore), "resultScore", resultScore);
        queryWrapper.eq(ObjectUtils.isNotEmpty(scoringStrategy), "scoringStrategy", scoringStrategy);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取用户答案封装
     *
     * @param userAnswer
     * @param request
     * @return
     */
    @Override
    public UserAnswerVO getUserAnswerVO(UserAnswer userAnswer, HttpServletRequest request) {
        // 对象转封装类
        UserAnswerVO userAnswerVO = UserAnswerVO.objToVo(userAnswer);

        //  可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = userAnswer.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        userAnswerVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
        long userAnswerId = userAnswer.getId();
        User loginUser = userService.getLoginUserPermitNull(request);
//        if (loginUser != null) {
//            // 获取点赞
//            QueryWrapper<UserAnswerThumb> userAnswerThumbQueryWrapper = new QueryWrapper<>();
//            userAnswerThumbQueryWrapper.in("userAnswerId", userAnswerId);
//            userAnswerThumbQueryWrapper.eq("userId", loginUser.getId());
//            UserAnswerThumb userAnswerThumb = userAnswerThumbMapper.selectOne(userAnswerThumbQueryWrapper);
//            userAnswerVO.setHasThumb(userAnswerThumb != null);
//            // 获取收藏
//            QueryWrapper<UserAnswerFavour> userAnswerFavourQueryWrapper = new QueryWrapper<>();
//            userAnswerFavourQueryWrapper.in("userAnswerId", userAnswerId);
//            userAnswerFavourQueryWrapper.eq("userId", loginUser.getId());
//            UserAnswerFavour userAnswerFavour = userAnswerFavourMapper.selectOne(userAnswerFavourQueryWrapper);
//            userAnswerVO.setHasFavour(userAnswerFavour != null);
//        }
        // endregion

        return userAnswerVO;
    }

    /**
     * 分页获取用户答案封装
     *
     * @param userAnswerPage
     * @param request
     * @return
     */
    @Override
    public Page<UserAnswerVO> getUserAnswerVOPage(Page<UserAnswer> userAnswerPage, HttpServletRequest request) {
        List<UserAnswer> userAnswerList = userAnswerPage.getRecords();
        Page<UserAnswerVO> userAnswerVOPage = new Page<>(userAnswerPage.getCurrent(), userAnswerPage.getSize(), userAnswerPage.getTotal());
        if (CollUtil.isEmpty(userAnswerList)) {
            return userAnswerVOPage;
        }
        // 对象列表 => 封装对象列表
        List<UserAnswerVO> userAnswerVOList = userAnswerList.stream().map(userAnswer -> {
            return UserAnswerVO.objToVo(userAnswer);
        }).collect(Collectors.toList());

        // 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = userAnswerList.stream().map(UserAnswer::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
//        // 2. 已登录，获取用户点赞、收藏状态
//        Map<Long, Boolean> userAnswerIdHasThumbMap = new HashMap<>();
//        Map<Long, Boolean> userAnswerIdHasFavourMap = new HashMap<>();
//        User loginUser = userService.getLoginUserPermitNull(request);
//        if (loginUser != null) {
//            Set<Long> userAnswerIdSet = userAnswerList.stream().map(UserAnswer::getId).collect(Collectors.toSet());
//            loginUser = userService.getLoginUser(request);
//            // 获取点赞
//            QueryWrapper<UserAnswerThumb> userAnswerThumbQueryWrapper = new QueryWrapper<>();
//            userAnswerThumbQueryWrapper.in("userAnswerId", userAnswerIdSet);
//            userAnswerThumbQueryWrapper.eq("userId", loginUser.getId());
//            List<UserAnswerThumb> userAnswerUserAnswerThumbList = userAnswerThumbMapper.selectList(userAnswerThumbQueryWrapper);
//            userAnswerUserAnswerThumbList.forEach(userAnswerUserAnswerThumb -> userAnswerIdHasThumbMap.put(userAnswerUserAnswerThumb.getUserAnswerId(), true));
//            // 获取收藏
//            QueryWrapper<UserAnswerFavour> userAnswerFavourQueryWrapper = new QueryWrapper<>();
//            userAnswerFavourQueryWrapper.in("userAnswerId", userAnswerIdSet);
//            userAnswerFavourQueryWrapper.eq("userId", loginUser.getId());
//            List<UserAnswerFavour> userAnswerFavourList = userAnswerFavourMapper.selectList(userAnswerFavourQueryWrapper);
//            userAnswerFavourList.forEach(userAnswerFavour -> userAnswerIdHasFavourMap.put(userAnswerFavour.getUserAnswerId(), true));
//        }
        // 填充信息
        userAnswerVOList.forEach(userAnswerVO -> {
            Long userId = userAnswerVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            userAnswerVO.setUser(userService.getUserVO(user));
//            userAnswerVO.setHasThumb(userAnswerIdHasThumbMap.getOrDefault(userAnswerVO.getId(), false));
//            userAnswerVO.setHasFavour(userAnswerIdHasFavourMap.getOrDefault(userAnswerVO.getId(), false));
        });
        // endregion

        userAnswerVOPage.setRecords(userAnswerVOList);
        return userAnswerVOPage;
    }

}
