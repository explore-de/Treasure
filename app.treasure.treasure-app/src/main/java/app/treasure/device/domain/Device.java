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
	private Boolean visible;

	@ManyToOne
	private Member bookedBy;
	private String createdOn;
	private String deviceGroup;
	private String extraInfo;
	private String deviceModel;
	private String deviceDamage;
	private String deviceAge;

	private String regCompany;
	private String deviceNumber;
	private String deviceProzessor;
	private String deviceHDDStorage;
	private String deviceRAM;
	private String deviceModelDate;
	private String deviceLocation;

	public String getDeviceLocation()
	{
		return deviceLocation;
	}

	public void setDeviceLocation(String deviceLocation)
	{
		this.deviceLocation = deviceLocation;
	}

	public String getDeviceModelDate()
	{
		return deviceModelDate;
	}

	public void setDeviceModelDate(String deviceModelDate)
	{
		this.deviceModelDate = deviceModelDate;
	}

	public String getDeviceRAM()
	{
		return deviceRAM;
	}

	public void setDeviceRAM(String deviceRAM)
	{
		this.deviceRAM = deviceRAM;
	}

	public String getDeviceHDDStorage()
	{
		return deviceHDDStorage;
	}

	public void setDeviceHDDStorage(String deviceHDDStorage)
	{
		this.deviceHDDStorage = deviceHDDStorage;
	}

	public String getDeviceProzessor()
	{
		return deviceProzessor;
	}

	public void setDeviceProzessor(String deviceProzessor)
	{
		this.deviceProzessor = deviceProzessor;
	}

	public String getDeviceNumber()
	{
		return deviceNumber;
	}

	public void setDeviceNumber(String deviceNumber)
	{
		this.deviceNumber = deviceNumber;
	}

	public void setRegCompany(String regCompany)
	{
		this.regCompany = regCompany;
	}

	public String getRegCompany()
	{
		return regCompany;
	}

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

	public Boolean getVisible()
	{
		return visible;
	}

	public void setVisible(Boolean visible)
	{
		this.visible = visible;
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