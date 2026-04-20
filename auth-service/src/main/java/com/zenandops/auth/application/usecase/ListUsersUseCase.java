package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use case for retrieving a paginated list of all Users.
 */
@ApplicationScoped
public class ListUsersUseCase {

    private final UserRepository userRepository;

    @Inject
    public ListUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieve a paginated list of Users.
     *
     * @param page the page number (zero-based)
     * @param size the number of items per page
     * @return a paginated result containing the Users
     */
    public PaginatedResult<User> execute(int page, int size) {
        List<User> items = userRepository.findAll(page, size);
        long totalItems = userRepository.count();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        return new PaginatedResult<>(items, page, size, totalItems, totalPages);
    }
}
