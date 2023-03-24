package com.nis.modules.dashboard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.dashboard.entity.VisualPlaylist;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface VisualPlaylistDao extends BaseMapper<VisualPlaylist> {

}
