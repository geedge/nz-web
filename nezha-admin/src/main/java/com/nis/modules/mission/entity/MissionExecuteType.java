package com.nis.modules.mission.entity;

public enum MissionExecuteType {
	UPGRADE_KILL_PROCESS(1,"upgrade_kill_process"),
	UPGRADE_START(1,"upgrade_start"),
	UPGRADE_BACKUP(1,"upgrade_backup"),
	UPGRADE_UPDATE(1,"upgrade_update"),
	UPGRADE_RECOVER(1,"upgrade_recover"),
	UPGRADE_EXEC(2,"upgrade_exec"),
	SINGLE_EXEC(2,"single_exec");
	
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