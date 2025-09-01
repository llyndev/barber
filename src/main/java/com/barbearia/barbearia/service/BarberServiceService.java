package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.BarberServiceRequest;
import com.barbearia.barbearia.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.mapper.BarberServiceMapper;
import com.barbearia.barbearia.model.BarberService;
import com.barbearia.barbearia.repository.BarberServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BarberServiceService {

    private final BarberServiceRepository barberServiceRepository;
    private final BarberServiceMapper barberServiceMapper;

    public List<BarberServiceResponse> findAll() {
        List<BarberService> barberService = barberServiceRepository.findAll();

        return barberService.stream()
                .map(barberServiceMapper::toDTO)
                .toList();
    }

    public BarberServiceResponse getById(Long id) {
        return barberServiceRepository.findById(id)
                .map(barberServiceMapper::toDTO)
                .orElseThrow( () -> new RuntimeException("Service not found"));
    }

    public BarberService getEntityById(Long id) {
        return barberServiceRepository.findById(id)
                .orElseThrow( () -> new RuntimeException("Service not found"));
    }

    public BarberServiceResponse save(BarberServiceRequest barberServiceRequest) {

        BarberService barberService = barberServiceMapper.toEntity(barberServiceRequest);

        barberService = barberServiceRepository.save(barberService);

        return barberServiceMapper.toDTO(barberService);
    }

    public void delete(Long id){
        barberServiceRepository.deleteById(id);
    }

}
