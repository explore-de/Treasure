package app.treasure.device.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import app.treasure.member.domain.Member;

@Entity
public class Device extends PanacheEntity
{
	// ===== Basic Info =====

	private String deviceName;
	private String brand;
	private String modelNumber;
	private String serialNumber;

	// ===== Further Details =====

	private String operatingSystem;
	private boolean isUsed;
	private LocalDate purchaseDate;
	private String storageLocation;
	private String condition;
	private String importantNotes;

	// ===== Status =====

	private String status;

	/** The member who has currently claimed this device. */
	@ManyToOne(fetch = FetchType.EAGER)
	private Member bookedBy;

	private LocalDateTime pickupTime;

	// ===== Registration (auto) =====

	private LocalDateTime registeredAt;

	@ManyToOne(fetch = FetchType.EAGER)
	private Member registeredBy;

	// ===== deviceName =====

	public String getDeviceName()
	{
		return deviceName;
	}

	public void setDeviceName(String deviceName)
	{
		this.deviceName = deviceName;
	}

	// ===== brand =====

	public String getBrand()
	{
		return brand;
	}

	public void setBrand(String brand)
	{
		this.brand = brand;
	}

	// ===== modelNumber =====

	public String getModelNumber()
	{
		return modelNumber;
	}

	public void setModelNumber(String modelNumber)
	{
		this.modelNumber = modelNumber;
	}

	// ===== serialNumber =====

	public String getSerialNumber()
	{
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber)
	{
		this.serialNumber = serialNumber;
	}

	// ===== operatingSystem =====

	public String getOperatingSystem()
	{
		return operatingSystem;
	}

	public void setOperatingSystem(String operatingSystem)
	{
		this.operatingSystem = operatingSystem;
	}

	// ===== isUsed =====

	public boolean isUsed()
	{
		return isUsed;
	}

	public void setUsed(boolean isUsed)
	{
		this.isUsed = isUsed;
	}

	// ===== purchaseDate =====

	public LocalDate getPurchaseDate()
	{
		return purchaseDate;
	}

	public void setPurchaseDate(LocalDate purchaseDate)
	{
		this.purchaseDate = purchaseDate;
	}

	public String getFormattedPurchaseDate()
	{
		return purchaseDate != null ? purchaseDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "";
	}

	// ===== storageLocation =====

	public String getStorageLocation()
	{
		return storageLocation;
	}

	public void setStorageLocation(String storageLocation)
	{
		this.storageLocation = storageLocation;
	}

	// ===== condition =====

	public String getCondition()
	{
		return condition;
	}

	public void setCondition(String condition)
	{
		this.condition = condition;
	}

	// ===== importantNotes =====

	public String getImportantNotes()
	{
		return importantNotes;
	}

	public void setImportantNotes(String importantNotes)
	{
		this.importantNotes = importantNotes;
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

	// ===== registeredAt =====

	public LocalDateTime getRegisteredAt()
	{
		return registeredAt;
	}

	public void setRegisteredAt(LocalDateTime registeredAt)
	{
		this.registeredAt = registeredAt;
	}

	public String getFormattedRegisteredAt()
	{
		return registeredAt != null ? registeredAt.format(FORMATTER) : "";
	}

	// ===== registeredBy =====

	public Member getRegisteredBy()
	{
		return registeredBy;
	}

	public void setRegisteredBy(Member registeredBy)
	{
		this.registeredBy = registeredBy;
	}
}