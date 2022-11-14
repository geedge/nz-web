package com.nis.modules.terminal.utils;

import cn.hutool.core.util.NumberUtil;
import com.jcraft.jsch.SftpProgressMonitor;
import lombok.Data;

@Data
public class SFTPUploadMonitor implements SftpProgressMonitor {

    /**
     * 总大小
     */
    private long totalSize = 0;

    /**
     * 已上传大小
     */
    private long uploadedSize = 0;

    private long startTime = 0L;

    public SFTPUploadMonitor(long totalSize) {
        this.totalSize = totalSize;
    }

    @Override
    public void init(int op, String src, String dest, long max) {
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean count(long count) {
        this.uploadedSize += count;
        if (count > 0) {
            return true;
        }
        return false;
    }

    @Override
    public void end() {
    }

    /**
     * 上传进度
     *
     * @return
     */
    public String getUploadProgress() {
        String percent = NumberUtil.formatPercent((double) this.uploadedSize / this.totalSize, 2);
        return percent.replace("%", "");
    }
}
