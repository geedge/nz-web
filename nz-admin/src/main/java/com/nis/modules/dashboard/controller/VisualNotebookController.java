package com.nis.modules.dashboard.controller;

import cn.hutool.core.util.StrUtil;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.dashboard.entity.VisualNotebook;
import com.nis.modules.dashboard.service.VisualNotebookService;
import com.nis.modules.dashboard.service.VisualService;
import com.nis.modules.dashboard.utils.DashboardConstant;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * visual notebook controller
 *
 * @author shizhendong
 * @date 2023-08-16
 */
@RestController
@RequestMapping("/visual")
public class VisualNotebookController {


    @Autowired
    private VisualService visualService;


    @Autowired
    private VisualNotebookService notebookService;

    /**
     * query notebook info
     *
     * @param id
     * @return
     */
    @GetMapping("/notebook/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.NOTEBOOK)
    public R queryNotebookInfo(@PathVariable("id") Integer id) {
        return R.ok(notebookService.queryNotebookInfo(id));
    }


    /**
     * query notebook page
     *
     * @param params
     * @return
     */
    @GetMapping("/notebook")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.NOTEBOOK)
    public R queryNotebookPage(@RequestParam Map<String, Object> params) {
        PageUtils page = notebookService.queryNotebookPage(params);
        return R.ok(page);
    }


    /**
     * save or update notebook
     *
     * @param notebook
     * @return
     */
    @RequestMapping(value = "/notebook", method = {RequestMethod.POST, RequestMethod.PUT})
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.NOTEBOOK)
    public R saveOrUpdateNotebook(@RequestBody VisualNotebook notebook) {
        ValidateUtils.is(notebook.getName()).notNull(RCode.NOTEBOOK_NAME_ISNULL);

        return R.ok().putData("id", notebookService.saveOrUpdateNotebook(notebook));
    }


    /**
     * duplicate notebook
     *
     * @param notebook
     * @return
     */
    @PostMapping("/notebook/duplicate")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.NOTEBOOK)
    public R duplicateNotebook(@RequestBody VisualNotebook notebook) {
        ValidateUtils.is(notebook.getId()).notNull(RCode.NOTEBOOK_ID_ISNULL);

        notebookService.duplicateNotebook(notebook.getId(), notebook.getName());
        return R.ok();
    }


    /**
     * delete notebook
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/notebook")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.NOTEBOOK)
    public R deleteNotebook(Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.NOTEBOOK_ID_ISNULL);

        notebookService.deleteNotebook(Tool.ListUtil.list(true, ids));
        return R.ok();
    }


    /**
     * import notebook
     *
     * @param jsonFile
     * @param existed
     * @param ignoreError
     * @return
     */
    @PostMapping("/notebook/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.NOTEBOOK)
    public R importNotebook(@RequestParam(required = true, value = "file") MultipartFile jsonFile,
                            @RequestParam(required = false, defaultValue = "1") Integer existed,
                            @RequestParam(required = false, defaultValue = "1") Integer ignoreError) {
        String filename = jsonFile.getOriginalFilename();
        String fileType = FilenameUtils.getExtension(filename);
        if (!StrUtil.equalsIgnoreCase("json", fileType)) {
            throw new NZException(RCode.NOTEBOOK_IMPORT_FILETYPE_ERROR);
        }

        return R.ok(notebookService.importNotebook(jsonFile, existed, ignoreError));
    }


    /**
     * export notebook
     *
     * @param response
     * @param params
     */
    @GetMapping("/notebook/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.NOTEBOOK)
    public void exportNotebook(HttpServletResponse response, @RequestParam Map<String, Object> params) {
        notebookService.exportNotebook(response, params);
    }


    /**
     * create notebook snapshot task
     *
     * @param requestody
     * @return
     */
    @PostMapping("/notebook/snapshot/task")
    @SysLog(operation = OperationEnum.CREAT_TASK, type = TypeEnum.NOTEBOOK)
    public R createNotebookSnapshotTask(@RequestBody Map<String, Object> requestody) {
        String format = (String) requestody.get("format");
        format = Tool.StrUtil.emptyToDefault(format, "html");

        Object id = requestody.get("id");
        Object start = requestody.get("start");
        Object end = requestody.get("end");

        ValidateUtils.is(id).notNull(RCode.NOTEBOOK_ID_ISNULL)
                .and(start).notNull(RCode.AGENT_PARAM_START_ISNULL)
                .and(end).notNull(RCode.AGENT_PARAM_END_ISNULL);

        String taskId = visualService.createNotebookSnapshotTask(format, Integer.valueOf(id.toString()), Long.valueOf(start.toString()), Long.valueOf(end.toString()));

        return R.ok().putData("tid", taskId);
    }

    /**
     * query notebook snapshot result process
     *
     * @param tid
     * @return
     */
    @GetMapping("/notebook/snapshot/result/{tid}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.NOTEBOOK)
    public R queryNotebookSnapshotResultProcess(@PathVariable(required = true) String tid) {
        return R.ok().putData("taskId", visualService.querySnapshotResultProcess(DashboardConstant.NOTEBOOK_SNAPSHOT_TYPE, tid));
    }


    /**
     * cancel notebook snapshot task
     *
     * @param tid
     * @return
     */
    @GetMapping("/notebook/snapshot/cancel/{tid}")
    @SysLog(operation = OperationEnum.CANCEL, type = TypeEnum.NOTEBOOK)
    public R cancelNotebookSnapshotTask(@PathVariable(required = true) String tid) {
        visualService.cancelSnapshotTask(DashboardConstant.NOTEBOOK_SNAPSHOT_TYPE, tid);
        return R.ok();
    }


    /**
     * download notebook snapshot file
     *
     * @param response
     * @param language
     * @param tid
     */
    @GetMapping("/notebook/snapshot/download/{tid}")
    @SysLog(operation = OperationEnum.DOWNLOAD, type = TypeEnum.NOTEBOOK)
    public void downloadNotebookSnapshotFile(HttpServletResponse response,
                                             @RequestHeader(required = false, defaultValue = "en") String language,
                                             @PathVariable(required = true) String tid) {
        visualService.downloadSnapshotFile(response, DashboardConstant.NOTEBOOK_SNAPSHOT_TYPE, language, tid);
    }

}
