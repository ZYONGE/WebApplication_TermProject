package shop.dto;

import java.util.ArrayList;
import java.util.List;

/*
  목록 한 페이지 — 조회 결과와 페이지 계산을 함께 들고 다닌다.

  totalPages · hasPrev · hasNext 는 저장하지 않고 계산한다. total 이나 size 만
  바꿔도 나머지가 따라오게 해서 두 값이 어긋나는 상태를 만들지 않는다.
*/
public class PageDTO<T> {

	private List<T> list;
	private int page;
	private int size;
	private int total;

	public PageDTO() {
		this.list = new ArrayList<T>();
		this.page = 1;
		this.size = 10;
	}

	public PageDTO(List<T> list, int page, int size, int total) {
		this.list = list == null ? new ArrayList<T>() : list;
		this.page = page;
		this.size = size;
		this.total = total;
	}

	public int getTotalPages() {
		if (total <= 0 || size <= 0) {
			return 1;
		}
		return (total + size - 1) / size;
	}

	public boolean isHasPrev() {
		return page > 1;
	}

	public boolean isHasNext() {
		return page < getTotalPages();
	}

	public boolean isEmpty() {
		return list == null || list.isEmpty();
	}

	/* 페이지 번호 묶음 — [11][12]...[20] 의 시작과 끝 */
	public int getBlockStart() {
		return ((page - 1) / 10) * 10 + 1;
	}

	public int getBlockEnd() {
		int end = getBlockStart() + 9;
		return end > getTotalPages() ? getTotalPages() : end;
	}

	public List<T> getList() {
		return list;
	}

	public void setList(List<T> list) {
		this.list = list;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}
}
