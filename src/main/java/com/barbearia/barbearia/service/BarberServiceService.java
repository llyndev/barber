package com.barbearia.barbearia.service;

import com.barbearia.barbearia.dto.request.BarberServiceRequest;
import com.barbearia.barbearia.dto.response.BarberServiceResponse;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.mapper.BarberServiceMapper;
import com.barbearia.barbearia.model.BarberService;
import com.barbearia.barbearia.repository.BarberServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class BarberServiceService {

    private final BarberServiceRepository barberServiceRepository;
    private final BarberServiceMapper barberServiceMapper;

    public List<BarberServiceResponse> listAll() {
        List<BarberService> barberService = barberServiceRepository.findAll();

        return barberService.stream()
                .map(BarberServiceMapper::toDTO)
                .toList();
    }

    public BarberServiceResponse getById(Long id) {
        return barberServiceRepository.findById(id)
                .map(BarberServiceMapper::toDTO)
                .orElseThrow( () -> new ResourceNotFoundException("Service not found"));
    }

    public BarberService getEntityById(Long id) {
        return barberServiceRepository.findById(id)
                .orElseThrow( () -> new ResourceNotFoundException("Service not found"));
    }

    public BarberServiceResponse save(BarberServiceRequest barberServiceRequest) {

        BarberService barberService = barberServiceMapper.toEntity(barberServiceRequest);

        barberService = barberServiceRepository.save(barberService);

        return barberServiceMapper.toDTO(barberService);
    }

    public BarberServiceResponse update(Long id, BarberServiceRequest request) {
        BarberService barberService = barberServiceRepository.findById(id).orElseThrow( () -> new ResourceNotFoundException("Service not found"));

        if (request.nameService() != null) {
            barberService.setNameService(request.nameService());
        }
        if (request.description() != null) {
            barberService.setDescription(request.description());
        }
        if (request.durationInMinutes() != null) {
            barberService.setDurationInMinutes(request.durationInMinutes());
        }
        if (request.price() != null) {
            barberService.setPrice(request.price());
        }

        BarberService updatedService = barberServiceRepository.save(barberService);

        return barberServiceMapper.toDTO(updatedService);
    }

    public void delete(Long id){
        barberServiceRepository.deleteById(id);
    }

}
