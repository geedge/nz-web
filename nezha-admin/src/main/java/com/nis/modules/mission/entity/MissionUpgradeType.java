package com.nis.modules.mission.entity;

public enum MissionUpgradeType {
	UPGRADE_AGENT("upgrade_agent"),
	UPGRADE_OTHER("upgrade_other"),
	UPGRADE_SERVER("upgrade_server");
	private String name;
	
	private MissionUpgradeType(String name) {
		this.name=name;
	}
	
	public static MissionUpgradeType getMissionUpgradeType(String name) {
		MissionUpgradeType[] missionUpgradeTypes = MissionUpgradeType.values();
		for(MissionUpgradeType missionUpgradeType:missionUpgradeTypes) {
			if(missionUpgradeType.getName().equals(name)) {
				return missionUpgradeType;
			}
		}
		return null;
	}

	public String getName() {
		return name;
	}
}
