package shop.util;

/*
  페이지 계산 — 목록 화면이 공통으로 쓴다.

  화면은 1부터 세고 LIMIT 은 0부터 센다. 그 변환을 한 군데로 모아
  각 DAO 가 offset 을 직접 계산하지 않게 한다.
*/
public class Paging {

	public static final int DEFAULT_SIZE = 10;

	/* 페이지 번호 묶음의 크기 — [1][2][3]...[10] 다음 */
	public static final int BLOCK_SIZE = 10;

	private Paging() {
	}

	public static int normalizePage(int page) {
		return page < 1 ? 1 : page;
	}

	public static int normalizeSize(int size) {
		if (size < 1) {
			return DEFAULT_SIZE;
		}
		return size > 100 ? 100 : size;
	}

	public static int offset(int page, int size) {
		return (normalizePage(page) - 1) * normalizeSize(size);
	}

	public static int totalPages(int total, int size) {
		if (total <= 0) {
			return 1;
		}
		int s = normalizeSize(size);
		return (total + s - 1) / s;
	}
}
