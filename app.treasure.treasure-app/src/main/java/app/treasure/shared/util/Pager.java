package app.treasure.shared.util;

import java.net.URI;

import jakarta.ws.rs.core.UriInfo;

/**
 * Where a paged list stands: the page shown (1-based), how many pages and rows
 * there are, and the links to the neighbouring pages. A link is null when there
 * is no such page. The links keep every other query parameter, so filters
 * survive paging.
 */
public record Pager(int page, int pageCount, long total, String prevUrl, String nextUrl)
{
	/** Rows per page of the device and member lists. */
	public static final int PAGE_SIZE = 25;

	/** The name of the query parameter that carries the page number. */
	public static final String PARAM = "page";

	/**
	 * The pager for the requested page of a list with {@code total} rows. A
	 * page number that is missing, below 1 or past the end becomes the nearest
	 * existing page, so a link to a page that emptied meanwhile still shows
	 * rows.
	 */
	public static Pager of(Integer requestedPage, long total, UriInfo uriInfo)
	{
		int pageCount = (int)Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
		int page = Math.clamp(requestedPage == null ? 1 : requestedPage, 1, pageCount);
		return new Pager(page, pageCount, total,
			page > 1 ? url(uriInfo, page - 1) : null,
			page < pageCount ? url(uriInfo, page + 1) : null);
	}

	/** The index of the shown page for Panache, which counts from 0. */
	public int index()
	{
		return page - 1;
	}

	private static String url(UriInfo uriInfo, int page)
	{
		URI uri = uriInfo.getRequestUriBuilder().replaceQueryParam(PARAM, page).build();
		return uri.getRawPath() + "?" + uri.getRawQuery();
	}
}
