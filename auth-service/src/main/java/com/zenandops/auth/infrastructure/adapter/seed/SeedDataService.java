package com.zenandops.auth.infrastructure.adapter.seed;

import com.zenandops.auth.application.port.PasswordEncoder;
import com.zenandops.auth.application.port.TagRepository;
import com.zenandops.auth.application.port.UserRepository;
import com.zenandops.auth.domain.entity.Tag;
import com.zenandops.auth.domain.entity.User;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Idempotent seed data service that populates MongoDB with default users, roles,
 * and tags on fresh deployments. Observes Quarkus StartupEvent to run before
 * the HTTP listener is ready.
 */
@ApplicationScoped
public class SeedDataService {

    private static final Logger LOG = Logger.getLogger(SeedDataService.class);

    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public SeedDataService(UserRepository userRepository,
                           TagRepository tagRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.passwordEncoder = passwordEncoder;
    }

    void onStart(@Observes StartupEvent event) {
        try {
            seed();
        } catch (Exception e) {
            LOG.error("Seed data routine failed. Continuing startup without terminating.", e);
        }
    }

    private void seed() {
        List<User> existingUsers = userRepository.findAll();
        if (!existingUsers.isEmpty()) {
            LOG.info("Users collection is not empty. Skipping seed data routine.");
            return;
        }

        LOG.info("Users collection is empty. Seeding default tags and users...");

        // Create default tags
        Tag deptEngineering = createTag("department", "engineering", "Engineering department");
        Tag deptOperations = createTag("department", "operations", "Operations department");
        Tag locationHQ = createTag("location", "HQ", "Headquarters location");
        Tag locationRemote = createTag("location", "remote", "Remote location");

        // Create default users with roles and tag assignments
        createUser("admin", "admin", "Administrator", "admin@zenandops.com",
                List.of("ADMIN", "USER"),
                resolveTagIds(deptEngineering, locationHQ));

        createUser("user", "user", "Default User", "user@zenandops.com",
                List.of("USER"),
                resolveTagIds(deptOperations));

        createUser("guest", "guest", "Guest User", "guest@zenandops.com",
                List.of("GUEST"),
                List.of());

        LOG.info("Seed data routine completed successfully.");
    }

    private Tag createTag(String key, String value, String description) {
        Optional<Tag> existing = tagRepository.findByKeyAndValue(key, value);
        if (existing.isPresent()) {
            LOG.infof("Tag %s:%s already exists. Skipping creation.", key, value);
            return existing.get();
        }

        Tag tag = new Tag();
        tag.setKey(key);
        tag.setValue(value);
        tag.setDescription(description);

        Instant now = Instant.now();
        tag.setCreatedAt(now);
        tag.setUpdatedAt(now);

        tagRepository.save(tag);
        LOG.infof("Created default tag: %s:%s", key, value);
        return tag;
    }

    private void createUser(String login, String password, String name, String email,
                            List<String> roles, List<String> tagIds) {
        User user = new User();
        user.setLogin(login);
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles(roles);
        user.setTagIds(tagIds);
        user.setActive(true);

        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userRepository.save(user);
        LOG.infof("Created default user: %s with roles %s", login, roles);
    }

    private List<String> resolveTagIds(Tag... tags) {
        List<String> ids = new ArrayList<>();
        for (Tag tag : tags) {
            if (tag != null && tag.getId() != null) {
                ids.add(tag.getId());
            }
        }
        return ids;
    }
}
