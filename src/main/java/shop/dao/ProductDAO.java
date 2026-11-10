package shop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import shop.dto.PageDTO;
import shop.dto.ProductDTO;
import shop.dto.ProductImageDTO;
import shop.util.DBUtil;
import shop.util.Paging;

/*
  product · product_image — 상품.

  ★ 노출 규칙 (ERD § 2.4)
    고객이 상품을 찾아가는 경로에는 WHERE status = '판매중' AND stock > 0 이 붙는다.
    목록 · 검색 · 상세(직접 URL 포함) 전부다. 관리자 조회에는 붙지 않는다.
    그래서 메서드 이름을 findOnSale* / findAny* 로 갈라 두었다 — 호출부에서
    어느 쪽을 쓰는지가 이름으로 드러나야 조건을 빠뜨리지 않는다.

  ★ product_type — 낱개(SINGLE) / 트랙(TRACK). price 는 1포 가격, stock 은 남은 포 수다 (ERD § 7.1).
    조합 탭(/custom)은 낱개 목록을 findOnSaleByType("SINGLE") 로 받는다.

  ★ 평균 별점은 product 컬럼이 아니다 (CRUD.md § 7).
    목록에서 상품마다 집계하면 N+1 이 되므로 LEFT JOIN 서브쿼리로 한 번에 받는다.
*/
public class ProductDAO {

	/* 노출 조건 — 한 군데서만 정의한다. */
	private static final String ON_SALE = " p.status = '판매중' AND p.stock > 0 ";

	private static final String SELECT_BASE =
		"SELECT p.product_id, p.category_id, p.product_type, p.name, p.price, p.stock, p.description,"
	  + "       p.thumbnail, p.status, p.created_at,"
	  + "       c.name AS category_name,"
	  + "       IFNULL(r.avg_rating, 0) AS avg_rating,"
	  + "       IFNULL(r.review_count, 0) AS review_count"
	  + "  FROM product p"
	  + "  JOIN category c ON c.category_id = p.category_id"
	  + "  LEFT JOIN (SELECT product_id, AVG(rating) AS avg_rating, COUNT(*) AS review_count"
	  + "               FROM review GROUP BY product_id) r ON r.product_id = p.product_id";

	/* ---------------- 고객 화면 ---------------- */

	public static final String SINGLE = "SINGLE";
	public static final String TRACK = "TRACK";

	/* 구분 값 검사 — 화면에서 온 값을 SQL 에 그대로 쓰지 않는다. */
	public static boolean isType(String type) {
		return SINGLE.equals(type) || TRACK.equals(type);
	}

