package com.barbearia.barbearia.security;

import com.barbearia.barbearia.modules.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

     /*
     * Usado APENAS no login (DaoAuthenticationProvider chama este método).
     * A partir daqui, toda requisição autenticada usa loadUserById.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        String normalized = email.trim().toLowerCase();

        return userRepository.findByEmail(normalized)
                .map(UserDetailsImpl::from)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found."
                ));
    }

     /*
     * Usado pelo JwtFilter em toda requisição autenticada.
     * Busca por chave primária: mais rápido que por email, e imune a
     * troca de email pelo usuário no meio de uma sessão ativa.
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        return userRepository.findById(id)
                .map(UserDetailsImpl::from)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found."
                ));
    }


}
