package app.treasure.mcp.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import app.treasure.member.domain.Member;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

/**
 * A personal API token for the MCP endpoint. Only the SHA-256 hash is stored;
 * the token acts as its member with the roles the member had when creating it.
 */
@Entity
public class McpToken extends PanacheEntity
{
	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	@ManyToOne(optional = false)
	private Member member;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, unique = true, length = 64)
	private String tokenHash;

	/**
	 * The first characters of the token, so its owner can recognise it in the
	 * list.
	 */
	@Column(nullable = false, length = 16)
	private String tokenPrefix;

	private String roles;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	private LocalDateTime lastUsedAt;

	public Member getMember()
	{
		return member;
	}

	public void setMember(Member member)
	{
		this.member = member;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getTokenHash()
	{
		return tokenHash;
	}

	public void setTokenHash(String tokenHash)
	{
		this.tokenHash = tokenHash;
	}

	public String getTokenPrefix()
	{
		return tokenPrefix;
	}

	public void setTokenPrefix(String tokenPrefix)
	{
		this.tokenPrefix = tokenPrefix;
	}

	public String getRoles()
	{
		return roles;
	}

	public void setRoles(String roles)
	{
		this.roles = roles;
	}

	public LocalDateTime getCreatedAt()
	{
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt)
	{
		this.createdAt = createdAt;
	}

	public LocalDateTime getLastUsedAt()
	{
		return lastUsedAt;
	}

	public void setLastUsedAt(LocalDateTime lastUsedAt)
	{
		this.lastUsedAt = lastUsedAt;
	}

	public Set<String> getRoleSet()
	{
		if (roles == null || roles.isBlank())
		{
			return Set.of();
		}
		return Arrays.stream(roles.split(",")).map(String::trim).filter(r -> !r.isEmpty())
			.collect(Collectors.toSet());
	}

	public String getFormattedCreatedAt()
	{
		return createdAt != null ? createdAt.format(FORMAT) : "";
	}

	public String getFormattedLastUsedAt()
	{
		return lastUsedAt != null ? lastUsedAt.format(FORMAT) : "never";
	}
}
