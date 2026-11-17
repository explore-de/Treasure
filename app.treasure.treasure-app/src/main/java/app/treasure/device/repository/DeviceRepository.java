package app.treasure.device.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

import app.treasure.device.domain.Device;
import app.treasure.device.filter.DeviceSearch;
import app.treasure.member.domain.Member;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeviceRepository implements PanacheRepository<Device>
{
	/** Marks the next character of a LIKE pattern as literal text. */
	private static final char LIKE_ESCAPE = '!';

	/**
	 * The devices matching the filters, by id. The query runs in the database,
	 * so the caller can count and page it without loading every device.
	 */
	public PanacheQuery<Device> search(DeviceSearch search)
	{
		StringBuilder where = new StringBuilder();
		Map<String, Object> params = new HashMap<>();

		contains(where, params, "d.deviceName", search.names());
		status(where, search.statuses());
		contains(where, params, "concat(m.firstName, ' ', m.lastName)", search.holders());
		contains(where, params, "d.deviceSerialNumber", search.serialNumbers());
		contains(where, params, "d.deviceGroup", search.groups());
		contains(where, params, "d.deviceModel", search.models());
		equalTo(where, params, "d.deviceDamage", search.conditions());
		contains(where, params, "d.regCompany", search.companies());
		contains(where, params, "d.deviceNumber", search.inventoryNumbers());
		contains(where, params, "d.deviceProzessor", search.processors());
		contains(where, params, "d.deviceHDDStorage", search.storages());
		contains(where, params, "d.deviceRAM", search.rams());
		contains(where, params, "cast(d.deviceModelDate as String)", search.modelYears());

		String query = "select d from Device d left join d.assignedTo m"
			+ (where.isEmpty() ? "" : " where " + where)
			+ " order by d.id";
		return find(query, params);
	}

	/**
	 * Devices currently assigned to the member, the ones held longest first.
	 */
	public List<Device> findAssignedTo(Member member)
	{
		return list("assignedTo = ?1 ORDER BY pickupTime, deviceName", member);
	}

	/**
	 * Devices currently assigned to any of the members, the ones held longest
	 * first: one query for a whole page of members.
	 */
	public List<Device> findAssignedToAny(Collection<Member> members)
	{
		if (members.isEmpty())
		{
			return List.of();
		}
		return list("assignedTo in ?1 ORDER BY pickupTime, deviceName", members);
	}

	public long countAssignedTo(Member member)
	{
		return count("assignedTo", member);
	}

	/** How many devices a member holds right now. */
	public long countAssigned()
	{
		return count("assignedTo is not null");
	}

	/** Devices nobody holds right now, by name. */
	public List<Device> findAvailable()
	{
		return list("assignedTo is null ORDER BY deviceName");
	}

	/** The assigned devices held longest, at most {@code limit} of them. */
	public List<Device> findAssignedLongestFirst(int limit)
	{
		return find("assignedTo is not null ORDER BY pickupTime, deviceName").page(0, limit).list();
	}

	/**
	 * Every group name still present on a device, each once. Reads one column
	 * instead of loading every device.
	 */
	public List<String> findDistinctGroups()
	{
		return getEntityManager()
			.createQuery("select distinct d.deviceGroup from Device d "
				+ "where d.deviceGroup is not null and d.deviceGroup <> ''", String.class)
			.getResultList();
	}

	/**
	 * How many devices each group has, by group name ignoring case. Devices
	 * without a group are counted under the empty name.
	 */
	public Map<String, Long> countPerGroup()
	{
		List<Object[]> rows = getEntityManager()
			.createQuery("select d.deviceGroup, count(d) from Device d group by d.deviceGroup", Object[].class)
			.getResultList();
		Map<String, Long> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (Object[] row : rows)
		{
			String group = row[0] == null ? "" : ((String)row[0]).trim();
			counts.merge(group, (Long)row[1], Long::sum);
		}
		return counts;
	}

	/** Adds "the field contains one of the values", ignoring case. */
	private static void contains(StringBuilder where, Map<String, Object> params, String field, List<String> values)
	{
		anyOf(where, params, values,
			name -> "lower(" + field + ") like :" + name + " escape '" + LIKE_ESCAPE + "'",
			value -> "%" + escapeLike(value.toLowerCase(Locale.ROOT)) + "%");
	}

	/** Adds "the field equals one of the values", ignoring case. */
	private static void equalTo(StringBuilder where, Map<String, Object> params, String field, List<String> values)
	{
		anyOf(where, params, values, name -> "lower(" + field + ") = :" + name,
			value -> value.toLowerCase(Locale.ROOT));
	}

	private static void anyOf(StringBuilder where, Map<String, Object> params, List<String> values,
		UnaryOperator<String> condition, UnaryOperator<String> parameter)
	{
		if (values.isEmpty())
		{
			return;
		}
		List<String> conditions = new ArrayList<>();
		for (String value : values)
		{
			String name = "p" + params.size();
			params.put(name, parameter.apply(value));
			conditions.add(condition.apply(name));
		}
		and(where).append('(').append(String.join(" or ", conditions)).append(')');
	}

	/**
	 * Adds the status filter. The status is not stored: a device is available
	 * exactly when nobody holds it. A value that is neither status matches no
	 * device.
	 */
	private static void status(StringBuilder where, List<String> statuses)
	{
		if (statuses.isEmpty())
		{
			return;
		}
		boolean available = statuses.stream().anyMatch(Device.STATUS_AVAILABLE::equalsIgnoreCase);
		boolean notAvailable = statuses.stream().anyMatch(Device.STATUS_NOT_AVAILABLE::equalsIgnoreCase);
		if (available && notAvailable)
		{
			return;
		}
		String condition = "1 = 0";
		if (available)
		{
			condition = "d.assignedTo is null";
		}
		else if (notAvailable)
		{
			condition = "d.assignedTo is not null";
		}
		and(where).append(condition);
	}

	private static StringBuilder and(StringBuilder where)
	{
		return where.isEmpty() ? where : where.append(" and ");
	}

	/** Makes %, _ and the escape character itself literal in a LIKE pattern. */
	private static String escapeLike(String value)
	{
		StringBuilder escaped = new StringBuilder();
		for (char c : value.toCharArray())
		{
			if (c == '%' || c == '_' || c == LIKE_ESCAPE)
			{
				escaped.append(LIKE_ESCAPE);
			}
			escaped.append(c);
		}
		return escaped.toString();
	}
}
