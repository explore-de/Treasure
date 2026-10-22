package app.treasure.dashboard.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import app.treasure.device.domain.Device;
import app.treasure.device.domain.DeviceHistory;
import app.treasure.device.repository.DeviceHistoryRepository;
import app.treasure.device.repository.DeviceRepository;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Landing page: how many devices exist, who holds what, and what changed
 * recently.
 */
@Authenticated
@Path("/")
public class DashboardResource extends Controller
{
	private static final int LONGEST_HELD_LIMIT = 5;
	private static final int RECENT_LIMIT = 8;
	private static final String NO_GROUP = "No group";

	@Inject
	DeviceRepository deviceRepository;

	@Inject
	DeviceHistoryRepository deviceHistoryRepository;

	/**
	 * One row of the "devices per group" list. percent is relative to the
	 * largest group so the bars can be drawn without maths in the template.
	 */
	public record GroupCount(String name, long count, int percent)
	{
		/**
		 * The name as a query-string value, so "R&D" or "Home office" survive
		 * the link to the filtered device list.
		 */
		public String encodedName()
		{
			return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
		}
	}

	@CheckedTemplate
	public static class Templates
	{
		private Templates()
		{
			// static
		}

		public static native TemplateInstance index(long total, long available, long assigned,
			List<GroupCount> groups, List<Device> longestHeld, List<DeviceHistory> recent);
	}

	@GET
	@Path("")
	public TemplateInstance index()
	{
		List<Device> all = deviceRepository.listAll();
		long assigned = all.stream().filter(d -> d.getAssignedTo() != null).count();

		Map<String, Long> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (Device device : all)
		{
			String group = device.getGroup() == null || device.getGroup().isBlank() ? NO_GROUP
				: device.getGroup().trim();
			counts.merge(group, 1L, Long::sum);
		}
		long largest = counts.values().stream().mapToLong(Long::longValue).max().orElse(1);
		List<GroupCount> groups = new ArrayList<>();
		counts.forEach(
			(name, count) -> groups.add(new GroupCount(name, count, (int)Math.round(count * 100.0 / largest))));
		groups.sort(Comparator.comparingLong((GroupCount g) -> g.count()).reversed().thenComparing(GroupCount::name));

		List<Device> longestHeld = deviceRepository.findAssignedLongestFirst().stream()
			.limit(LONGEST_HELD_LIMIT)
			.toList();
		List<DeviceHistory> recent = deviceHistoryRepository.findRecent(RECENT_LIMIT);

		return Templates.index(all.size(), all.size() - assigned, assigned, groups, longestHeld, recent);
	}
}
