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
	private String deviceName; // fields

	private String status;

	private LocalDateTime pickupTime;

	@ManyToOne
	private Member bookedBy; // accessor methods
	private String createdOn;

	public String getDeviceName()
	{
		return deviceName;
	}

	public void setCreatedOn(String createdOn)
	{
		this.createdOn = createdOn;
	}

	public String getCreatedOn()
	{
		return createdOn;
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

	public Member getBookedBy()
	{
		return bookedBy;
	}

	public void setBookedBy(Member bookedby)
	{
		this.bookedBy = bookedby;
	}

	public LocalDateTime getPickupTime()
	{
		return pickupTime;
	}

	public void setPickupTime(LocalDateTime pickupTime)
	{
		this.pickupTime = pickupTime;
	}

	public String getBookedName() // null safe getter
	{
		if (bookedBy != null)
		{
			return bookedBy.getDisplayName();
		}
		return "";
	}

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"); // formates
																										// a
																										// datetime
																										// value
																										// to
																										// string

	public String getFormattedPickupTime()
	{
		return pickupTime != null ? pickupTime.format(formatter) : ""; // Ternary
																		// operator
	}

}
