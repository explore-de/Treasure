package app.treasure.device.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import app.treasure.member.domain.Member;

@Entity
public class Device extends PanacheEntity
{
	private String deviceName;
	private String status;
	private LocalDateTime pickupTime;
	private String deviceSerialNumber;

	@ManyToOne
	private Member bookedBy;

	private String createdOn;

	// ✅ rename backing field to avoid JPQL keyword issues
	private String deviceGroup;

	private String extraInfo;
	private String deviceModel;
	private String deviceDamage;
	private String deviceAge;

	public String getDeviceName()
	{
		return deviceName;
	}

	public void setDeviceName(String deviceName)
	{
		this.deviceName = deviceName;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	public LocalDateTime getPickupTime()
	{
		return pickupTime;
	}

	public void setPickupTime(LocalDateTime pickupTime)
	{
		this.pickupTime = pickupTime;
	}

	public void setDeviceSerialNumber(String deviceSerialNumber)
	{
		this.deviceSerialNumber = deviceSerialNumber;
	}

	public String getDeviceSerialNumber()
	{
		return deviceSerialNumber;
	}

	public Member getBookedBy()
	{
		return bookedBy;
	}

	public void setBookedBy(Member bookedby)
	{
		this.bookedBy = bookedby;
	}

	public void setCreatedOn(String createdOn)
	{
		this.createdOn = createdOn;
	}

	public String getCreatedOn()
	{
		return createdOn;
	}

	// ✅ keep public API as "group" (matches your forms/templates)
	public void setGroup(String group)
	{
		this.deviceGroup = group;
	}

	public String getGroup()
	{
		return deviceGroup;
	}

	public void setExtraInfo(String extraInfo)
	{
		this.extraInfo = extraInfo;
	}

	// ✅ FIX: correct JavaBean getter name
	public String getExtraInfo()
	{
		return extraInfo;
	}

	public void setDeviceModel(String deviceModel)
	{
		this.deviceModel = deviceModel;
	}

	public String getDeviceModel()
	{
		return deviceModel;
	}

	public void setDeviceDamage(String deviceDamage)
	{
		this.deviceDamage = deviceDamage;
	}

	public String getDeviceDamage()
	{
		return deviceDamage;
	}

	public void setDeviceAge(String deviceAge)
	{
		this.deviceAge = deviceAge;
	}

	public String getDeviceAge()
	{
		return deviceAge;
	}

	public String getBookedName()
	{
		if (bookedBy != null)
		{
			return bookedBy.getDisplayName();
		}
		return "";
	}

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	public String getFormattedPickupTime()
	{
		return pickupTime != null ? pickupTime.format(formatter) : "";
	}
}