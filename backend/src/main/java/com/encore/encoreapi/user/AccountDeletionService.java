package com.encore.encoreapi.user;

import com.encore.encoreapi.ticket.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public AccountDeletionService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deleteExpiredAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(14);
        for (User user : userRepository.findByDeletionRequestedAtLessThanEqual(cutoff)) {
            orderRepository.deleteAll(orderRepository.findByUserId(user.getId()));
            userRepository.delete(user);
        }
    }
}
