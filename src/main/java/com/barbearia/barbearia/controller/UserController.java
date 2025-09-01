package com.barbearia.barbearia.controller;

import com.barbearia.barbearia.dto.response.UserResponse;
import com.barbearia.barbearia.mapper.UserMapper;
import com.barbearia.barbearia.model.AppUser;
import com.barbearia.barbearia.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public List<UserResponse> getAll() {
        List<AppUser> users = userService.findAll();

        return users.stream()
                .map(userMapper::toDTO)
                .toList();
    }

}
