package app.treasure.device.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import app.treasure.device.domain.Device;
import app.treasure.device.filter.DeviceSearch;
import app.treasure.member.domain.Member;
import app.treasure.member.repository.MemberRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class DeviceRepositoryTest
{
	@Inject
	DeviceRepository deviceRepository;

	@Inject
	MemberRepository memberRepository;

	@TestTransaction
	@Test
	void shouldListAllPersistedDevices()
	{
		long before = deviceRepository.count();

		Device laptop = new Device();
		laptop.setDeviceName("Laptop");
		deviceRepository.persist(laptop);

		Device phone = new Device();
		phone.setDeviceName("Phone");
		deviceRepository.persist(phone);

		List<Device> all = deviceRepository.listAll();

		assertEquals(before + 2, all.size());
	}

	@TestTransaction
	@Test
	void shouldListEachGroupOnceWhenSeveralDevicesShareIt()
	{
		// given
		String group = "Shared group " + UUID.randomUUID();
		persistDeviceInGroup("SharedGroupLaptop", group);
		persistDeviceInGroup("SharedGroupPhone", group);

		// when
		List<String> groups = deviceRepository.findDistinctGroups();

		// then
		assertEquals(1, groups.stream().filter(group::equals).count());
	}

	@TestTransaction
	@Test
	void shouldLeaveOutBlankGroupWhenListingGroups()
	{
		// given
		persistDeviceInGroup("BlankGroupTablet", "");

		// when
		List<String> groups = deviceRepository.findDistinctGroups();

		// then
		assertFalse(groups.contains(""));
	}

	private void persistDeviceInGroup(String name, String group)
	{
		Device device = new Device();
		device.setDeviceName(name);
		device.setGroup(group);
		deviceRepository.persist(device);
	}

	@TestTransaction
	@Test
	void shouldFindDeviceWhenNameContainsSearchTextInAnotherCase()
	{
		// given
		String marker = unique();
		Device laptop = persistDevice("Laptop " + marker.toUpperCase());

		// when
		List<Device> found = deviceRepository.search(DeviceSearch.all().withNames(List.of(marker.toLowerCase()))).list();

		// then
		assertEquals(List.of(laptop.id), ids(found));
	}

	@TestTransaction
	@Test
	void shouldLeaveOutDeviceWhenNameDoesNotContainSearchText()
	{
		// given
		persistDevice("Unrelated " + unique());

		// when
		List<Device> found = deviceRepository.search(DeviceSearch.all().withNames(List.of(unique()))).list();

		// then
		assertEquals(List.of(), found);
	}

	@TestTransaction
	@Test
	void shouldTreatPercentSignAsTextWhenSearchingByName()
	{
		// given
		String marker = unique();
		persistDevice("Plain " + marker);
		Device discounted = persistDevice("50% " + marker);

		// when
		List<Device> found = deviceRepository.search(DeviceSearch.all().withNames(List.of("50% " + marker))).list();

		// then
		assertEquals(List.of(discounted.id), ids(found));
	}

	@TestTransaction
	@Test
	void shouldFindDeviceWhenOneOfSeveralNamesMatches()
	{
		// given
		String first = unique();
		String second = unique();
		Device laptop = persistDevice("Laptop " + first);
		Device phone = persistDevice("Phone " + second);

		// when
		List<Device> found = deviceRepository.search(DeviceSearch.all().withNames(List.of(first, second))).list();

		// then
		assertEquals(List.of(laptop.id, phone.id), ids(found));
	}

	@TestTransaction
	@Test
	void shouldFindOnlyDevicesNobodyHoldsWhenStatusIsAvailable()
	{
		// given
		String marker = unique();
		Device free = persistDevice("Free " + marker);
		persistHeldDevice("Held " + marker, persistMember("Holder", marker));

		// when
		List<Device> found = deviceRepository
			.search(DeviceSearch.all().withNames(List.of(marker)).withStatuses(List.of("Available"))).list();

		// then
		assertEquals(List.of(free.id), ids(found));
	}

	@TestTransaction
	@Test
	void shouldFindOnlyHeldDevicesWhenStatusIsNotAvailable()
	{
		// given
		String marker = unique();
		persistDevice("Free " + marker);
		Device held = persistHeldDevice("Held " + marker, persistMember("Holder", marker));

		// when
		List<Device> found = deviceRepository
			.search(DeviceSearch.all().withNames(List.of(marker)).withStatuses(List.of("not available"))).list();

		// then
		assertEquals(List.of(held.id), ids(found));
	}

	@TestTransaction
	@Test
	void shouldFindNoDeviceWhenStatusIsNeitherAvailableNorNotAvailable()
	{
		// given
		String marker = unique();
		persistDevice("Free " + marker);

		// when
		List<Device> found = deviceRepository
			.search(DeviceSearch.all().withNames(List.of(marker)).withStatuses(List.of("broken"))).list();

		// then
		assertEquals(List.of(), found);
	}

	@TestTransaction
	@Test
	void shouldFindDeviceWhenHolderNameContainsSearchText()
	{
		// given
		String marker = unique();
		Device held = persistHeldDevice("Held " + marker, persistMember("Erika", marker));
		persistDevice("Free " + marker);

		// when
		List<Device> found = deviceRepository
			.search(DeviceSearch.all().withHolders(List.of("erika " + marker))).list();

		// then
		assertEquals(List.of(held.id), ids(found));
	}

	@TestTransaction
	@Test
	void shouldLeaveOutDeviceWhenConditionOnlyContainsSearchText()
	{
		// given
		String marker = unique();
		Device device = persistDevice("Damaged " + marker);
		device.setDeviceDamage("lightly damaged");

		// when
		List<Device> found = deviceRepository
			.search(DeviceSearch.all().withNames(List.of(marker)).withConditions(List.of("damaged"))).list();

		// then
		assertEquals(List.of(), found);
	}

	@TestTransaction
	@Test
	void shouldFindDeviceWhenModelYearContainsSearchText()
	{
		// given
		String marker = unique();
		Device device = persistDevice("Dated " + marker);
		device.setDeviceModelDate(2023);

		// when
		List<Device> found = deviceRepository
			.search(DeviceSearch.all().withNames(List.of(marker)).withModelYears(List.of("202"))).list();

		// then
		assertEquals(List.of(device.id), ids(found));
	}

	@TestTransaction
	@Test
	void shouldCountGroupOnceWhenSpellingDiffersOnlyInCase()
	{
		// given
		String group = "Counted group " + unique();
		persistDeviceInGroup("CountedLaptop", group.toLowerCase());
		persistDeviceInGroup("CountedPhone", group.toUpperCase());

		// when
		Long count = deviceRepository.countPerGroup().get(group);

		// then
		assertEquals(2L, count);
	}

	@TestTransaction
	@Test
	void shouldReturnOnlyAsManyLongestHeldDevicesAsAsked()
	{
		// given
		Member holder = persistMember("Limit", unique());
		persistHeldDevice("LimitLaptop", holder);
		persistHeldDevice("LimitPhone", holder);

		// when
		List<Device> found = deviceRepository.findAssignedLongestFirst(1);

		// then
		assertEquals(1, found.size());
	}

	private static String unique()
	{
		return UUID.randomUUID().toString();
	}

	private static List<Long> ids(List<Device> devices)
	{
		return devices.stream().map(device -> device.id).toList();
	}

	private Device persistDevice(String name)
	{
		Device device = new Device();
		device.setDeviceName(name);
		deviceRepository.persist(device);
		return device;
	}

	private Device persistHeldDevice(String name, Member holder)
	{
		Device device = persistDevice(name);
		device.setAssignedTo(holder);
		device.setPickupTime(LocalDateTime.now());
		return device;
	}

	private Member persistMember(String firstName, String lastName)
	{
		Member member = new Member();
		member.setFirstName(firstName);
		member.setLastName(lastName);
		member.setUserName(firstName + "." + lastName);
		memberRepository.persist(member);
		return member;
	}
}
