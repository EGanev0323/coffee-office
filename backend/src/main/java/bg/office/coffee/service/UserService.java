package bg.office.coffee.service;

import bg.office.coffee.domain.AppUser;
import bg.office.coffee.domain.Role;
import bg.office.coffee.repo.AppUserRepository;
import bg.office.coffee.security.JwtService;
import bg.office.coffee.web.dto.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private static final String BAD_LOGIN = "Грешно потребителско име или парола.";

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public UserService(AppUserRepository users, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        AppUser user = users.findByUsernameIgnoreCase(req.username().trim())
                .orElseThrow(() -> new UnauthorizedException(BAD_LOGIN));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(BAD_LOGIN);
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("Профилът е деактивиран. Обърни се към администратора.");
        }
        return new LoginResponse(jwtService.issue(user, req.remember()), MeDto.from(user));
    }

    /**
     * Сменя паролата и отписва всички други устройства (нова версия на токените).
     * Връща нов токен за текущото устройство, за да не бъде отписано и то.
     */
    @Transactional
    public LoginResponse changeOwnPassword(Long userId, ChangePasswordRequest req, boolean remember) {
        AppUser user = load(userId);
        if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Текущата парола не е вярна.");
        }
        user.setPasswordHash(encoder.encode(req.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        return new LoginResponse(jwtService.issue(user, remember), MeDto.from(user));
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return users.findAllByOrderByDisplayNameAsc().stream().map(UserDto::from).toList();
    }

    @Transactional
    public UserDto create(CreateUserRequest req) {
        String username = req.username().trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("Потребителското име „" + username + "“ вече е заето.");
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setDisplayName(req.displayName().trim());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setRole(req.role());
        return UserDto.from(users.save(user));
    }

    @Transactional
    public UserDto update(Long id, UpdateUserRequest req, Long actorId) {
        AppUser user = load(id);
        if (id.equals(actorId) && (!req.active() || req.role() != Role.ADMIN)) {
            throw new BusinessException("Не можеш да деактивираш себе си или да махнеш собствените си администраторски права.");
        }
        user.setDisplayName(req.displayName().trim());
        user.setRole(req.role());
        user.setActive(req.active());
                user.setUnlimited(req.unlimited());
        if (req.boardVisible() != null) {
            user.setBoardVisible(req.boardVisible());
        }
        return UserDto.from(user);
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest req) {
        AppUser user = load(id);
        user.setPasswordHash(encoder.encode(req.password()));
        // Колегата се отписва от всички устройства и трябва да влезе с новата парола.
        user.setTokenVersion(user.getTokenVersion() + 1);
    }

    private AppUser load(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Потребителят не е намерен."));
    }
}