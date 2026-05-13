package org.yongzhang.firebird.Data;

import java.util.List;

public class CategoriesResponse {
    private List<String> categories;

    public CategoriesResponse() {}
    public CategoriesResponse(List<String> categories) { this.categories = categories; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
}

