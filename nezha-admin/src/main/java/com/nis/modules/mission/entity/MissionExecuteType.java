package com.nis.modules.mission.entity;

public enum MissionExecuteType {
	UPGRADE_KILL_PROCESS(1,"upgrade_kill_process"),    //停用进程
	UPGRADE_START(1,"upgrade_start"),                  //启动
	UPGRADE_BACKUP(1,"upgrade_backup"),                //备份
	UPGRADE_UPDATE(1,"upgrade_update"),                //覆盖更新
	UPGRADE_RECOVER(1,"upgrade_recover"),              //恢复
	UPGRADE_EXEC(2,"upgrade_exec"),                    //可执行命令
	SINGLE_EXEC(2,"single_exec");                      //单次执行命令
	
	private Integer type;
	private String name;
	
	private MissionExecuteType(Integer type,String name) {
		this.type=type;
		this.name=name;
	}
	
	public static MissionExecuteType getMissionExecuteType(String name) {
		MissionExecuteType[] missionExecuteTypes = MissionExecuteType.values();
		for(MissionExecuteType missionExecuteType:missionExecuteTypes) {
			if(missionExecuteType.getName().equals(name)) {
				return missionExecuteType;
			}
		}
		return null;
	}

	public String getName() {
		return name;
	}

	public Integer getType() {
		return type;
	}
	
}