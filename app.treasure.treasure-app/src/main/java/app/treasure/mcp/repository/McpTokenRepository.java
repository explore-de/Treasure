package app.treasure.mcp.repository;

import java.util.List;

import app.treasure.mcp.domain.McpToken;
import app.treasure.member.domain.Member;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class McpTokenRepository implements PanacheRepository<McpToken>
{
	public McpToken findByHash(String tokenHash)
	{
		return find("tokenHash", tokenHash).firstResult();
	}

	public List<McpToken> findByMember(Member member)
	{
		return find("member", Sort.by("createdAt").descending(), member).list();
	}
}
