package shop.dto;

import java.util.ArrayList;
import java.util.List;

/* 카테고리 */
public class CategoryDTO {

	private int categoryId;
	private Integer parentId;
	private String name;
	private int sortOrder;
	private List<CategoryDTO> children;


	/* 하위 카테고리는 null 대신 빈 목록으로 시작한다 — JSTL 에서 분기를 줄인다 */
	public CategoryDTO() {
		this.children = new ArrayList<CategoryDTO>();
	}

	public int getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(int categoryId) {
		this.categoryId = categoryId;
	}

	public Integer getParentId() {
		return parentId;
	}

	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public List<CategoryDTO> getChildren() {
		return children;
	}

	public void setChildren(List<CategoryDTO> children) {
		this.children = children;
	}
}
