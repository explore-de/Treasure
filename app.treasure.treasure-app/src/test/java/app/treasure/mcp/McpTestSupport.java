package app.treasure.mcp;

import java.util.Set;
import java.util.UUID;

import app.treasure.device.domain.Device;
import app.treasure.device.repository.DeviceHistoryRepository;
import app.treasure.device.repository.DeviceRepository;
import app.treasure.mcp.repository.McpTokenRepository;
import app.treasure.mcp.service.McpTokenService;
import app.treasure.mcp.service.McpTokenService.IssuedToken;
import app.treasure.shared.BaseMemberTest;
import app.treasure.shared.security.Roles;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

/**
 * Members, tokens and devices for the MCP tests; each call uses fresh, unique
 * values.
 */
public abstract class McpTestSupport extends BaseMemberTest
{
	@Inject
	protected McpTokenService tokenService;

	@Inject
	protected McpTokenRepository tokenRepository;

	@Inject
	protected DeviceRepository deviceRepository;

	@Inject
	protected DeviceHistoryRepository deviceHistoryRepository;

	protected static String unique(String prefix)
	{
		return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	/** A member with a unique username and a token for them. */
	protected IssuedToken memberWithToken(String usernamePrefix)
	{
		String username = unique(usernamePrefix);
		return tokenService.issue(createTestMember(username), "test token", Set.of("user"));
	}

	/** Like {@link #memberWithToken}, but the token may also change data. */
	protected IssuedToken adminWithToken(String usernamePrefix)
	{
		String username = unique(usernamePrefix);
		return tokenService.issue(createTestMember(username), "test token", Set.of("user", Roles.ADMIN));
	}

	@Transactional(TxType.REQUIRES_NEW)
	protected Long createFreeDevice(String name, String serialNumber)
	{
		Device device = new Device();
		device.setDeviceName(name);
		device.setDeviceSerialNumber(serialNumber);
		device.setStatus("available");
		deviceRepository.persist(device);
		return device.id;
	}

	@Transactional(TxType.REQUIRES_NEW)
	protected Device findDevice(Long id)
	{
		Device device = deviceRepository.findById(id);
		if (device.getAssignedTo() != null)
		{
			device.getAssignedTo().getUserName();
		}
		return device;
	}

	@Transactional(TxType.REQUIRES_NEW)
	protected void deleteDevice(Long id)
	{
		deviceHistoryRepository.delete("device.id = ?1", id);
		deviceRepository.deleteById(id);
	}

	@Transactional(TxType.REQUIRES_NEW)
	protected void deleteToken(IssuedToken issued)
	{
		tokenRepository.deleteById(issued.token().id);
	}
}
