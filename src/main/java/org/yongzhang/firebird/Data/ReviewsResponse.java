package org.yongzhang.firebird.Data;

import java.util.List;

public class ReviewsResponse {
    private List<Review> reviews;

    public ReviewsResponse() {}
    public ReviewsResponse(List<Review> reviews) { this.reviews = reviews; }

    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
}

