package com.nis.modules.mission.entity;

public enum MissionExecuteType {
	UPGRADE_KILL_PROCESS("upgrade_kill_process"),    //停用进程
	UPGRADE_START("upgrade_start"),                  //启动
	UPGRADE_BACKUP("upgrade_backup"),                //备份
	UPGRADE_UPDATE("upgrade_update"),                //覆盖更新
	UPGRADE_RECOVER("upgrade_recover"),              //恢复
	UPGRADE_EXEC("upgrade_exec"),                    //可执行命令
	SINGLE_EXEC("single_exec");                      //单次执行命令
	
	private String name;
	
	private MissionExecuteType(String name) {
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

}