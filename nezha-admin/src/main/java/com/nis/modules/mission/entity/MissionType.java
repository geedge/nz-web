package com.nis.modules.mission.entity;

public enum MissionType{
	FILE_PUSH(1,"FilePush"),
	EXECUTE_INSTRUCT(4,"ExecuteInstruct"),
	UGPRADE(6,"Ugprade");
	
	private int missionTypeId;
	private String missionTypeName;
	
	private MissionType(int missionTypeId,String missionTypeName) {
		this.missionTypeId=missionTypeId;
		this.missionTypeName=missionTypeName;
	}
	
	public static MissionType getMissionNameByTypeId(Integer typeId) {
		MissionType[] values = MissionType.values();
        int length = values.length;
        for (int i = 0; i < length; i++) {
            if (typeId==values[i].missionTypeId) {
                return values[i];
            }
        }
        return null;
	}

	public int getMissionTypeId() {
		return missionTypeId;
	}

	public void setMissionTypeId(int missionTypeId) {
		this.missionTypeId = missionTypeId;
	}

	public String getMissionTypeName() {
		return missionTypeName;
	}

	public void setMissionTypeName(String missionTypeName) {
		this.missionTypeName = missionTypeName;
	}
}
