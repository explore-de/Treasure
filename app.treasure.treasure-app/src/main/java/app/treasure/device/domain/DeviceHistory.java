package app.treasure.device.domain;

import app.treasure.member.domain.Member;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class DeviceHistory extends PanacheEntity
{
	@ManyToOne(fetch = FetchType.EAGER)
	private Device device;

	@ManyToOne(fetch = FetchType.EAGER)
	private Member performedBy;

	private LocalDateTime timestamp;

	/** CREATED, CLAIMED, UNCLAIMED, EDITED, ASSIGNED, UNASSIGNED */
	private String action;

	private String details;

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	// ===== device =====

	public Device getDevice()
	{
		return device;
	}

	public void setDevice(Device device)
	{
		this.device = device;
	}

	// ===== performedBy =====

	public Member getPerformedBy()
	{
		return performedBy;
	}

	public void setPerformedBy(Member performedBy)
	{
		this.performedBy = performedBy;
	}

	// ===== timestamp =====

	public LocalDateTime getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp)
	{
		this.timestamp = timestamp;
	}

	public String getFormattedTimestamp()
	{
		return timestamp != null ? timestamp.format(FORMATTER) : "";
	}

	// ===== action =====

	public String getAction()
	{
		return action;
	}

	public void setAction(String action)
	{
		this.action = action;
	}

	// ===== details =====

	public String getDetails()
	{
		return details;
	}

	public void setDetails(String details)
	{
		this.details = details;
	}
}