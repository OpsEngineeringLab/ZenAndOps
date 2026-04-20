package com.zenandops.auth.application.usecase;

import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.exception.SelfDeletionException;
import com.zenandops.auth.domain.exception.UserNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case for deleting a User.
 * Prevents self-deletion by comparing the target id with the current user id.
 */
@ApplicationScoped
public class DeleteUserUseCase {

    private final UserRepository userRepository;

    @Inject
    public DeleteUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Delete a User by id.
     *
     * @param id            the user identifier to delete
     * @param currentUserId the identifier of the currently authenticated user
     * @throws SelfDeletionException if the user attempts to delete themselves
     * @throws UserNotFoundException if no User exists with the given id
     */
    public void execute(String id, String currentUserId) {
        if (id.equals(currentUserId)) {
            throw new SelfDeletionException(
                    "Cannot delete your own user account");
        }

        userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        userRepository.delete(id);
    }
}
