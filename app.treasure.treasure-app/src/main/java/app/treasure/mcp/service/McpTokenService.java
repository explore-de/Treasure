package app.treasure.mcp.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.treasure.mcp.domain.McpToken;
import app.treasure.mcp.repository.McpTokenRepository;
import app.treasure.member.domain.Member;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/** Issues, checks and revokes the personal API tokens of the MCP endpoint. */
@ApplicationScoped
public class McpTokenService
{
	private static final Logger LOG = LoggerFactory.getLogger(McpTokenService.class);

	/**
	 * Marks Treasure tokens, so a leaked one is easy to recognise in logs and
	 * secret scanners.
	 */
	public static final String TOKEN_PREFIX = "trs_";

	private static final int TOKEN_BYTES = 32;
	private static final int SHOWN_PREFIX_LENGTH = 8;
	/** lastUsedAt is written at most this often, not on every MCP request. */
	private static final Duration LAST_USED_RESOLUTION = Duration.ofMinutes(1);

	private final SecureRandom random = new SecureRandom();

	@Inject
	McpTokenRepository tokenRepository;

	/**
	 * A freshly issued token; the secret exists only here and is never stored.
	 */
	public record IssuedToken(McpToken token, String secret)
	{
	}

	@Transactional
	public IssuedToken issue(Member member, String name, Set<String> roles)
	{
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		String secret = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

		McpToken token = new McpToken();
		token.setMember(member);
		token.setName(name.trim());
		token.setTokenHash(hash(secret));
		token.setTokenPrefix(secret.substring(0, SHOWN_PREFIX_LENGTH));
		token.setRoles(String.join(",", new TreeSet<>(roles)));
		token.setCreatedAt(LocalDateTime.now());
		tokenRepository.persist(token);

		LOG.info("Issued MCP token {} '{}' for {}", token.id, token.getName(), member.getUserName());
		return new IssuedToken(token, secret);
	}

	/**
	 * The token the secret belongs to, or null when it is unknown or revoked.
	 * Runs during authentication, where no request context is active yet.
	 */
	@ActivateRequestContext
	@Transactional
	public McpToken authenticate(String secret)
	{
		if (secret == null || !secret.startsWith(TOKEN_PREFIX))
		{
			return null;
		}
		McpToken token = tokenRepository.findByHash(hash(secret));
		if (token == null)
		{
			return null;
		}
		LocalDateTime now = LocalDateTime.now();
		if (token.getLastUsedAt() == null || token.getLastUsedAt().isBefore(now.minus(LAST_USED_RESOLUTION)))
		{
			token.setLastUsedAt(now);
		}
		// touch the lazy parts here, the identity is built after the
		// transaction ends
		token.getMember().getUserName();
		return token;
	}

	/** Deletes the token when it belongs to the member; false otherwise. */
	@Transactional
	public boolean revoke(Long tokenId, Member owner)
	{
		McpToken token = tokenRepository.findById(tokenId);
		if (token == null || owner == null || !token.getMember().id.equals(owner.id))
		{
			LOG.warn("Refused to revoke MCP token {} for {}", tokenId, owner != null ? owner.getUserName() : null);
			return false;
		}
		tokenRepository.delete(token);
		LOG.info("Revoked MCP token {} '{}' of {}", tokenId, token.getName(), owner.getUserName());
		return true;
	}

	/**
	 * Hex SHA-256 of the secret. Tokens are long random values, so a plain hash
	 * is enough.
	 */
	static String hash(String secret)
	{
		try
		{
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}
}
