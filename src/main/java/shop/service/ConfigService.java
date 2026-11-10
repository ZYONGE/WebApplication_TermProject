package shop.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import shop.dao.ConfigDAO;
import shop.dto.SystemConfigDTO;

/*
  system_config 읽기 — 배송비 계산과 점검 모드 판정이 여기를 거친다.

  값을 캐시하지 않는다. /dev 에서 바꾼 설정이 다음 요청에 바로 보여야
  시연이 성립하기 때문이다 (schema.sql system_config 주석).
*/
public class ConfigService {

	public static final int DEFAULT_MAX_INTAKE_DAYS = 90;
	/* 섭취 일수 입력의 처음 값 — 상품상세 · 조합 탭이 함께 쓴다 (상한 · 재고보다 크면 그 값으로 줄인다) */
	public static final int DEFAULT_DAYS = 30;
	private static final int[] QUICK_DAYS = { 7, 14, 30, 60, 90 };

	private final ConfigDAO configDAO = new ConfigDAO();

	public boolean isMaintenance() throws SQLException {
		return "MAINTENANCE".equals(configDAO.findValue(ConfigDAO.SHOP_STATUS));
	}

	public int deliveryFee() throws SQLException {
		return configDAO.findInt(ConfigDAO.DELIVERY_FEE, 3000);
	}

	public int freeShippingMin() throws SQLException {
		return configDAO.findInt(ConfigDAO.FREE_SHIPPING_MIN, 50000);
	}

	public int lowStockThreshold() throws SQLException {
		return configDAO.findInt(ConfigDAO.LOW_STOCK_THRESHOLD, 30);
	}

	/* 섭취 일수 상한 — 파싱 실패 · 키 없음이면 코드 기본값 90 (ERD § 7 설정값) */
	public int maxIntakeDays() throws SQLException {
		int days = configDAO.findInt(ConfigDAO.MAX_INTAKE_DAYS, DEFAULT_MAX_INTAKE_DAYS);
		return days > 0 ? days : DEFAULT_MAX_INTAKE_DAYS;
	}

	/* 섭취 일수 빠른 선택 — 상한을 넘는 값은 뺀다 (CONCEPT § 5) */
	public java.util.List<Integer> quickDays() throws SQLException {
		int max = maxIntakeDays();
		java.util.List<Integer> list = new java.util.ArrayList<Integer>();
		for (int d : QUICK_DAYS) {
			if (d <= max) {
				list.add(Integer.valueOf(d));
			}
		}
		return list;
	}

	/*
	  배송비 계산 — 주문 시점에 한 번 계산해 orders 에 복사 저장한다.
	      delivery_fee = (total_price >= FREE_SHIPPING_MIN) ? 0 : DELIVERY_FEE
	  설정이 바뀌어도 과거 주문 금액이 변하지 않는다 (ERD § 2.8).
	*/
	public int calculateDeliveryFee(int totalPrice) throws SQLException {
		if (totalPrice <= 0) {
			return 0;
		}
		return totalPrice >= freeShippingMin() ? 0 : deliveryFee();
	}

	public List<SystemConfigDTO> findAll() throws SQLException {
		return configDAO.findAll();
	}

	public Map<String, String> findAllAsMap() throws SQLException {
		return configDAO.findAllAsMap();
	}
}
