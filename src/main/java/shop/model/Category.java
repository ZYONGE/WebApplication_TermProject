package shop.model;

import java.util.ArrayList;
import java.util.List;

/* 카테고리 (category) — 대분류는 parentId 가 없고, 소분류는 children 에 들어간다 */
public class Category {
	private int categoryId, sortOrder;
	private Integer parentId;
	private String name, parentName;
	private List<Category> children = new ArrayList<Category>();

	public int getCategoryId() { return categoryId; }
	public int getSortOrder() { return sortOrder; }
	public Integer getParentId() { return parentId; }
	public String getName() { return name; }
	public String getParentName() { return parentName; }
	public List<Category> getChildren() { return children; }

	public void setChildren(List<Category> children) { this.children = children; }
}
