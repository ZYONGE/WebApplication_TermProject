package shop.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
  ERD 그림 — README 의 mermaid erDiagram 글을 읽어 SVG 그림으로 그린다 (/dev/erd).
  외부 라이브러리 · CDN 없이 서버에서 직접 그린다.

  1. parse()  : 엔티티(표)와 관계(선)를 읽는다
  2. layout() : 표를 정해 둔 열(LAYOUT)에 세로로 쌓고 크기를 잰다
  3. anchors(): 선이 표의 어느 변 어디에 붙을지 정한다
  4. toSvg()  : 선 → 표 → 관계 기호(까마귀발) → 관계 이름 순으로 그린다
*/
public class Erd {

	/* 열 배치 — 왼쪽 열부터. 목록에 없는 엔티티는 마지막 열에 */
	private static final String[][] LAYOUT = {
		{ "ADMIN_LOG", "ADDRESS", "NOTICE", "SYSTEM_CONFIG" },
		{ "MEMBER" },
		{ "ORDERS", "DELIVERY_HISTORY", "REVIEW", "QNA", "WISHLIST", "CART" },
		{ "PAYMENT", "DELIVERY", "ORDER_ITEM", "PRODUCT" },
		{ "PRODUCT_IMAGE", "CATEGORY" } };
	private static final double FONT = 12, HEADER_H = 26, ROW_H = 20, PAD = 8, CELL_GAP = 10;
	private static final double COL_GAP = 110, ROW_GAP = 40, MARGIN = 30;
	private static final int LEFT = 0, RIGHT = 1, TOP = 2, BOTTOM = 3;

