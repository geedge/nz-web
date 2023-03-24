package com.nis.modules.dashboard.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.dashboard.dao.VisualPlaylistDao;
import com.nis.modules.dashboard.entity.VisualDashboard;
import com.nis.modules.dashboard.entity.VisualPlaylist;
import com.nis.modules.dashboard.service.VisualDashboardService;
import com.nis.modules.dashboard.service.VisualPlaylistService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class VisualPlaylistServiceImpl extends ServiceImpl<VisualPlaylistDao, VisualPlaylist> implements VisualPlaylistService {

    @Autowired
    private VisualDashboardService dashboardService;

    @Override
    public VisualPlaylist queryPlaylistInfo(Integer id) {
        VisualPlaylist playlist = this.getById(id);
        ValidateUtils.is(playlist).notNull(RCode.PLAYLIST_NOT_EXIST);

        Object dashboardIds = playlist.getDashboardIds();
        String dashboardIdsJsonArrStr = Tool.ObjectUtil.isEmpty(dashboardIds) ? Tool.StrUtil.EMPTY : Tool.StrUtil.toString(dashboardIds);
        if (Tool.StrUtil.isNotEmpty(dashboardIdsJsonArrStr)) {
            JSONArray dashboardIdList = Tool.JSONUtil.parseArray(dashboardIdsJsonArrStr);
            if (Tool.CollUtil.isNotEmpty(dashboardIdList)) {
                List<VisualDashboard> dashboardList = dashboardService.list(new LambdaUpdateWrapper<VisualDashboard>().in(VisualDashboard::getId, dashboardIdList));

                List<Object> dashboards = Tool.ListUtil.list(true);
                for (VisualDashboard dashboard : dashboardList) {
                    Map<Object, Object> map = Tool.MapUtil.builder()
                            .put("id", dashboard.getId())
                            .put("name", dashboard.getName())
                            .map();
                    dashboards.add(map);
                }
                playlist.setDashboards(dashboards);
            }
        }
        return playlist;
    }

    @Override
    public PageUtils queryPlaylistPage(Map<String, Object> params) {
        String ids = (String) params.get("ids");
        String name = (String) params.get("name");

        IPage result = this.page(
                new Query(VisualPlaylist.class).getPage(params),
                new LambdaUpdateWrapper<VisualPlaylist>()
                        .in(Tool.StrUtil.isNotEmpty(ids), VisualPlaylist::getId, Tool.StrUtil.split(ids, ","))
                        .like(Tool.StrUtil.isNotEmpty(name), VisualPlaylist::getName, Tool.StrUtil.likeEscape(name)));

        return new PageUtils(result);
    }

    /**
     * validate Playlist
     *
     * @param playlist
     */
    private void validatePlaylist(VisualPlaylist playlist) {
        // dashboards
        String dashboardIdsJsonArrStr = Tool.StrUtil.toString(playlist.getDashboardIds());
        JSONArray dashboardIdList = Tool.JSONUtil.parseArray(dashboardIdsJsonArrStr);
        if (Tool.CollUtil.isEmpty(dashboardIdList)) {
            throw new NZException(RCode.PLAYLIST_DASHBOARD_IDS_ISNULL);
        } else {
            List<VisualDashboard> dashboardList = dashboardService.list(new LambdaUpdateWrapper<VisualDashboard>().in(VisualDashboard::getId, dashboardIdList));
            ValidateUtils.is(dashboardList).notNull(RCode.DASHBOARD_NOT_EXIST);
            if (ObjectUtil.notEqual(dashboardList.size(), dashboardIdList.size())) {
                throw new NZException(RCode.DASHBOARD_NOT_EXIST);
            }
        }

        // name duplicate
        String name = playlist.getName();
        VisualPlaylist tVisualPlaylist = this.getOne(new LambdaQueryWrapper<VisualPlaylist>()
                .select(VisualPlaylist::getId)
                .eq(VisualPlaylist::getName, name)
                .ne(ObjectUtil.isNotNull(playlist.getId()), VisualPlaylist::getId, playlist.getId())
                .last("limit 1"));
        if (ToolUtil.isNotEmpty(tVisualPlaylist)) {
            throw new NZException(RCode.PLAYLIST_NAME_DUPLICATE);
        }
    }

    @Override
    public Integer savePlaylist(VisualPlaylist playlist) {
        // validate Playlist
        this.validatePlaylist(playlist);

        playlist.setUid(ShiroUtils.getUserId().intValue());
        playlist.setUts(System.currentTimeMillis());

        Object dashboardIds = playlist.getDashboardIds();
        playlist.setDashboardIds(StrUtil.toString(dashboardIds));

        // save
        this.save(playlist);

        return playlist.getId();
    }

    @Override
    public Integer updatePlaylist(VisualPlaylist playlist) {
        // validate Playlist
        this.validatePlaylist(playlist);

        playlist.setUid(ShiroUtils.getUserId().intValue());
        playlist.setUts(System.currentTimeMillis());

        Object dashboardIds = playlist.getDashboardIds();
        playlist.setDashboardIds(StrUtil.toString(dashboardIds));

        // update
        this.updateById(playlist);

        return playlist.getId();
    }

    @Override
    public void delete(Integer[] ids) {
        this.removeByIds(Arrays.asList(ids));
    }

}
