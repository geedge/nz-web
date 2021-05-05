package com.nis.modules.sys.entity;

import cn.hutool.core.util.StrUtil;
import org.snmp4j.PDU;
import org.snmp4j.smi.Variable;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TrapMessage implements Serializable{

	private String agentSendIP;
	private String pduAgentIP;
	private Timestamp receiverTime;
	private int trapVersion;
	private int trapV1GenericType;
	private int trapV1SpecificType;
	private String trapName;
	private String trapOID;
	private List trapPDUOIDs;
	private List trapPDUOIDValues;
	private PDU originalPDU;
	public static final int TrapVersionV1 = 1;
	public static final int TrapVersionV2 = 2;

	public TrapMessage()
	{
		agentSendIP = "";
		pduAgentIP = "";
		receiverTime = new Timestamp(System.currentTimeMillis());
		trapVersion = 1;
		trapV1GenericType = -1;
		trapV1SpecificType = -1;
		trapName = "";
		trapOID = "";
		trapPDUOIDs = new ArrayList();
		trapPDUOIDValues = new ArrayList();
	}

	public Variable getOIDValue(String s)
	{
		for (int i = 0; i < trapPDUOIDs.size(); i++)
		{
			String s1 = (String)trapPDUOIDs.get(i);
			if(StrUtil.equals(s,s1))
				return (Variable)trapPDUOIDValues.get(i);
		}

		return null;
	}

	public String getAgentSendIP()
	{
		return agentSendIP;
	}

	public void setAgentSendIP(String s)
	{
		agentSendIP = s;
	}

	public Timestamp getReceiverTime()
	{
		return receiverTime;
	}

	public void setReceiverTime(Timestamp timestamp)
	{
		receiverTime = timestamp;
	}

	public int getTrapVersion()
	{
		return trapVersion;
	}

	public void setTrapVersion(int i)
	{
		trapVersion = i;
	}

	public String getTrapName()
	{
		return trapName;
	}

	public void setTrapName(String s)
	{
		trapName = s;
	}

	public String getTrapOID()
	{
		return trapOID;
	}

	public void setTrapOID(String s)
	{
		trapOID = s;
	}

	public String getPduAgentIP()
	{
		return pduAgentIP;
	}

	public void setPduAgentIP(String s)
	{
		pduAgentIP = s;
	}

	public int getTrapV1GenericType()
	{
		return trapV1GenericType;
	}

	public void setTrapV1GenericType(int i)
	{
		trapV1GenericType = i;
	}

	public int getTrapV1SpecificType()
	{
		return trapV1SpecificType;
	}

	public void setTrapV1SpecificType(int i)
	{
		trapV1SpecificType = i;
	}

	public List getTrapPDUOIDs()
	{
		return trapPDUOIDs;
	}

	public void setTrapPDUOIDs(List list)
	{
		trapPDUOIDs = list;
	}

	public List getTrapPDUOIDValues()
	{
		return trapPDUOIDValues;
	}

	public void setTrapPDUOIDValues(List list)
	{
		trapPDUOIDValues = list;
	}

	public void setOriginalPDU(PDU pdu)
	{
		originalPDU = pdu;
	}

	public PDU getOriginalPDU()
	{
		return originalPDU;
	}
}
