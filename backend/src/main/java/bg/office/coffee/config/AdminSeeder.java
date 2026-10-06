package bg.office.coffee.config;

import bg.office.coffee.domain.AppUser;
import bg.office.coffee.domain.Role;
import bg.office.coffee.repo.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * При първо стартиране (няма активен админ) създава администраторски профил
 * от ADMIN_USERNAME / ADMIN_PASSWORD. При следващи стартирания не прави нищо.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;
    private final String displayName;

    public AdminSeeder(AppUserRepository users, PasswordEncoder encoder,
                       @Value("${app.admin.username}") String username,
                       @Value("${app.admin.password}") String password,
                       @Value("${app.admin.display-name}") String displayName) {
        this.users = users;
        this.encoder = encoder;
        this.username = username.trim().toLowerCase();
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRoleAndActiveTrue(Role.ADMIN)) {
            return;
        }
        AppUser admin = users.findByUsernameIgnoreCase(username).orElseGet(AppUser::new);
        boolean isNew = admin.getId() == null;
        admin.setUsername(username);
        admin.setDisplayName(isNew ? displayName : admin.getDisplayName());
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        users.save(admin);
        log.warn("Създаден е администратор '{}'. Смени паролата след първия вход.", username);
    }
}
