package com.ichaabane.book_network.application.specification;

import com.ichaabane.book_network.domain.model.Book;
import org.springframework.data.jpa.domain.Specification;

public class BookSpecification {

    // Private constructor to hide the implicit public constructor
    private BookSpecification() {
        // Utility class - prevent instantiation
    }
    
    public static Specification<Book> withOwnerId(Integer ownerId) {
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("owner").get("id"), ownerId));
    }
}
