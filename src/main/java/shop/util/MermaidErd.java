package shop.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
  mermaid erDiagram → SVG 이미지.

  /dev/erd 에서 README 의 ERD 를 그림으로 보여 주기 위한 것이다.
  mermaid.js 는 CDN 로드가 금지돼 있고 저장소에도 없어서, 필요한 만큼만 직접 해석해 그린다.

  지원하는 문법 (README 블록이 쓰는 범위)
      A ||--o{ B : "라벨"        관계. --(실선) / ..(점선)
      A { type name PK "설명" }  엔티티와 속성. 키는 PK · FK · UK 와 "FK,UK" 조합

  배치는 자동 레이아웃 대신 이 프로젝트의 테이블에 맞춘 고정 배치를 쓴다(LAYOUT).
  목록에 없는 엔티티가 생기면 맨 오른쪽 열에 이어 붙인다 — 그림이 깨지지 않고 늘어날 뿐이다.
*/
public class MermaidErd {

	/*
	  열 단위 배치. 모든 관계가 바로 옆 열끼리만 이어지도록 짰다 — 열을 건너뛰는 선은
	  가운데 열 상자 밑을 지나가 읽을 수 없게 된다.
	      MEMBER 쪽 1:N  →  둘째 열 (MEMBER 와 PRODUCT 를 함께 참조하는 표가 여기 모인다)
	      PRODUCT · 주문 계열  →  셋째 열,  같은 열 관계(PRODUCT-ORDER_ITEM)는 서로 이웃
	  열 안의 위아래 순서는 상대편 위치에 맞춰 선이 덜 꼬이게 정했다.
	*/
	private static final String[][] LAYOUT = {
		{ "ADMIN_LOG", "ADDRESS", "NOTICE", "SYSTEM_CONFIG" },
		{ "MEMBER" },
		{ "ORDERS", "DELIVERY_HISTORY", "REVIEW", "QNA", "WISHLIST", "CART" },
		{ "PAYMENT", "DELIVERY", "ORDER_ITEM", "PRODUCT" },
		{ "PRODUCT_IMAGE", "CATEGORY" } };

	private static final double FONT = 12;
	private static final double HEADER_H = 26;
	private static final double ROW_H = 20;
	private static final double PAD = 8;
	private static final double CELL_GAP = 10;
	private static final double COL_GAP = 110;
	private static final double ROW_GAP = 40;
	private static final double MARGIN = 30;

	private static final Pattern RELATION = Pattern.compile(
		"^\\s*([A-Za-z0-9_]+)\\s+([|}o][|o])(--|\\.\\.)([|o][|{o])\\s+([A-Za-z0-9_]+)\\s*:\\s*\"?([^\"]*)\"?\\s*$");
	private static final Pattern ENTITY_OPEN = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*\\{\\s*$");
	private static final Pattern ATTRIBUTE = Pattern.compile(
		"^\\s*(\\S+)\\s+(\\S+)(?:\\s+((?:PK|FK|UK)(?:\\s*,\\s*(?:PK|FK|UK))*))?(?:\\s+\"([^\"]*)\")?\\s*$");

	private static class Attr {
		String type;
		String name;
		String keys;
		String comment;
	}

	private static class Entity {
		String name;
		List<Attr> attrs = new ArrayList<Attr>();
		double x;
		double y;
		double w;
		double h;
		/* type · name · keys · comment 칸 너비 */
		double[] cell = new double[4];
	}

	private static class Relation {
		String from;
		String to;
		String fromCard;
		String toCard;
		String label;
		boolean dashed;
		Anchor a;
		Anchor b;
	}

	/* 상자 테두리 위의 연결점과 바깥 방향 */
	private static class Anchor {
		double x;
		double y;
		double nx;
		double ny;
	}

	private final Map<String, Entity> entities = new LinkedHashMap<String, Entity>();
	private final List<Relation> relations = new ArrayList<Relation>();

	/* 마크다운에서 erDiagram 이 든 첫 mermaid 블록만 꺼낸다. 없으면 null. */
	public static String extractBlock(String markdown) {
		if (markdown == null) {
			return null;
		}
		String[] lines = markdown.split("\r?\n");
		StringBuilder block = null;
		for (String line : lines) {
			String trimmed = line.trim();
			if (block == null) {
				if (trimmed.equals("```mermaid")) {
					block = new StringBuilder();
				}
				continue;
			}
			if (trimmed.equals("```")) {
				if (block.indexOf("erDiagram") >= 0) {
					return block.toString();
				}
				block = null;
				continue;
			}
			block.append(line).append('\n');
		}
		return null;
	}

	public static MermaidErd parse(String source) {
		MermaidErd erd = new MermaidErd();
		Entity open = null;
		for (String line : source.split("\r?\n")) {
			String trimmed = line.trim();
			if (trimmed.length() == 0 || trimmed.startsWith("%%") || trimmed.equals("erDiagram")) {
				continue;
			}
			if (open != null) {
				if (trimmed.equals("}")) {
					open = null;
					continue;
				}
				Matcher m = ATTRIBUTE.matcher(trimmed);
				if (m.matches()) {
					Attr attr = new Attr();
					attr.type = m.group(1);
					attr.name = m.group(2);
					attr.keys = m.group(3) == null ? "" : m.group(3).replaceAll("\\s+", "");
					attr.comment = m.group(4) == null ? "" : m.group(4);
					open.attrs.add(attr);
				}
				continue;
			}
			Matcher m = ENTITY_OPEN.matcher(trimmed);
			if (m.matches()) {
				open = erd.entity(m.group(1));
				continue;
			}
			m = RELATION.matcher(trimmed);
			if (m.matches()) {
				Relation r = new Relation();
				r.from = erd.entity(m.group(1)).name;
				r.fromCard = cardinality(m.group(2));
				r.dashed = "..".equals(m.group(3));
				r.toCard = cardinality(m.group(4));
				r.to = erd.entity(m.group(5)).name;
				r.label = m.group(6).trim();
				erd.relations.add(r);
			}
		}
		return erd;
	}

	/* ||, |o / o|, }o / o{, }| / |{ → 4가지로 정규화 */
	private static String cardinality(String token) {
		boolean many = token.indexOf('{') >= 0 || token.indexOf('}') >= 0;
		boolean zero = token.indexOf('o') >= 0;
		if (many) {
			return zero ? "zeroOrMore" : "oneOrMore";
		}
		return zero ? "zeroOrOne" : "exactlyOne";
	}

	private Entity entity(String name) {
		Entity e = entities.get(name);
		if (e == null) {
			e = new Entity();
			e.name = name;
			entities.put(name, e);
		}
		return e;
	}

	public int entityCount() {
		return entities.size();
	}

	public int relationCount() {
		return relations.size();
	}

	/* ---------------- 배치 ---------------- */

	private List<List<Entity>> columns() {
		List<List<Entity>> cols = new ArrayList<List<Entity>>();
		Map<String, Boolean> placed = new HashMap<String, Boolean>();
		for (String[] names : LAYOUT) {
			List<Entity> col = new ArrayList<Entity>();
			for (String name : names) {
				Entity e = entities.get(name);
				if (e != null) {
					col.add(e);
					placed.put(name, Boolean.TRUE);
				}
			}
			if (!col.isEmpty()) {
				cols.add(col);
			}
		}
		List<Entity> rest = new ArrayList<Entity>();
		for (Entity e : entities.values()) {
			if (!placed.containsKey(e.name)) {
				rest.add(e);
			}
		}
		if (!rest.isEmpty()) {
			cols.add(rest);
		}
		return cols;
	}

	private void measure(Entity e) {
		double[] cell = e.cell;
		for (Attr a : e.attrs) {
			cell[0] = Math.max(cell[0], textWidth(a.type));
			cell[1] = Math.max(cell[1], textWidth(a.name));
			cell[2] = Math.max(cell[2], textWidth(a.keys));
			cell[3] = Math.max(cell[3], textWidth(a.comment));
		}
		double inner = 0;
		int used = 0;
		for (double c : cell) {
			if (c > 0) {
				inner += c;
				used++;
			}
		}
		inner += Math.max(0, used - 1) * CELL_GAP;
		e.w = Math.max(Math.max(inner, textWidth(e.name) + 10) + PAD * 2, 150);
		e.h = HEADER_H + e.attrs.size() * ROW_H + (e.attrs.isEmpty() ? 6 : 4);
	}

	/* 화면 폭 근사치 — 한글은 영문 두 배 남짓으로 잡는다 */
	private static double textWidth(String s) {
		if (s == null) {
			return 0;
		}
		double w = 0;
		for (int i = 0; i < s.length(); i++) {
			w += s.charAt(i) < 0x80 ? FONT * 0.6 : FONT * 1.05;
		}
		return w;
	}

	private double[] layout() {
		List<List<Entity>> cols = columns();
		double[] colHeight = new double[cols.size()];
		double[] colWidth = new double[cols.size()];
		double tallest = 0;
		for (int i = 0; i < cols.size(); i++) {
			for (Entity e : cols.get(i)) {
				measure(e);
				colWidth[i] = Math.max(colWidth[i], e.w);
				colHeight[i] += e.h;
			}
			colHeight[i] += (cols.get(i).size() - 1) * ROW_GAP;
			tallest = Math.max(tallest, colHeight[i]);
		}

		double x = MARGIN;
		for (int i = 0; i < cols.size(); i++) {
			/* 열마다 세로 가운데 정렬 — MEMBER 처럼 혼자 선 열이 위에 붙지 않게 */
			double y = MARGIN + (tallest - colHeight[i]) / 2;
			for (Entity e : cols.get(i)) {
				e.x = x + (colWidth[i] - e.w) / 2;
				e.y = y;
				y += e.h + ROW_GAP;
			}
			x += colWidth[i] + COL_GAP;
		}
		return new double[] { x - COL_GAP + MARGIN, tallest + MARGIN * 2 };
	}

	/* ---------------- 연결점 ---------------- */

	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	private static final int TOP = 2;
	private static final int BOTTOM = 3;

	private void assignAnchors() {
		/* 엔티티 · 변별 연결 요청을 모은 뒤 상대편 위치 순으로 정렬해 고르게 나눈다 — 선 꼬임을 줄인다 */
		Map<String, List<Object[]>> slots = new LinkedHashMap<String, List<Object[]>>();
		for (Relation r : relations) {
			Entity a = entities.get(r.from);
			Entity b = entities.get(r.to);
			int sa;
			int sb;
			if (a == b) {
				sa = RIGHT;
				sb = RIGHT;
			} else if (Math.abs(cx(a) - cx(b)) > 1) {
				sa = cx(a) < cx(b) ? RIGHT : LEFT;
				sb = cx(a) < cx(b) ? LEFT : RIGHT;
			} else {
				sa = cy(a) < cy(b) ? BOTTOM : TOP;
				sb = cy(a) < cy(b) ? TOP : BOTTOM;
			}
			r.a = new Anchor();
			r.b = new Anchor();
			slot(slots, a, sa).add(new Object[] { r.a, b, Boolean.valueOf(a == b) });
			slot(slots, b, sb).add(new Object[] { r.b, a, Boolean.valueOf(a == b) });
		}

		for (Map.Entry<String, List<Object[]>> entry : slots.entrySet()) {
			String[] key = entry.getKey().split("#");
			final Entity e = entities.get(key[0]);
			final int side = Integer.parseInt(key[1]);
			List<Object[]> list = entry.getValue();
			Collections.sort(list, new Comparator<Object[]>() {
				public int compare(Object[] p, Object[] q) {
					Entity op = (Entity) p[1];
					Entity oq = (Entity) q[1];
					double vp = side == LEFT || side == RIGHT ? cy(op) : cx(op);
					double vq = side == LEFT || side == RIGHT ? cy(oq) : cx(oq);
					return Double.compare(vp, vq);
				}
			});
			int n = list.size();
			for (int i = 0; i < n; i++) {
				Anchor an = (Anchor) list.get(i)[0];
				double t = (i + 1.0) / (n + 1.0);
				if (side == LEFT || side == RIGHT) {
					double top = e.y + HEADER_H / 2;
					an.x = side == LEFT ? e.x : e.x + e.w;
					an.y = top + (e.y + e.h - top) * t;
					an.nx = side == LEFT ? -1 : 1;
					an.ny = 0;
				} else {
					an.x = e.x + e.w * t;
					an.y = side == TOP ? e.y : e.y + e.h;
					an.nx = 0;
					an.ny = side == TOP ? -1 : 1;
				}
			}
		}
	}

	private List<Object[]> slot(Map<String, List<Object[]>> slots, Entity e, int side) {
		String key = e.name + "#" + side;
		List<Object[]> list = slots.get(key);
		if (list == null) {
			list = new ArrayList<Object[]>();
			slots.put(key, list);
		}
		return list;
	}

	private static double cx(Entity e) {
		return e.x + e.w / 2;
	}

	private static double cy(Entity e) {
		return e.y + e.h / 2;
	}

	/* ---------------- SVG ---------------- */

	/* 원본 크기 그대로 — 이미지 파일로 내려보낼 때 */
	public String toSvg() {
		return toSvg(false);
	}

	/*
	  fit = true 면 폭을 감싸는 영역에 맞춘다 (width 100%, 높이는 viewBox 비율로 자동).
	  페이지에 직접 넣을 때 쓴다 — 원본 폭이 화면보다 넓어도 가로 스크롤이 생기지 않는다.
	*/
	public String toSvg(boolean fit) {
		double[] size = layout();
		assignAnchors();

		StringBuilder svg = new StringBuilder();
		svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
		   .append(fit ? "100%" : fmt(size[0])).append('"');
		if (!fit) {
			svg.append(" height=\"").append(fmt(size[1])).append('"');
		}
		svg.append(" viewBox=\"0 0 ")
		   .append(fmt(size[0])).append(' ').append(fmt(size[1]))
		   .append("\" font-family=\"'Apple SD Gothic Neo','Malgun Gothic',sans-serif\" font-size=\"")
		   .append(fmt(FONT)).append("\">\n");
		svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");

		/* 그리는 순서: 선 → 상자 → 끝 기호 → 라벨. 라벨은 상자 위에 와야 읽힌다. */
		List<double[]> labelPoints = new ArrayList<double[]>();
		for (Relation r : relations) {
			double[] c = curve(r);
			svg.append("<path d=\"M").append(fmt(c[0])).append(' ').append(fmt(c[1]))
			   .append(" C").append(fmt(c[2])).append(' ').append(fmt(c[3])).append(' ')
			   .append(fmt(c[4])).append(' ').append(fmt(c[5])).append(' ')
			   .append(fmt(c[6])).append(' ').append(fmt(c[7]))
			   .append("\" fill=\"none\" stroke=\"#666666\" stroke-width=\"1.2\"")
			   .append(r.dashed ? " stroke-dasharray=\"6 4\"" : "").append("/>\n");
			labelPoints.add(new double[] {
				0.125 * c[0] + 0.375 * c[2] + 0.375 * c[4] + 0.125 * c[6],
				0.125 * c[1] + 0.375 * c[3] + 0.375 * c[5] + 0.125 * c[7] });
		}

		for (Entity e : entities.values()) {
			appendEntity(svg, e);
		}

		for (Relation r : relations) {
			appendMarker(svg, r.a, r.fromCard);
			appendMarker(svg, r.b, r.toCard);
		}

		for (int i = 0; i < relations.size(); i++) {
			String label = relations.get(i).label;
			if (label.length() == 0) {
				continue;
			}
			double[] p = labelPoints.get(i);
			double w = textWidth(label) + 8;
			svg.append("<rect x=\"").append(fmt(p[0] - w / 2)).append("\" y=\"").append(fmt(p[1] - 9))
			   .append("\" width=\"").append(fmt(w)).append("\" height=\"18\" rx=\"3\" fill=\"#ffffff\"")
			   .append(" stroke=\"#bbbbbb\" stroke-width=\"0.8\"/>\n");
			svg.append("<text x=\"").append(fmt(p[0])).append("\" y=\"").append(fmt(p[1] + 4))
			   .append("\" text-anchor=\"middle\" fill=\"#333333\">").append(esc(label)).append("</text>\n");
		}

		svg.append("</svg>\n");
		return svg.toString();
	}

	/* 연결점 바깥 방향으로 제어점을 빼서 상자에서 수직으로 나가는 곡선을 만든다 */
	private double[] curve(Relation r) {
		Anchor a = r.a;
		Anchor b = r.b;
		double dist = Math.hypot(b.x - a.x, b.y - a.y);
		double c = r.from.equals(r.to) ? 60 : Math.max(45, Math.min(170, dist * 0.4));
		return new double[] {
			a.x, a.y,
			a.x + a.nx * c, a.y + a.ny * c,
			b.x + b.nx * c, b.y + b.ny * c,
			b.x, b.y };
	}

	private void appendEntity(StringBuilder svg, Entity e) {
		svg.append("<g>\n");
		svg.append("<rect x=\"").append(fmt(e.x)).append("\" y=\"").append(fmt(e.y))
		   .append("\" width=\"").append(fmt(e.w)).append("\" height=\"").append(fmt(e.h))
		   .append("\" fill=\"#ffffff\" stroke=\"#444444\" stroke-width=\"1.2\"/>\n");
		svg.append("<rect x=\"").append(fmt(e.x)).append("\" y=\"").append(fmt(e.y))
		   .append("\" width=\"").append(fmt(e.w)).append("\" height=\"").append(fmt(HEADER_H))
		   .append("\" fill=\"#e9e9e9\" stroke=\"#444444\" stroke-width=\"1.2\"/>\n");
		svg.append("<text x=\"").append(fmt(e.x + e.w / 2)).append("\" y=\"").append(fmt(e.y + 17))
		   .append("\" text-anchor=\"middle\" font-weight=\"bold\" fill=\"#222222\">")
		   .append(esc(e.name)).append("</text>\n");

		double[] start = new double[4];
		double x = e.x + PAD;
		for (int i = 0; i < 4; i++) {
			start[i] = x;
			if (e.cell[i] > 0) {
				x += e.cell[i] + CELL_GAP;
			}
		}
		double y = e.y + HEADER_H;
		for (int i = 0; i < e.attrs.size(); i++) {
			Attr a = e.attrs.get(i);
			double base = y + i * ROW_H + 14;
			if (i > 0) {
				svg.append("<line x1=\"").append(fmt(e.x)).append("\" y1=\"").append(fmt(y + i * ROW_H))
				   .append("\" x2=\"").append(fmt(e.x + e.w)).append("\" y2=\"").append(fmt(y + i * ROW_H))
				   .append("\" stroke=\"#eeeeee\"/>\n");
			}
			appendText(svg, start[0], base, a.type, "#888888", false);
			appendText(svg, start[1], base, a.name, "#222222", a.keys.indexOf("PK") >= 0);
			appendText(svg, start[2], base, a.keys, "#555555", true);
			appendText(svg, start[3], base, a.comment, "#888888", false);
		}
		svg.append("</g>\n");
	}

	private void appendText(StringBuilder svg, double x, double y, String text, String color,
			boolean bold) {
		if (text == null || text.length() == 0) {
			return;
		}
		svg.append("<text x=\"").append(fmt(x)).append("\" y=\"").append(fmt(y)).append("\" fill=\"")
		   .append(color).append('"').append(bold ? " font-weight=\"bold\"" : "").append('>')
		   .append(esc(text)).append("</text>\n");
	}

	/*
	  카디널리티 기호 (까마귀발 표기). 로컬 좌표에서 +x 가 상자 바깥 방향이고,
	  연결점의 바깥 방향 벡터로 회전시켜 붙인다.
	      exactlyOne  ||      zeroOrOne  o|      oneOrMore  |<      zeroOrMore  o<
	*/
	private void appendMarker(StringBuilder svg, Anchor an, String card) {
		double angle = Math.toDegrees(Math.atan2(an.ny, an.nx));
		svg.append("<g transform=\"translate(").append(fmt(an.x)).append(' ').append(fmt(an.y))
		   .append(") rotate(").append(fmt(angle)).append(")\" stroke=\"#444444\" stroke-width=\"1.3\" fill=\"#ffffff\">");
		boolean many = card.endsWith("OrMore");
		boolean zero = card.startsWith("zero");

		/* 상자 쪽(안쪽) 기호: 여럿이면 까마귀발, 하나면 막대 */
		double next;
		if (many) {
			svg.append("<path d=\"M0 -7 L11 0 M0 0 L11 0 M0 7 L11 0\" fill=\"none\"/>");
			next = 15;
		} else {
			appendBar(svg, 7);
			next = 12;
		}
		/* 바깥쪽 기호: 0 을 허용하면 원, 아니면 막대 */
		if (zero) {
			svg.append("<circle cx=\"").append(fmt(next + 4)).append("\" cy=\"0\" r=\"4\"/>");
		} else {
			appendBar(svg, next);
		}
		svg.append("</g>\n");
	}

	private void appendBar(StringBuilder svg, double x) {
		svg.append("<path d=\"M").append(fmt(x)).append(" -6 L").append(fmt(x)).append(" 6\"/>");
	}

	private static String fmt(double v) {
		return String.valueOf(Math.round(v * 10) / 10.0);
	}

	private static String esc(String s) {
		StringBuilder b = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '&') {
				b.append("&amp;");
			} else if (c == '<') {
				b.append("&lt;");
			} else if (c == '>') {
				b.append("&gt;");
			} else if (c == '"') {
				b.append("&quot;");
			} else {
				b.append(c);
			}
		}
		return b.toString();
	}
}
