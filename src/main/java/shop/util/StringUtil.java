package shop.util;

/* 문자열 가공 — 화면에 내보내기 직전의 공통 처리만 모은다. */
public class StringUtil {

	private StringUtil() {
	}

	public static boolean isEmpty(String s) {
		return s == null || s.trim().length() == 0;
	}

	public static String nvl(String s, String defaultValue) {
		return isEmpty(s) ? defaultValue : s;
	}

	public static String trim(String s) {
		return s == null ? null : s.trim();
	}

	/*
	  작성자명 마스킹 — 홍길동 → 홍*동

	  조회 SQL 에서 가공하지 않고 DTO 세팅 시점에 처리한다.
	  관리자 화면은 같은 쿼리로 원본 이름을 써야 하기 때문이다. (ERD § 7)
	*/
	public static String maskName(String name) {
		if (isEmpty(name)) {
			return "";
		}
		if (name.length() == 1) {
			return "*";
		}
		if (name.length() == 2) {
			return name.charAt(0) + "*";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(name.charAt(0));
		for (int i = 1; i < name.length() - 1; i++) {
			sb.append('*');
		}
		sb.append(name.charAt(name.length() - 1));
		return sb.toString();
	}

	/*
	  LIKE '%검색어%' 값 — 검색어 안의 % · _ · \ 를 이스케이프한다.
	  그대로 넣으면 '%' 한 글자 검색이 전체를 돌려준다. MySQL 기본 이스케이프 문자는 \ 다.
	*/
	public static String likeContains(String keyword) {
		String k = keyword == null ? "" : keyword;
		k = k.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + k + "%";
	}
}
