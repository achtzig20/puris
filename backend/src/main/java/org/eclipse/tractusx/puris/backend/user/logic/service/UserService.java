package org.eclipse.tractusx.puris.backend.user.logic.service;

import org.eclipse.tractusx.puris.backend.user.domain.model.User;
import org.eclipse.tractusx.puris.backend.user.logic.dto.UserSearchCriteria;

import java.util.List;
import java.util.UUID;

public interface UserService {

    User create(User user);

    User update(User user);

    User findById(UUID id);

    List<User> findAll();

    void delete(UUID id);

    List<User> search(UserSearchCriteria criteria);

}
