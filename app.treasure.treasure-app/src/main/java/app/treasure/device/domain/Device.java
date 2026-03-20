package app.treasure.device.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

	/** The member who has currently claimed this device. */
	@ManyToOne(fetch = FetchType.EAGER)
	private Member bookedBy;

	// ===== deviceName =====

	public String getDeviceName()
	{
		return deviceName;
	}

	public void setDeviceName(String deviceName)
	{
		this.deviceName = deviceName;
	}

	// ===== status =====

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	// ===== bookedBy =====

	public Member getBookedBy()
	{
		return bookedBy;
	}

	public void setBookedBy(Member bookedBy)
	{
		this.bookedBy = bookedBy;
	}

	// ===== pickupTime =====

	public LocalDateTime getPickupTime()
	{
		return pickupTime;
	}

	public void setPickupTime(LocalDateTime pickupTime)
	{
		this.pickupTime = pickupTime;
	}

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	public String getFormattedPickupTime()
	{
		return pickupTime != null ? pickupTime.format(FORMATTER) : "";
	}
}