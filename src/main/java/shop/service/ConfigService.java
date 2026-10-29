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
		return configDAO.findInt(ConfigDAO.LOW_STOCK_THRESHOLD, 5);
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
