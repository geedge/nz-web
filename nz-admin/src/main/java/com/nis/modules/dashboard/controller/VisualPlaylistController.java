package com.nis.modules.dashboard.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.dashboard.entity.VisualPlaylist;
import com.nis.modules.dashboard.service.VisualPlaylistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/visual")
public class VisualPlaylistController {


    @Autowired
    private VisualPlaylistService playlistService;


    /**
     * detail
     */
    @GetMapping("/playlist/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.PLAYLIST)
    public R detail(@PathVariable Integer id) {
        VisualPlaylist playlist = playlistService.queryPlaylistInfo(id);
        return R.ok(playlist);
    }


    /**
     * query list
     */
    @GetMapping("/playlist")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.PLAYLIST)
    public R queryPlaylistPage(@RequestParam Map<String, Object> params) {
        PageUtils page = playlistService.queryPlaylistPage(params);
        return R.ok(page);
    }


    /**
     * save
     */
    @PostMapping("/playlist")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.PLAYLIST)
    public R savePlaylist(@RequestBody VisualPlaylist playlist) {
        ValidateUtils.is(playlist.getName()).notNull(RCode.PLAYLIST_NAME_ISNULL).maxLength(64)
                .and(playlist.getIntvl()).notNull(RCode.PLAYLIST_INTVL_ISNULL)
                .and(playlist.getDashboardIds()).notNull(RCode.PLAYLIST_DASHBOARD_IDS_ISNULL)
                .and(playlist.getMode()).notNull(RCode.PLAYLIST_MODE_ISNULL);

        return R.ok().putData("id", playlistService.savePlaylist(playlist));
    }

    /**
     * update
     */
    @PutMapping("/playlist")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.PLAYLIST)
    public R updatePlaylist(@RequestBody VisualPlaylist playlist) {
        ValidateUtils.is(playlist.getId()).notNull(RCode.PLAYLIST_ID_ISNULL)
                .and(playlist.getName()).notNull(RCode.PLAYLIST_NAME_ISNULL).maxLength(64)
                .and(playlist.getIntvl()).notNull(RCode.PLAYLIST_INTVL_ISNULL)
                .and(playlist.getDashboardIds()).notNull(RCode.PLAYLIST_DASHBOARD_IDS_ISNULL)
                .and(playlist.getMode()).notNull(RCode.PLAYLIST_MODE_ISNULL);;

        return R.ok().putData("id", playlistService.updatePlaylist(playlist));
    }


    /**
     * delete
     */
    @DeleteMapping("/playlist")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.PLAYLIST)
    public R delete(Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.PLAYLIST_ID_ISNULL);

        playlistService.delete(ids);
        return R.ok();
    }

}