	private static final Pattern RELATION = Pattern.compile(
		"^\\s*([A-Za-z0-9_]+)\\s+([|}o][|o])(--|\\.\\.)([|o][|{o])\\s+([A-Za-z0-9_]+)\\s*:\\s*\"?([^\"]*)\"?\\s*$");
	private static final Pattern ENTITY_OPEN = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*\\{\\s*$");
	private static final Pattern ATTRIBUTE = Pattern.compile(
		"^\\s*(\\S+)\\s+(\\S+)(?:\\s+((?:PK|FK|UK)(?:\\s*,\\s*(?:PK|FK|UK))*))?(?:\\s+\"([^\"]*)\")?\\s*$");

	/* 표 하나 — attrs 는 { 타입, 이름, 키, 설명 } */
	private static class Entity {
		String name;
		List<String[]> attrs = new ArrayList<String[]>();
		double x, y, w, h;
		double[] cell = new double[4];
	}

	/* 선이 붙는 점 (nx, ny 는 선이 나가는 방향) */
	private static class Anchor {
		double x, y, nx, ny;
	}

	private static class Relation {
		String from, to, fromCard, toCard, label;
		boolean dashed;
		Anchor a = new Anchor(), b = new Anchor();
	}

	private final Map<String, Entity> entities = new LinkedHashMap<String, Entity>();
	private final List<Relation> relations = new ArrayList<Relation>();

	/* 마크다운에서 erDiagram 이 든 ```mermaid 블록만 꺼낸다 */
	public static String extractBlock(String markdown) {
		StringBuilder block = null;
		for (String line : markdown.split("\r?\n")) {
			String t = line.trim();
			if (block == null) {
				if (t.equals("```mermaid")) {
					block = new StringBuilder();
				}
			} else if (t.equals("```")) {
				if (block.indexOf("erDiagram") >= 0) {
					return block.toString();
				}
				block = null;
			} else {
				block.append(line).append('\n');
			}
		}
		return null;
	}

	public static Erd parse(String source) {
		Erd erd = new Erd();
		Entity open = null;
		for (String line : source.split("\r?\n")) {
			String t = line.trim();
			if (t.isEmpty() || t.startsWith("%%") || t.equals("erDiagram")) {
				continue;
			}
			if (open != null) {
				Matcher m = ATTRIBUTE.matcher(t);
				if (t.equals("}")) {
					open = null;
				} else if (m.matches()) {
					open.attrs.add(new String[] { m.group(1), m.group(2),
						m.group(3) == null ? "" : m.group(3).replaceAll("\\s+", ""), m.group(4) == null ? "" : m.group(4) });
				}
				continue;
			}
			Matcher m = ENTITY_OPEN.matcher(t);
			if (m.matches()) {
				open = erd.entity(m.group(1));
			} else if ((m = RELATION.matcher(t)).matches()) {
				Relation r = new Relation();
				r.from = erd.entity(m.group(1)).name;
				r.fromCard = m.group(2);
				r.dashed = "..".equals(m.group(3));
				r.toCard = m.group(4);
				r.to = erd.entity(m.group(5)).name;
				r.label = m.group(6).trim();
				erd.relations.add(r);
			}
		}
		return erd;
	}

	private Entity entity(String name) {
		if (!entities.containsKey(name)) {
			Entity e = new Entity();
			e.name = name;
			entities.put(name, e);
		}
		return entities.get(name);
	}

	public int entityCount() {
		return entities.size();
	}

	public int relationCount() {
		return relations.size();
	}

	/* 글자 폭 어림 — 영문 0.6em, 한글 1.05em */
	private static double textWidth(String s) {
		double w = 0;
		for (char c : s.toCharArray()) {
			w += c < 0x80 ? FONT * 0.6 : FONT * 1.05;
		}
		return w;
	}

	/* 표를 열에 쌓는다 — 그림 전체 크기 { 너비, 높이 } */
	private double[] layout() {
		List<List<Entity>> cols = new ArrayList<List<Entity>>();
		List<Entity> rest = new ArrayList<Entity>(entities.values());
		for (String[] names : LAYOUT) {
			List<Entity> col = new ArrayList<Entity>();
			for (String name : names) {
				if (entities.containsKey(name)) {
					col.add(entities.get(name));
					rest.remove(entities.get(name));
				}
			}
			if (!col.isEmpty()) {
				cols.add(col);
			}
		}
		if (!rest.isEmpty()) {
			cols.add(rest);
		}
		double[] colH = new double[cols.size()], colW = new double[cols.size()];
		double tallest = 0;
		for (int i = 0; i < cols.size(); i++) {
			for (Entity e : cols.get(i)) {
				measure(e);
				colW[i] = Math.max(colW[i], e.w);
				colH[i] += e.h;
			}
			colH[i] += (cols.get(i).size() - 1) * ROW_GAP;
			tallest = Math.max(tallest, colH[i]);
		}
		double x = MARGIN;
		for (int i = 0; i < cols.size(); i++) {
			double y = MARGIN + (tallest - colH[i]) / 2;
			for (Entity e : cols.get(i)) {
				e.x = x + (colW[i] - e.w) / 2;
				e.y = y;
				y += e.h + ROW_GAP;
			}
			x += colW[i] + COL_GAP;
		}
		return new double[] { x - COL_GAP + MARGIN, tallest + MARGIN * 2 };
	}

	/* 표 크기 — 칸(타입 · 이름 · 키 · 설명)마다 가장 긴 글자에 맞춘다 */
	private void measure(Entity e) {
		for (String[] a : e.attrs) {
			for (int i = 0; i < 4; i++) {
				e.cell[i] = Math.max(e.cell[i], textWidth(a[i]));
			}
		}
		double inner = 0;
		int used = 0;
		for (double c : e.cell) {
			if (c > 0) {
				inner += c;
				used++;
			}
		}
		inner += Math.max(0, used - 1) * CELL_GAP;
		e.w = Math.max(Math.max(inner, textWidth(e.name) + 10) + PAD * 2, 150);
		e.h = HEADER_H + e.attrs.size() * ROW_H + (e.attrs.isEmpty() ? 6 : 4);
	}

	private static double cx(Entity e) {
		return e.x + e.w / 2;
	}

	private static double cy(Entity e) {
		return e.y + e.h / 2;
	}

	/* 선이 붙을 변 — 옆 열이면 좌우, 같은 열이면 위아래. 한 변에 여러 선이면 상대 위치 순으로 고르게 */
	private void anchors() {
		Map<String, List<Object[]>> slots = new LinkedHashMap<String, List<Object[]>>();
		for (Relation r : relations) {
			Entity a = entities.get(r.from), b = entities.get(r.to);
			int sa, sb;
			if (a == b) {
				sa = sb = RIGHT;
			} else if (Math.abs(cx(a) - cx(b)) > 1) {
				sa = cx(a) < cx(b) ? RIGHT : LEFT;
				sb = cx(a) < cx(b) ? LEFT : RIGHT;
			} else {
				sa = cy(a) < cy(b) ? BOTTOM : TOP;
				sb = cy(a) < cy(b) ? TOP : BOTTOM;
			}
			slots.computeIfAbsent(a.name + "#" + sa, k -> new ArrayList<Object[]>()).add(new Object[] { r.a, b });
			slots.computeIfAbsent(b.name + "#" + sb, k -> new ArrayList<Object[]>()).add(new Object[] { r.b, a });
		}
		for (Map.Entry<String, List<Object[]>> entry : slots.entrySet()) {
			Entity e = entities.get(entry.getKey().split("#")[0]);
			int side = Integer.parseInt(entry.getKey().split("#")[1]);
			boolean vertical = side == LEFT || side == RIGHT;
			List<Object[]> list = entry.getValue();
			list.sort((p, q) -> vertical ? Double.compare(cy((Entity) p[1]), cy((Entity) q[1]))
					: Double.compare(cx((Entity) p[1]), cx((Entity) q[1])));
			for (int i = 0; i < list.size(); i++) {
				Anchor an = (Anchor) list.get(i)[0];
				double t = (i + 1.0) / (list.size() + 1.0);
				if (vertical) {
					double top = e.y + HEADER_H / 2;
					an.x = side == LEFT ? e.x : e.x + e.w;
					an.y = top + (e.y + e.h - top) * t;
					an.nx = side == LEFT ? -1 : 1;
				} else {
					an.x = e.x + e.w * t;
					an.y = side == TOP ? e.y : e.y + e.h;
					an.ny = side == TOP ? -1 : 1;
				}
			}
		}
	}

	/* fit 이면 화면 폭에 맞춰 늘어나는 그림 (페이지 안에 넣을 때) */
	public String toSvg(boolean fit) {
		double[] size = layout();
		anchors();
		StringBuilder svg = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
			.append(fit ? "100%" : fmt(size[0])).append('"');
		if (!fit) {
			svg.append(" height=\"").append(fmt(size[1])).append('"');
		}
		svg.append(" viewBox=\"0 0 ").append(fmt(size[0])).append(' ').append(fmt(size[1]))
			.append("\" font-family=\"'Apple SD Gothic Neo','Malgun Gothic',sans-serif\" font-size=\"")
			.append(fmt(FONT)).append("\">\n<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");
		List<double[]> labels = new ArrayList<double[]>();
		for (Relation r : relations) {
			double c = r.from.equals(r.to) ? 60 : Math.max(45, Math.min(170, Math.hypot(r.b.x - r.a.x, r.b.y - r.a.y) * 0.4));
			double[] p = { r.a.x, r.a.y, r.a.x + r.a.nx * c, r.a.y + r.a.ny * c,
				r.b.x + r.b.nx * c, r.b.y + r.b.ny * c, r.b.x, r.b.y };
			svg.append("<path d=\"M").append(fmt(p[0])).append(' ').append(fmt(p[1])).append(" C")
				.append(fmt(p[2])).append(' ').append(fmt(p[3])).append(' ').append(fmt(p[4])).append(' ')
				.append(fmt(p[5])).append(' ').append(fmt(p[6])).append(' ').append(fmt(p[7]))
				.append("\" fill=\"none\" stroke=\"#666666\" stroke-width=\"1.2\"")
				.append(r.dashed ? " stroke-dasharray=\"6 4\"" : "").append("/>\n");
			/* 곡선 한가운데 (t = 0.5) — 관계 이름 자리 */
			labels.add(new double[] { 0.125 * p[0] + 0.375 * p[2] + 0.375 * p[4] + 0.125 * p[6],
				0.125 * p[1] + 0.375 * p[3] + 0.375 * p[5] + 0.125 * p[7] });
		}
		for (Entity e : entities.values()) {
			table(svg, e);
		}
		for (Relation r : relations) {
			marker(svg, r.a, r.fromCard);
			marker(svg, r.b, r.toCard);
		}
		for (int i = 0; i < relations.size(); i++) {
			String label = relations.get(i).label;
			if (!label.isEmpty()) {
				double[] p = labels.get(i);
				double w = textWidth(label) + 8;
				svg.append("<rect x=\"").append(fmt(p[0] - w / 2)).append("\" y=\"").append(fmt(p[1] - 9))
					.append("\" width=\"").append(fmt(w)).append("\" height=\"18\" rx=\"3\" fill=\"#ffffff\"")
					.append(" stroke=\"#bbbbbb\" stroke-width=\"0.8\"/>\n<text x=\"").append(fmt(p[0]))
					.append("\" y=\"").append(fmt(p[1] + 4)).append("\" text-anchor=\"middle\" fill=\"#333333\">")
					.append(esc(label)).append("</text>\n");
			}
		}
		return svg.append("</svg>\n").toString();
	}

	private void table(StringBuilder svg, Entity e) {
		svg.append("<g>\n<rect x=\"").append(fmt(e.x)).append("\" y=\"").append(fmt(e.y))
			.append("\" width=\"").append(fmt(e.w)).append("\" height=\"").append(fmt(e.h))
			.append("\" fill=\"#ffffff\" stroke=\"#444444\" stroke-width=\"1.2\"/>\n<rect x=\"").append(fmt(e.x))
			.append("\" y=\"").append(fmt(e.y)).append("\" width=\"").append(fmt(e.w)).append("\" height=\"")
			.append(fmt(HEADER_H)).append("\" fill=\"#e9e9e9\" stroke=\"#444444\" stroke-width=\"1.2\"/>\n<text x=\"")
			.append(fmt(e.x + e.w / 2)).append("\" y=\"").append(fmt(e.y + 17))
			.append("\" text-anchor=\"middle\" font-weight=\"bold\" fill=\"#222222\">").append(esc(e.name)).append("</text>\n");
		double[] start = new double[4];
		double x = e.x + PAD;
		for (int i = 0; i < 4; i++) {
			start[i] = x;
			x += e.cell[i] > 0 ? e.cell[i] + CELL_GAP : 0;
		}
		double y = e.y + HEADER_H;
		String[] colors = { "#888888", "#222222", "#555555", "#888888" };
		for (int i = 0; i < e.attrs.size(); i++) {
			String[] a = e.attrs.get(i);
			if (i > 0) {
				svg.append("<line x1=\"").append(fmt(e.x)).append("\" y1=\"").append(fmt(y + i * ROW_H)).append("\" x2=\"")
					.append(fmt(e.x + e.w)).append("\" y2=\"").append(fmt(y + i * ROW_H)).append("\" stroke=\"#eeeeee\"/>\n");
			}
			boolean[] bold = { false, a[2].contains("PK"), true, false };
			for (int k = 0; k < 4; k++) {
				if (!a[k].isEmpty()) {
					svg.append("<text x=\"").append(fmt(start[k])).append("\" y=\"").append(fmt(y + i * ROW_H + 14))
						.append("\" fill=\"").append(colors[k]).append('"').append(bold[k] ? " font-weight=\"bold\"" : "")
						.append('>').append(esc(a[k])).append("</text>\n");
				}
			}
		}
		svg.append("</g>\n");
	}

	/* 관계 기호 — 많음(까마귀발) 또는 하나(막대), 그 다음 0(동그라미) 또는 1(막대) */
	private void marker(StringBuilder svg, Anchor an, String card) {
		svg.append("<g transform=\"translate(").append(fmt(an.x)).append(' ').append(fmt(an.y)).append(") rotate(")
			.append(fmt(Math.toDegrees(Math.atan2(an.ny, an.nx))))
			.append(")\" stroke=\"#444444\" stroke-width=\"1.3\" fill=\"#ffffff\">");
		double next = 12;
		if (card.contains("{") || card.contains("}")) {
			svg.append("<path d=\"M0 -7 L11 0 M0 0 L11 0 M0 7 L11 0\" fill=\"none\"/>");
			next = 15;
		} else {
			svg.append("<path d=\"M7.0 -6 L7.0 6\"/>");
		}
		if (card.contains("o")) {
			svg.append("<circle cx=\"").append(fmt(next + 4)).append("\" cy=\"0\" r=\"4\"/>");
		} else {
			svg.append("<path d=\"M").append(fmt(next)).append(" -6 L").append(fmt(next)).append(" 6\"/>");
		}
		svg.append("</g>\n");
	}

	private static String fmt(double v) {
		return String.valueOf(Math.round(v * 10) / 10.0);
	}

	private static String esc(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
