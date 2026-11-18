package shop.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
  운영 설정 (system_config) — 배송비 · 무료배송 기준 · 섭취 일수 상한 · 점검 모드.
  /dev 에서 바꾼 값이 다음 요청에 바로 보이도록 캐시하지 않는다.
*/
public class ConfigDAO {

	/* 섭취 일수 입력의 처음 값 */
	public static final int DEFAULT_DAYS = 30;

	public static String value(String key) throws SQLException {
		return DB.text("SELECT config_value FROM system_config WHERE config_key = ?", key);
	}

	/* 숫자 설정 — 없거나 숫자가 아니면 기본값 */
	public static int number(String key, int defaultValue) throws SQLException {
		try {
			return Integer.parseInt(value(key).trim());
		} catch (RuntimeException e) {
			return defaultValue;
		}
	}

	public static boolean isMaintenance() throws SQLException {
		return "MAINTENANCE".equals(value("SHOP_STATUS"));
	}

	public static int maxDays() throws SQLException {
		int days = number("MAX_INTAKE_DAYS", 90);
		return days > 0 ? days : 90;
	}

	public static int freeShippingMin() throws SQLException {
		return number("FREE_SHIPPING_MIN", 50000);
	}

	public static int baseDeliveryFee() throws SQLException {
		return number("DELIVERY_FEE", 3000);
	}

	/* 배송비 = 상품 합계가 무료배송 기준 이상이면 0, 아니면 기본 배송비 (주문 때 계산해 저장한다) */
	public static int deliveryFee(int totalPrice) throws SQLException {
		if (totalPrice <= 0) {
			return 0;
		}
		return totalPrice >= freeShippingMin() ? 0 : baseDeliveryFee();
	}

	/* 섭취 일수 빠른 선택 — 상한을 넘는 값은 뺀다 */
	public static List<Integer> quickDays() throws SQLException {
		List<Integer> list = new ArrayList<Integer>();
		for (int d : new int[] { 7, 14, 30, 60, 90 }) {
			if (d <= maxDays()) {
				list.add(d);
			}
		}
		return list;
	}

	public static List<Map<String, Object>> all() throws SQLException {
		return DB.maps("SELECT * FROM system_config ORDER BY config_key");
	}

	public static int update(String key, String value) throws SQLException {
		return DB.update("UPDATE system_config SET config_value = ? WHERE config_key = ?", value, key);
	}
}