	/* 메인 — 최신 상품 */
	public List<ProductDTO> findLatestOnSale(int limit) throws SQLException {
		String sql = SELECT_BASE + " WHERE" + ON_SALE
				   + " ORDER BY p.created_at DESC, p.product_id DESC LIMIT ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		List<ProductDTO> list = new ArrayList<ProductDTO>();
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, limit);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs));
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* 메인 트랙 패키지 영역 — 구분별 최신 상품 */
	public List<ProductDTO> findLatestOnSaleByType(String type, int limit) throws SQLException {
		String sql = SELECT_BASE + " WHERE" + ON_SALE + " AND p.product_type = ?"
				   + " ORDER BY p.created_at DESC, p.product_id DESC LIMIT ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		List<ProductDTO> list = new ArrayList<ProductDTO>();
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, type);
			pstmt.setInt(2, limit);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs));
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/*
	  조합 탭 — 노출 조건을 통과한 구분별 전체 목록 (ERD § 7.1 조합 탭 노출).
	  분류 탭으로 나눠 보여주므로 카테고리 순서 → 이름 순으로 받는다.
	*/
	public List<ProductDTO> findOnSaleByType(String type) throws SQLException {
		String sql = SELECT_BASE + " WHERE" + ON_SALE + " AND p.product_type = ?"
				   + " ORDER BY c.sort_order, p.category_id, p.name, p.product_id";
		return findList(sql, type);
	}

	/* 관리자 — 트랙 구성 · 금지 조합 고르기용 낱개 전체 (노출 조건 없음) */
	public List<ProductDTO> findAnyByType(String type) throws SQLException {
		String sql = SELECT_BASE + " WHERE p.product_type = ?"
				   + " ORDER BY c.sort_order, p.category_id, p.name, p.product_id";
		return findList(sql, type);
	}

	/* 여러 상품을 한 번에 — 경고 계산이 이름 · 구분을 얻는 데 쓴다. 노출 조건 없음. */
	public Map<Integer, ProductDTO> findAnyByIds(Collection<Integer> ids) throws SQLException {
		Map<Integer, ProductDTO> map = new LinkedHashMap<Integer, ProductDTO>();
		if (ids == null || ids.isEmpty()) {
			return map;
		}
		String sql = SELECT_BASE + " WHERE p.product_id IN (" + placeholders(ids.size()) + ")";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			int i = 1;
			for (Integer id : ids) {
				pstmt.setInt(i++, id.intValue());
			}
			rs = pstmt.executeQuery();
			while (rs.next()) {
				ProductDTO dto = map(rs);
				map.put(Integer.valueOf(dto.getProductId()), dto);
			}
			return map;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/*
	  한 대분류(그 밑 소분류 포함)에 있는 특정 구분의 상품 수 — 자기 자신은 뺀다.
	  낱개 / 트랙을 같은 대분류에 섞지 않는 규칙(ERD § 8-12)을 대분류 이름 없이 판정한다.
	*/
	public int countUnderRoot(int rootId, String type, int exceptProductId) throws SQLException {
		List<Object> args = new ArrayList<Object>();
		args.add(Integer.valueOf(rootId));
		args.add(Integer.valueOf(rootId));
		args.add(type);
		args.add(Integer.valueOf(exceptProductId));
		return count("SELECT COUNT(*) FROM product p"
				+ " WHERE (p.category_id = ? OR p.category_id IN (SELECT category_id FROM category WHERE parent_id = ?))"
				+ " AND p.product_type = ? AND p.product_id <> ?", args);
	}

	/* 판매 이력 — product_type 은 이력이 생긴 뒤 바꾸지 않는다 (ERD § 8-15). */
	public int countOrderItems(int productId) throws SQLException {
		List<Object> args = new ArrayList<Object>();
		args.add(Integer.valueOf(productId));
		return count("SELECT COUNT(*) FROM order_item WHERE product_id = ?", args);
	}

	/*
	  상품 목록 — 카테고리 · 검색어 · 정렬.

	  정렬 컬럼은 파라미터를 그대로 붙이지 않는다. 화이트리스트로 변환한다
	  (ERD § 6.5 — ORDER BY 도 인젝션 표면이다).
	*/
	public PageDTO<ProductDTO> findOnSalePage(Integer categoryId, String type, String keyword,
			String sort, int page, int size) throws SQLException {
		StringBuilder where = new StringBuilder(" WHERE" + ON_SALE);
		List<Object> args = new ArrayList<Object>();

		/* 낱개 / 트랙 구분 필터 — 허용 값만 바인딩한다. */
		if (isType(type)) {
			where.append(" AND p.product_type = ?");
			args.add(type);
		}

		if (categoryId != null && categoryId.intValue() > 0) {
			/* 대분류를 고르면 그 하위 소분류 상품까지 함께 보여준다. */
			where.append(" AND (p.category_id = ?"
					   + " OR p.category_id IN (SELECT category_id FROM category WHERE parent_id = ?))");
			args.add(categoryId);
			args.add(categoryId);
		}
		if (keyword != null && keyword.length() > 0) {
			where.append(" AND p.name LIKE ?");
			args.add("%" + keyword + "%");
		}

		int total = count("SELECT COUNT(*) FROM product p" + where, args);

		String sql = SELECT_BASE + where + orderBy(sort) + " LIMIT ? OFFSET ?";
		List<ProductDTO> list = new ArrayList<ProductDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			int i = bind(pstmt, args);
			pstmt.setInt(i++, Paging.normalizeSize(size));
			pstmt.setInt(i, Paging.offset(page, size));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs));
			}
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
		return new PageDTO<ProductDTO>(list, page, size, total);
	}

	/* 정렬 키 → SQL 변환. 목록에 없는 값은 기본 정렬로 떨어뜨린다. */
	private String orderBy(String sort) {
		if ("priceAsc".equals(sort)) {
			return " ORDER BY p.price ASC, p.product_id DESC";
		}
		if ("priceDesc".equals(sort)) {
			return " ORDER BY p.price DESC, p.product_id DESC";
		}
		if ("rating".equals(sort)) {
			return " ORDER BY avg_rating DESC, review_count DESC, p.product_id DESC";
		}
		if ("name".equals(sort)) {
			return " ORDER BY p.name ASC, p.product_id DESC";
		}
		return " ORDER BY p.created_at DESC, p.product_id DESC";
	}

	/* 상품 상세 — 노출 조건을 적용한다. 직접 URL 로 숨김 상품에 닿을 수 없다. */
	public ProductDTO findOnSaleById(int productId) throws SQLException {
		return findOne(SELECT_BASE + " WHERE p.product_id = ? AND" + ON_SALE, productId);
	}

	/* ---------------- 관리자 화면 ---------------- */

	/* 노출 조건 없이 전부 본다. */
	public ProductDTO findAnyById(int productId) throws SQLException {
		return findOne(SELECT_BASE + " WHERE p.product_id = ?", productId);
	}

	public PageDTO<ProductDTO> findAnyPage(Integer categoryId, String type, String keyword,
			String status, int page, int size) throws SQLException {
		StringBuilder where = new StringBuilder(" WHERE 1 = 1");
		List<Object> args = new ArrayList<Object>();
		if (categoryId != null && categoryId.intValue() > 0) {
			where.append(" AND (p.category_id = ?"
					   + " OR p.category_id IN (SELECT category_id FROM category WHERE parent_id = ?))");
			args.add(categoryId);
			args.add(categoryId);
		}
		if (isType(type)) {
			where.append(" AND p.product_type = ?");
			args.add(type);
		}
		if (keyword != null && keyword.length() > 0) {
			where.append(" AND p.name LIKE ?");
			args.add("%" + keyword + "%");
		}
		if ("판매중".equals(status) || "숨김".equals(status)) {
			where.append(" AND p.status = ?");
			args.add(status);
		}

		int total = count("SELECT COUNT(*) FROM product p" + where, args);
		String sql = SELECT_BASE + where + " ORDER BY p.product_id DESC LIMIT ? OFFSET ?";

		List<ProductDTO> list = new ArrayList<ProductDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			int i = bind(pstmt, args);
			pstmt.setInt(i++, Paging.normalizeSize(size));
			pstmt.setInt(i, Paging.offset(page, size));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs));
			}
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
		return new PageDTO<ProductDTO>(list, page, size, total);
	}

	public int insert(ProductDTO product) throws SQLException {
		String sql = "INSERT INTO product"
				   + " (category_id, product_type, name, price, stock, description, thumbnail, status)"
				   + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			pstmt.setInt(1, product.getCategoryId());
			pstmt.setString(2, isType(product.getProductType()) ? product.getProductType() : SINGLE);
			pstmt.setString(3, product.getName());
			pstmt.setInt(4, product.getPrice());
			pstmt.setInt(5, product.getStock());
			pstmt.setString(6, product.getDescription());
			pstmt.setString(7, product.getThumbnail());
			pstmt.setString(8, product.getStatus());
			pstmt.executeUpdate();
			rs = pstmt.getGeneratedKeys();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public int update(ProductDTO product) throws SQLException {
		String sql = "UPDATE product SET category_id = ?, product_type = ?, name = ?, price = ?,"
				   + " stock = ?, description = ?, thumbnail = ?, status = ? WHERE product_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, product.getCategoryId());
			pstmt.setString(2, isType(product.getProductType()) ? product.getProductType() : SINGLE);
			pstmt.setString(3, product.getName());
			pstmt.setInt(4, product.getPrice());
			pstmt.setInt(5, product.getStock());
			pstmt.setString(6, product.getDescription());
			pstmt.setString(7, product.getThumbnail());
			pstmt.setString(8, product.getStatus());
			pstmt.setInt(9, product.getProductId());
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	/*
	  order_item FK 가 RESTRICT 라 주문된 상품은 삭제에 실패한다 — 화면은 '숨김' 을 권한다.
	  트랙 구성품 · 금지 조합에 걸린 낱개도 RESTRICT 라 먼저 빼야 지워진다 (schema.sql 3-1 · 3-2).
	*/
	public int delete(int productId) throws SQLException {
		String sql = "DELETE FROM product WHERE product_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, productId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	/* ---------------- 재고 ---------------- */

	/*
	  재고 차감 — 결제 트랜잭션에서 쓴다.

	  WHERE stock >= ? 가 핵심이다. 영향 행이 0 이면 재고 부족이므로 호출부가 롤백한다.
	  이 조건이 없으면 UNSIGNED 컬럼이 음수가 되면서 SQLException 으로 터진다. (ERD § 6.1)
	*/
	public int decreaseStock(Connection conn, int productId, int quantity) throws SQLException {
		String sql = "UPDATE product SET stock = stock - ?"
				   + " WHERE product_id = ? AND stock >= ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, quantity);
			pstmt.setInt(2, productId);
			pstmt.setInt(3, quantity);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* 주문 취소 시 재고 복구 (ERD § 6.3) */
	public int increaseStock(Connection conn, int productId, int quantity) throws SQLException {
		String sql = "UPDATE product SET stock = stock + ? WHERE product_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, quantity);
			pstmt.setInt(2, productId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* /dev 강제 재고 지정 */
	public int setStock(Connection conn, int productId, int stock) throws SQLException {
		String sql = "UPDATE product SET stock = ? WHERE product_id = ?";
		PreparedStatement pstmt = null;
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, stock);
			pstmt.setInt(2, productId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt);
		}
	}

	/* ---------------- product_image ---------------- */

	public List<ProductImageDTO> findImages(int productId) throws SQLException {
		String sql = "SELECT image_id, product_id, img_path, sort_order"
				   + "  FROM product_image WHERE product_id = ? ORDER BY sort_order, image_id";
		List<ProductImageDTO> list = new ArrayList<ProductImageDTO>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, productId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				ProductImageDTO dto = new ProductImageDTO();
				dto.setImageId(rs.getInt("image_id"));
				dto.setProductId(rs.getInt("product_id"));
				dto.setImgPath(rs.getString("img_path"));
				dto.setSortOrder(rs.getInt("sort_order"));
				list.add(dto);
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* UNIQUE (product_id, sort_order) 때문에 순서값은 비어 있는 다음 번호를 쓴다. */
	public int nextImageSortOrder(int productId) throws SQLException {
		String sql = "SELECT IFNULL(MAX(sort_order), 0) + 1 FROM product_image WHERE product_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, productId);
			rs = pstmt.executeQuery();
			return rs.next() ? rs.getInt(1) : 1;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	public int insertImage(int productId, String imgPath, int sortOrder) throws SQLException {
		String sql = "INSERT INTO product_image (product_id, img_path, sort_order) VALUES (?, ?, ?)";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, productId);
			pstmt.setString(2, imgPath);
			pstmt.setInt(3, sortOrder);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	public int deleteImage(int imageId) throws SQLException {
		String sql = "DELETE FROM product_image WHERE image_id = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, imageId);
			return pstmt.executeUpdate();
		} finally {
			DBUtil.close(pstmt, conn);
		}
	}

	/* ---------------- 공통 ---------------- */

	private List<ProductDTO> findList(String sql, String type) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		List<ProductDTO> list = new ArrayList<ProductDTO>();
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, type);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(map(rs));
			}
			return list;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	/* IN 절 자리표시자 — 값은 언제나 바인딩한다. */
	static String placeholders(int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) {
			sb.append(i == 0 ? "?" : ", ?");
		}
		return sb.toString();
	}

	private ProductDTO findOne(String sql, int productId) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, productId);
			rs = pstmt.executeQuery();
			return rs.next() ? map(rs) : null;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	private int count(String sql, List<Object> args) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			pstmt = conn.prepareStatement(sql);
			bind(pstmt, args);
			rs = pstmt.executeQuery();
			return rs.next() ? rs.getInt(1) : 0;
		} finally {
			DBUtil.close(rs, pstmt, conn);
		}
	}

	private int bind(PreparedStatement pstmt, List<Object> args) throws SQLException {
		int i = 1;
		for (Object arg : args) {
			pstmt.setObject(i++, arg);
		}
		return i;
	}

	private ProductDTO map(ResultSet rs) throws SQLException {
		ProductDTO dto = new ProductDTO();
		dto.setProductId(rs.getInt("product_id"));
		dto.setCategoryId(rs.getInt("category_id"));
		dto.setCategoryName(rs.getString("category_name"));
		dto.setProductType(rs.getString("product_type"));
		dto.setName(rs.getString("name"));
		dto.setPrice(rs.getInt("price"));
		dto.setStock(rs.getInt("stock"));
		dto.setDescription(rs.getString("description"));
		dto.setThumbnail(rs.getString("thumbnail"));
		dto.setStatus(rs.getString("status"));
		dto.setCreatedAt(rs.getTimestamp("created_at"));
		dto.setAvgRating(rs.getDouble("avg_rating"));
		dto.setReviewCount(rs.getInt("review_count"));
		return dto;
	}
}
