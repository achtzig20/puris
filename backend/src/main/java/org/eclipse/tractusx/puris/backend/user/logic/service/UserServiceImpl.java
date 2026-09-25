package org.eclipse.tractusx.puris.backend.user.logic.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.user.domain.model.User;
import org.eclipse.tractusx.puris.backend.user.domain.repository.UserRepository;
import org.eclipse.tractusx.puris.backend.user.logic.dto.UserSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User create(User user) {
        user.setEmail(user.getEmail().toLowerCase());
        user.setFirstName(user.getFirstName().trim());
        user.setLastName(user.getLastName().trim());

        Optional<User> existing = userRepository.findByEmail(user.getEmail());
        if (existing.isPresent()) {
            log.error("Could not create user, email already in use: " + user.getEmail());
            return null;
        }
        return userRepository.save(user);
    }

    @Override
    public User update(User user) {
        Optional<User> existing = userRepository.findById(user.getId());
        if (existing.isEmpty()) {
            log.error("Could not update user " + user.getId() + " because it didn't exist before");
            return null;
        }
        try {
            return userRepository.save(user);
        } catch (Exception e) {
        }
        return user;
    }

    @Override
    public User findById(UUID id) {
        return userRepository.findById(id).get();
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public void delete(UUID id) {
        userRepository.deleteById(id);
    }

    @Override
    public List<User> search(UserSearchCriteria criteria) {
        return userRepository.findAll().stream()
            .filter(u -> criteria.getFirstName() == null || criteria.getFirstName().equalsIgnoreCase(u.getFirstName()))
            .filter(u -> criteria.getLastName() == null || criteria.getLastName().equalsIgnoreCase(u.getLastName()))
            .filter(u -> criteria.getEmail() == null || criteria.getEmail().equalsIgnoreCase(u.getEmail()))
            .collect(Collectors.toList());
    }

}
