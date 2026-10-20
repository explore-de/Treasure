package app.treasure.device.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import app.treasure.member.domain.Member;

@Entity
public class Device extends PanacheEntity
{
	private String deviceName;
	private String status;
	private LocalDateTime pickupTime;
	private String deviceSerialNumber;

	@ManyToOne
	private Member assignedTo;

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

	public Member getAssignedTo()
	{
		return assignedTo;
	}

	public void setAssignedTo(Member assignedto)
	{
		this.assignedTo = assignedto;
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

	@OneToMany(mappedBy = "device", cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<DeviceHistory> histories;

	public String getBookedName()
	{
		if (assignedTo != null)
		{
			return assignedTo.getDisplayName();
		}
		return "";
	}

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	public String getFormattedPickupTime()
	{
		return pickupTime != null ? pickupTime.format(formatter) : "";
	}

	/**
	 * Whole days since the device was handed out; 0 when it is not assigned.
	 */
	public long getDaysHeld()
	{
		return pickupTime != null ? java.time.temporal.ChronoUnit.DAYS.between(pickupTime, LocalDateTime.now()) : 0;
	}

	/**
	 * createdOn is stored as the ISO text of a LocalDateTime. Render it like
	 * the pickup time; fall back to the raw value for rows that were written in
	 * another format.
	 */
	public String getFormattedCreatedOn()
	{
		if (createdOn == null || createdOn.isBlank())
		{
			return "";
		}
		try
		{
			return LocalDateTime.parse(createdOn).format(formatter);
		}
		catch (DateTimeParseException e)
		{
			return createdOn;
		}
	}
}