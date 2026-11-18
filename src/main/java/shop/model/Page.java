package shop.model;

import java.util.List;

/* 목록 한 페이지 — 화면 아래 페이지 번호(1 2 3 … 10)를 그리는 데 필요한 값 */
public class Page<T> {
	private List<T> list;
	private int page, size, total;

	public Page(List<T> list, int page, int size, int total) {
		this.list = list;
		this.size = size;
		this.total = total;
		this.page = Math.min(Math.max(page, 1), getTotalPages());
	}

	public List<T> getList() { return list; }
	public int getPage() { return page; }
	public int getSize() { return size; }
	public int getTotal() { return total; }
	public boolean isEmpty() { return list.isEmpty(); }
	public boolean isHasPrev() { return page > 1; }
	public boolean isHasNext() { return page < getTotalPages(); }

	public int getTotalPages() {
		return total <= 0 || size <= 0 ? 1 : (total + size - 1) / size;
	}

	/* 번호 묶음 — 1~10, 11~20 … */
	public int getBlockStart() { return (page - 1) / 10 * 10 + 1; }
	public int getBlockEnd() { return Math.min(getBlockStart() + 9, getTotalPages()); }
}
