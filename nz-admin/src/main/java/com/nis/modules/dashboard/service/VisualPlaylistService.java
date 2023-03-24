package com.nis.modules.dashboard.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.dashboard.entity.VisualPlaylist;

import java.util.Map;

public interface VisualPlaylistService extends IService<VisualPlaylist> {

    VisualPlaylist queryPlaylistInfo(Integer id);

    PageUtils queryPlaylistPage(Map<String, Object> params);

    Integer savePlaylist(VisualPlaylist playlist);

    Integer updatePlaylist(VisualPlaylist playlist);

    void delete(Integer[] ids);

}
