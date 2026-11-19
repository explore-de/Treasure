package app.treasure.dashboard.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

	@Inject
	DeviceRepository deviceRepository;

	@Inject
	DeviceHistoryRepository deviceHistoryRepository;

	/**
	 * One row of the "devices per group" list. percent is relative to the
	 * largest group so the bars can be drawn without maths in the template.
	 * grouped is false for the row of devices without a group, whose name is
	 * the "No group" text in the reader's language and not a link.
	 */
	public record GroupCount(String name, long count, int percent, boolean grouped)
	{
		public GroupCount(String name, long count, int percent)
		{
			this(name, count, percent, true);
		}

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
		long total = deviceRepository.count();
		long assigned = deviceRepository.countAssigned();

		Map<String, Long> counts = deviceRepository.countPerGroup();
		long largest = counts.values().stream().mapToLong(Long::longValue).max().orElse(1);
		List<GroupCount> groups = new ArrayList<>();
		String noGroup = i18n.getMessage("dashboard.noGroup");
		counts.forEach((name, count) -> groups.add(new GroupCount(name.isEmpty() ? noGroup : name, count,
			(int)Math.round(count * 100.0 / largest), !name.isEmpty())));
		groups.sort(Comparator.comparingLong((GroupCount g) -> g.count()).reversed().thenComparing(GroupCount::name));

		List<Device> longestHeld = deviceRepository.findAssignedLongestFirst(LONGEST_HELD_LIMIT);
		List<DeviceHistory> recent = deviceHistoryRepository.findRecent(RECENT_LIMIT);

		return Templates.index(total, total - assigned, assigned, groups, longestHeld, recent);
	}
}
