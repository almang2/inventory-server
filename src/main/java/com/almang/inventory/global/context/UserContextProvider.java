package com.almang.inventory.global.context;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserContextProvider {

    private final UserRepository userRepository;

    public UserStoreContext findUserAndStore(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
        Store store = user.getStore();

        return new UserStoreContext(user, store);
    }

    public record UserStoreContext(User user, Store store) {}
}
