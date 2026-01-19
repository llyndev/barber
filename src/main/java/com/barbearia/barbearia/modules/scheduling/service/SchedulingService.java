package com.barbearia.barbearia.modules.scheduling.service;

import com.barbearia.barbearia.modules.scheduling.dto.request.EndSchedulingRequest;
import com.barbearia.barbearia.modules.scheduling.dto.request.SchedulingRequest;
import com.barbearia.barbearia.modules.scheduling.dto.response.SchedulingResponse;
import com.barbearia.barbearia.exception.ConflictingScheduleException;
import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.scheduling.mapper.SchedulingMapper;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.catalog.model.BarberService;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.model.BusinessRole;
import com.barbearia.barbearia.modules.scheduling.model.Scheduling;
import com.barbearia.barbearia.modules.scheduling.model.AppointmentStatus;
import com.barbearia.barbearia.modules.account.service.UserService;
import com.barbearia.barbearia.modules.catalog.repository.BarberServiceRepository;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.scheduling.repository.SchedulingRepository;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.security.UserDetailsImpl;
import com.barbearia.barbearia.modules.availability.service.OpeningHoursService;
import com.barbearia.barbearia.tenant.BusinessContext;
import com.barbearia.barbearia.modules.inventory.service.InventoryService;
import com.barbearia.barbearia.modules.inventory.model.StockMovementType;
import com.barbearia.barbearia.modules.scheduling.dto.request.ProductUsageRequest;
import com.barbearia.barbearia.modules.inventory.repository.ProductRepository;
import com.barbearia.barbearia.modules.inventory.model.Product;
import com.barbearia.barbearia.modules.scheduling.model.SchedulingProduct;
import com.barbearia.barbearia.modules.orders.service.OrderService;
import com.barbearia.barbearia.modules.orders.dto.request.CreateOrderRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingService {

    private static final int SLOT_MINUTES = 15;

    private final UserService userService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final SchedulingRepository schedulingRepository;
    private final BarberServiceRepository barberServiceRepository;
    private final OpeningHoursService openingHoursService;
    private final BusinessRepository businessRepository;
    private final UserBusinessRepository userBusinessRepository;
    private final InventoryService inventoryService;
    private final ProductRepository productRepository;

    private Long getBusinessIdFromContext() {
        String businessIdStr = BusinessContext.getBusinessId();
        if (businessIdStr == null || businessIdStr.isBlank()) {
            throw new IllegalStateException("Business ID não encontrado");
        }

        return Long.parseLong(businessIdStr);
    }

    private Business getBusinessEntityFromContext() {
        Long businessId = getBusinessIdFromContext();
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<SchedulingResponse> listAll() {
        Long businessId = getBusinessIdFromContext();
        List<Scheduling> scheduling = schedulingRepository.findAllByBusinessId(businessId);
        return SchedulingMapper.toResponseList(scheduling);
    }

    @Transactional(readOnly = true)
    public List<SchedulingResponse> findAllByBusinessId(Long businessId) {
        List<Scheduling> scheduling = schedulingRepository.findAllByBusinessId(businessId);
        return SchedulingMapper.toResponseList(scheduling);
    }

    @Transactional(readOnly = true)
    public List<SchedulingResponse> getByDateRange(LocalDateTime start, LocalDateTime end, Long businessId) {
        List<Scheduling> scheduling = schedulingRepository.findByDateTimeBetweenAndBusinessId(start, end, businessId);
        return SchedulingMapper.toResponseList(scheduling);
    }

    @Transactional(readOnly = true)
    public SchedulingResponse getById(Long id) {
        Long businessId = getBusinessIdFromContext();
        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(id, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Agendamento não encontrado"));
        return SchedulingMapper.toResponse(scheduling);
    }

    @Transactional(readOnly = true)
    public List<SchedulingResponse> getByClientId(Long id) {
        List<Scheduling> scheduling = schedulingRepository.findByUser_Id(id);
        return SchedulingMapper.toResponseList(scheduling);
    }

    public List<SchedulingResponse> getByBarberId(Long id) {
        Long businessId = getBusinessIdFromContext();
        List<Scheduling> scheduling = schedulingRepository.findByBarber_IdAndBusinessId(id, businessId);
        return SchedulingMapper.toResponseList(scheduling);
    }

    public List<SchedulingResponse> getByDateTime(LocalDateTime start, LocalDateTime end) {
        Long businessId = getBusinessIdFromContext();
        List<Scheduling> scheduling = schedulingRepository.findByDateTimeBetweenAndBusinessId(start, end, businessId);
        return SchedulingMapper.toResponseList(scheduling);
    }

    @Transactional
    public Scheduling createScheduling(Long authenticatedUserId, SchedulingRequest request) {
        log.info("Creating scheduling. Authenticated User: {}, Request: {}", authenticatedUserId, request);

        Long businessId = getBusinessIdFromContext();
        Business business = getBusinessEntityFromContext();

        AppUser clientUser = null;
        String clientName = null;

        boolean isStaff = userBusinessRepository.existsByUserIdAndBusinessIdAndRole(authenticatedUserId, businessId, BusinessRole.OWNER) ||
                          userBusinessRepository.existsByUserIdAndBusinessIdAndRole(authenticatedUserId, businessId, BusinessRole.MANAGER) ||
                          userBusinessRepository.existsByUserIdAndBusinessIdAndRole(authenticatedUserId, businessId, BusinessRole.BARBER);

        if (isStaff) {
            if (request.clientId() != null) {
                clientUser = userService.getEntityById(request.clientId());
            } else if (request.clientName() != null && !request.clientName().isBlank()) {
                clientName = request.clientName();
            } else {
                // If staff doesn't specify client, assume they are booking for themselves
                clientUser = userService.getEntityById(authenticatedUserId);
            }
        } else {
            // Regular client booking for themselves
            if (request.dateTime().isBefore(LocalDateTime.now())) {
                throw new InvalidRequestException("The appointment must be for a future date/time.");
            }
            clientUser = userService.getEntityById(authenticatedUserId);
        }

        AppUser barber = userService.getEntityById(request.barberId());

        boolean isBarberInThisBusiness = userBusinessRepository.existsByUserIdAndBusinessIdAndRole(barber.getId(), businessId, BusinessRole.BARBER);

        if (!isBarberInThisBusiness) {
            throw new ResourceNotFoundException("Barbeiro inválido");
        }


        List<BarberService> barberService = barberServiceRepository.findAllById(request.barberServiceIds());

        List<BarberService> validServices = barberService.stream()
                .filter(service -> service.getBusiness().getId().equals(businessId))
                .toList();

        if (barberService.isEmpty() || validServices.size() != request.barberServiceIds().size()) {
            throw new ResourceNotFoundException("Serviço não encontrado");
        }

        LocalDateTime start = request.dateTime().withSecond(0).withNano(0);
        
        if (!isStaff || !Boolean.TRUE.equals(request.force())) {
            ensureAvailableOrThrow(barber.getId(), validServices, start);
        }

        Scheduling sched = new Scheduling();
        sched.setUser(clientUser);
        sched.setClientName(clientName);
        sched.setBarber(barber);
        sched.setBarberService(validServices);
        sched.setDateTime(start);
        sched.setStates(AppointmentStatus.SCHEDULED);
        sched.setBusiness(business);

        Scheduling saved = schedulingRepository.save(sched);
        log.info("Agendamento criado com sucesso ID: {}", saved.getId());

        return saved;

    }

    @Transactional
    public void cancelClient(Long schedulingId, Long clientId, @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long businessId = getBusinessIdFromContext();

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(schedulingId, businessId)
                .orElseThrow( () -> new ResourceNotFoundException("Agendamento não encontrado."));

        if (userDetails == null) {
            throw new RuntimeException("Usuário não encontrado");
        }

        if (scheduling.getUser() == null || !scheduling.getUser().getId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "FORBIDDEN");
        }

        scheduling.setStates(AppointmentStatus.CANCELED);
        schedulingRepository.save(scheduling);
    }

    @Transactional
    public void cancelByBusinessMember(Long schedulingId, Long userId, String reason, @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long businessId = getBusinessIdFromContext();

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(schedulingId, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Agendamento não encontrado")
        );

        boolean isManagerOrOwner = userBusinessRepository.existsByUserIdAndBusinessIdAndRole(userId, businessId, BusinessRole.OWNER) || userBusinessRepository.existsByUserIdAndBusinessIdAndRole(userId, businessId, BusinessRole.MANAGER);

        boolean isAssignedBarber = scheduling.getBarber().getId().equals(userId);

        if (!isManagerOrOwner && !isAssignedBarber) {
            throw new SecurityException("Não autorizado");
        }

        scheduling.setStates(AppointmentStatus.CANCELED);
        scheduling.setReasonCancel(reason);
        schedulingRepository.save(scheduling);
    }

    public Scheduling endService(Long schedulingId, EndSchedulingRequest endSchedulingRequest, Long barberId) {
        Long businessId = getBusinessIdFromContext();

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(schedulingId, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Agendamento não encontrado")
        );

        boolean isManagerOrOwner = userBusinessRepository.existsByUserIdAndBusinessIdAndRole(barberId, businessId, BusinessRole.OWNER) || userBusinessRepository.existsByUserIdAndBusinessIdAndRole(barberId, businessId, BusinessRole.MANAGER);

        boolean isAssignedBarber = scheduling.getBarber().getId().equals(barberId);

        if (!isManagerOrOwner && !isAssignedBarber) {
            throw new SecurityException("Não autorizado");
        }
        
        if (scheduling.getStates() != AppointmentStatus.SCHEDULED) {
            throw new InvalidRequestException("Requisição inválida");
        }

        if (endSchedulingRequest.servicesIds() != null && !endSchedulingRequest.servicesIds().isEmpty()) {
            List<BarberService> newServices = barberServiceRepository.findAllById(endSchedulingRequest.servicesIds());
            List<BarberService> validServices = newServices.stream()
                    .filter(service -> service.getBusiness().getId().equals(businessId))
                    .toList();

            if (validServices.size() != endSchedulingRequest.servicesIds().size()) {
                throw new ResourceNotFoundException("Um ou mais serviços não encontrados");
            }

            scheduling.getBarberService().addAll(validServices);
        }

        if (endSchedulingRequest.productsUsed() != null && !endSchedulingRequest.productsUsed().isEmpty()) {
            Business business = getBusinessEntityFromContext();
            AppUser user = userRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
            List<SchedulingProduct> schedulingProducts = new ArrayList<>();

            for (ProductUsageRequest productUsage : endSchedulingRequest.productsUsed()) {
                inventoryService.registerMovement(
                    business.getSlug(),
                    productUsage.productId(),
                    StockMovementType.EXIT,
                    productUsage.quantity(),
                    "Usado no agendamento #" + schedulingId,
                    user
                );

                Product product = productRepository.findById(productUsage.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
                
                SchedulingProduct schedulingProduct = SchedulingProduct.builder()
                    .scheduling(scheduling)
                    .product(product)
                    .quantity(productUsage.quantity())
                    .build();
                
                schedulingProducts.add(schedulingProduct);
            }

            if (scheduling.getProductsUsed() == null) {
                scheduling.setProductsUsed(new ArrayList<>());
            }
            scheduling.getProductsUsed().addAll(schedulingProducts);
        }

        scheduling.setStates(AppointmentStatus.COMPLETED);
        scheduling.setObservation(endSchedulingRequest.observation());
        scheduling.setAdditionalValue(endSchedulingRequest.additionalValue());
        scheduling.setPaymentMethod(endSchedulingRequest.paymentMethod());

        return schedulingRepository.save(scheduling);
    }

    @Transactional
    public Scheduling addService(Long schedulingId, List<Long> newServiceIds) {

        Long businessId = getBusinessIdFromContext();

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(schedulingId, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Agendamento não encontrado")
        );

        List<BarberService> newService = barberServiceRepository.findAllById(newServiceIds);
        List<BarberService> validServices = newService.stream()
                .filter(services -> services.getBusiness().getId().equals(businessId))
                .toList();

        if (newService.isEmpty() || validServices.size() != newServiceIds.size()) {
            throw new ResourceNotFoundException("Serviço não encontrado");
        }

        for (BarberService serviceToAdd : newService) {
            if (!scheduling.getBarberService().contains(serviceToAdd)) {
                scheduling.getBarberService().add(serviceToAdd);
            }
        }

        return schedulingRepository.save(scheduling);

    }

    public List<LocalTime> getAvailableSlots(LocalDate date, List<Long> barberServiceIds, Long barberId) {

        Long businessId = getBusinessIdFromContext();

        if (barberServiceIds == null || barberServiceIds.isEmpty()) {
            return List.of();
        }

        List<BarberService> barberServices = barberServiceRepository.findAllById(barberServiceIds);
        List<BarberService> validServices = barberServices.stream()
                .filter(services -> services.getBusiness().getId().equals(businessId))
                .toList();

        if (validServices.size() != barberServiceIds.size()) {
            throw new ResourceNotFoundException("Um ou mais serviços não encontrado");
        }

        int totalDurationInMinutes = validServices.stream()
                .mapToInt(s -> (s.getDurationInMinutes() != null && s.getDurationInMinutes() > 0) ? s.getDurationInMinutes() : SLOT_MINUTES)
                .sum();

        final Duration durationService = Duration.ofMinutes(totalDurationInMinutes);

        final long slotsNeeded = (long) Math.ceil((double) durationService.toMinutes() / SLOT_MINUTES);

        var hoursOpt = openingHoursService.findForBarberAndDate(barberId, date);
        if (hoursOpt.isEmpty() || !hoursOpt.get().active()) {
            return List.of();
        }

        final LocalTime open = hoursOpt.get().openTime();
        final LocalTime close = hoursOpt.get().closeTime();

        List<LocalTime> daySlots = new ArrayList<>();
        LocalTime t = open;
        while (!t.isAfter(close.minusMinutes(SLOT_MINUTES))) {
            daySlots.add(t);
            t = t.plusMinutes(SLOT_MINUTES);
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Scheduling> appointments = schedulingRepository.findByBarber_IdAndDateTimeBetween(barberId, startOfDay, endOfDay);

        Set<LocalTime> occupied = new HashSet<>();

        for (Scheduling scheduling : appointments) {
            if (scheduling == null) continue;
            if (scheduling.getDateTime() == null) continue;
            if (scheduling.getStates() == AppointmentStatus.CANCELED || scheduling.getStates() == AppointmentStatus.RESCHEDULED) continue;

            LocalTime start = scheduling.getDateTime().toLocalTime().withSecond(0).withNano(0);

            int existingDurationInMinutes = scheduling.getBarberService().stream()
                    .mapToInt(s -> (s.getDurationInMinutes() != null && s.getDurationInMinutes() > 0) ? s.getDurationInMinutes() : SLOT_MINUTES)
                    .sum();

            long schedulingSlots = (long) Math.ceil((double) existingDurationInMinutes / SLOT_MINUTES);

            LocalTime st = start;
            for (int i = 0; i < schedulingSlots; i++) {
                if (!st.isBefore(close)) break;
                occupied.add(st);
                st = st.plusMinutes(SLOT_MINUTES);
            }
        }

        LocalTime lastPossibleStartTime = close.minus(durationService);
        if (lastPossibleStartTime.isBefore(open)) {
            return List.of();
        }

        boolean isToday = date.isEqual(LocalDate.now());
        LocalTime nowTime = LocalTime.now();

        return daySlots.stream()
                .filter(s -> !s.isAfter(lastPossibleStartTime))
                .filter(s -> !occupied.contains(s))
                .filter(s -> !isToday || s.isAfter(nowTime))
                .filter(s -> {
                    for (int i = 1; i < slotsNeeded; i++) {
                        LocalTime next = s.plusMinutes(i * (long) SLOT_MINUTES);
                        if (next.isAfter(close)) return false;
                        if (occupied.contains(next)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private void ensureAvailableOrThrow(Long barberId, List<BarberService> barberService, LocalDateTime start) {
        if (start == null) {
            throw new InvalidRequestException("Obrigatório informar detalhes do agendamento");
        }

        if ((start.getMinute() % SLOT_MINUTES) != 0 || start.getSecond() != 0 || start.getNano() != 0) {
            throw new InvalidRequestException("Horário não encontrado");
        }

        if (start.toLocalDate().isEqual(LocalDate.now()) && !start.toLocalTime().isAfter(LocalTime.now())) {
            throw new InvalidRequestException("Requisição inválida");
        }

        var hoursOpt = openingHoursService.findForBarberAndDate(barberId, start.toLocalDate());
        if (hoursOpt.isEmpty() || !hoursOpt.get().active()) {
            throw new ConflictingScheduleException("Barbearia fechada ou barbeiro indisponível");
        }

        LocalTime open = hoursOpt.get().openTime();
        LocalTime close = hoursOpt.get().closeTime();

        int totalDurationInMinutes = barberService.stream()
                .mapToInt(s -> (s.getDurationInMinutes() != null && s.getDurationInMinutes() > 0) ? s.getDurationInMinutes() : SLOT_MINUTES)
                .sum();

        Duration totalDuration = Duration.ofMinutes(totalDurationInMinutes);

        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = startTime.plus(totalDuration);

        if (startTime.isBefore(open) || endTime.isAfter(close)) {
            throw new InvalidRequestException("Appointment is outside of opening hours.");
        }

        LocalDateTime dayStart = start.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = start.toLocalDate().atTime(LocalTime.MAX);

        List<Scheduling> dayAppointments = schedulingRepository
                .findByBarber_IdAndDateTimeBetween(barberId, dayStart, dayEnd);

        LocalDateTime newEnd = start.plus(totalDuration);

        for (Scheduling scheduling : dayAppointments) {
            if (scheduling == null || scheduling.getDateTime() == null) continue;
            if (scheduling.getStates() == AppointmentStatus.CANCELED || scheduling.getStates() == AppointmentStatus.RESCHEDULED) continue;

            int existingDurationInMinutes = scheduling.getBarberService().stream()
                    .mapToInt(s -> (s.getDurationInMinutes() != null && s.getDurationInMinutes() > 0) ? s.getDurationInMinutes() : SLOT_MINUTES)
                    .sum();

            LocalDateTime existingStart = scheduling.getDateTime().withSecond(0).withNano(0);
            LocalDateTime existingEnd = existingStart.plusMinutes(existingDurationInMinutes);

            boolean overlaps = start.isBefore(existingEnd) && newEnd.isAfter(existingStart);
            if (overlaps) {
                throw new ConflictingScheduleException("Horário conflitante");
            }
        }
    }

    @Transactional
    public void startAppointment(Long schedulingId) {
        Scheduling scheduling = schedulingRepository.findById(schedulingId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduling not found"));
        
        scheduling.setStates(AppointmentStatus.IN_PROGRESS);
        schedulingRepository.save(scheduling);

        orderService.createOrder(new CreateOrderRequest(scheduling.getId(), null, null, null));
    }
}
