package com.barbearia.barbearia.modules.scheduling.service;

import com.barbearia.barbearia.exception.ConflictException;
import com.barbearia.barbearia.modules.inventory.dto.request.StockMovementCommand;
import com.barbearia.barbearia.modules.scheduling.dto.request.*;
import com.barbearia.barbearia.modules.scheduling.dto.response.SchedulingResponse;
import com.barbearia.barbearia.exception.ConflictingScheduleException;
import com.barbearia.barbearia.exception.InvalidRequestException;
import com.barbearia.barbearia.exception.ResourceNotFoundException;
import com.barbearia.barbearia.modules.scheduling.event.SchedulingCompletedEvent;
import com.barbearia.barbearia.modules.scheduling.mapper.SchedulingMapper;
import com.barbearia.barbearia.modules.account.model.AppUser;
import com.barbearia.barbearia.modules.catalog.model.BarberService;
import com.barbearia.barbearia.modules.business.model.Business;
import com.barbearia.barbearia.modules.business.model.BusinessRole;
import com.barbearia.barbearia.modules.scheduling.model.Scheduling;
import com.barbearia.barbearia.modules.scheduling.model.AppointmentStatus;
import com.barbearia.barbearia.modules.catalog.repository.BarberServiceRepository;
import com.barbearia.barbearia.modules.business.repository.BusinessRepository;
import com.barbearia.barbearia.modules.scheduling.model.SchedulingAdditionalValue;
import com.barbearia.barbearia.modules.scheduling.repository.SchedulingRepository;
import com.barbearia.barbearia.modules.business.repository.UserBusinessRepository;
import com.barbearia.barbearia.modules.account.repository.UserRepository;
import com.barbearia.barbearia.modules.availability.service.OpeningHoursService;
import com.barbearia.barbearia.tenant.BusinessContext;
import com.barbearia.barbearia.modules.inventory.service.InventoryService;
import com.barbearia.barbearia.modules.inventory.model.StockMovementType;
import com.barbearia.barbearia.modules.inventory.repository.ProductRepository;
import com.barbearia.barbearia.modules.inventory.model.Product;
import com.barbearia.barbearia.modules.scheduling.model.SchedulingProduct;
import com.barbearia.barbearia.modules.orders.service.OrderService;
import com.barbearia.barbearia.modules.orders.dto.request.CreateOrderRequest;
import com.barbearia.barbearia.modules.googlecalender.service.GoogleCalenderService;

import com.barbearia.barbearia.tenant.BusinessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchedulingService {

    private static final int SLOT_MINUTES = 15;

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final SchedulingRepository schedulingRepository;
    private final BarberServiceRepository barberServiceRepository;
    private final OpeningHoursService openingHoursService;
    private final BusinessRepository businessRepository;
    private final UserBusinessRepository userBusinessRepository;
    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final GoogleCalenderService googleCalenderService;
    private final SchedulingMapper schedulingMapper;
    private final BusinessGuard businessGuard;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Lista paginada de todos os agendamentos da barbearia atual.
     */
    public Page<SchedulingResponse> listAll(Pageable pageable) {
        businessGuard.requireOwnerOrManager();

        Long businessId = BusinessContext.requireBusinessId();

        return schedulingRepository
                .findAllByBusinessId(businessId, pageable)
                .map(schedulingMapper::toResponse);
    }

    /**
     * Agendamentos num intervalo de datas (usado pela agenda semanal/mensal).
     */
    public List<SchedulingResponse> getByDateRange(LocalDateTime start, LocalDateTime end) {
        Long businessId = BusinessContext.requireBusinessId();

        List<Scheduling> scheduling = schedulingRepository.findByDateTimeBetweenAndBusinessId(start, end, businessId);
        return schedulingMapper.toResponseList(scheduling);
    }

    /**
     * Um agendamento específico pelo id.
     */
    public SchedulingResponse getById(Long id) {
        Long businessId = BusinessContext.requireBusinessId();

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(id, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Agendamento não encontrado"));
        return schedulingMapper.toResponse(scheduling);
    }

    /**
     * Agendamento do client autenticado.
     */
    public List<SchedulingResponse> getByClientId(Long id) {
        List<Scheduling> scheduling = schedulingRepository.findByUser_Id(id);
        return schedulingMapper.toResponseList(scheduling);
    }

    /**
     * Agenda do barbeiro autenticado dentro da barbearia atual.
     */
    public List<SchedulingResponse> getByBarberId(Long id) {
        Long businessId = BusinessContext.requireBusinessId();

        List<Scheduling> scheduling = schedulingRepository.findByBarber_IdAndBusinessId(id, businessId);
        return schedulingMapper.toResponseList(scheduling);
    }

    /**
     * Agendamentos em um único dia.
     */
    public List<SchedulingResponse> getByDateTime(LocalDateTime start, LocalDateTime end) {
        Long businessId = BusinessContext.requireBusinessId();
        List<Scheduling> scheduling = schedulingRepository.findByDateTimeBetweenAndBusinessId(start, end, businessId);
        return schedulingMapper.toResponseList(scheduling);
    }

    /**
     * Criar um agendamento para si mesmo.
     */
    @Transactional
    public SchedulingResponse createScheduling(Long authenticatedUserId, SchedulingRequest request) {
        Long businessId = BusinessContext.requireBusinessId();

        Business business = businessRepository.getReferenceById(businessId);

        AppUser clientUser = userRepository.findById(authenticatedUserId).orElseThrow(
                () -> new ResourceNotFoundException("User not found."));

        boolean isBarberInThisBusiness = userBusinessRepository.existsByUserIdAndBusinessIdAndRole(request.barberId(), businessId, BusinessRole.BARBER);

        if (!isBarberInThisBusiness) {
            throw new ResourceNotFoundException("Invalid barber");
        }

        List<BarberService> barberService = barberServiceRepository.findAllById(request.barberServiceIds());

        List<BarberService> validServices = barberService.stream()
                .filter(service -> service.getBusiness().getId().equals(businessId))
                .toList();

        if (barberService.isEmpty() || validServices.size() != request.barberServiceIds().size()) {
            throw new ResourceNotFoundException("Serviço não encontrado");
        }

        LocalDateTime start = request.dateTime().withSecond(0).withNano(0);

        AppUser barber = userRepository.findByIdForUpdate(request.barberId()).orElseThrow(
                ()-> new ResourceNotFoundException("Invalid Barber"));

        ensureAvailableOrThrow(barber.getId(), validServices, start);

        Scheduling sched = new Scheduling();
        sched.setUser(clientUser);
        sched.setBarber(barber);
        sched.setBarberService(new ArrayList<>(validServices));
        sched.setDateTime(start);
        sched.setStates(AppointmentStatus.SCHEDULED);
        sched.setBusiness(business);

        Scheduling saved;

        try {
            saved = schedulingRepository.saveAndFlush(sched);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictingScheduleException("Hórario conflitante.");
        }

        SchedulingResponse response = schedulingMapper.toResponse(saved);

        try {
            googleCalenderService.syncSchedulingCreated(saved);
        } catch (Exception ex) {
            log.warn("falha ao tentar salvar agendamento no calendario." + ex.getMessage());
        }

        return response;
    }

    /**
     * Recepção/barbeiro agendando para um cliente que não tem conta
     * (nome e telefone digitados na hora).
     */
    @Transactional
    public SchedulingResponse createStaffScheduling(SchedulingStaffRequest request) {
        businessGuard.requireOwnerOrMangerOrBarber();

        Long businessId = BusinessContext.requireBusinessId();
        Business business = businessRepository.getReferenceById(businessId);

        List<BarberService> barberService = barberServiceRepository.findAllById(request.barberServiceIds());

        List<BarberService> validServices = barberService.stream()
                .filter(service -> service.getBusiness().getId().equals(businessId))
                .toList();

        if (barberService.isEmpty() || validServices.size() != request.barberServiceIds().size()) {
            throw new ResourceNotFoundException("Serviço não encontrado");
        }

        LocalDateTime start = request.dateTime().withSecond(0).withNano(0);

        AppUser barber = userRepository.findByIdForUpdate(request.barberId()).orElseThrow(
                ()-> new ResourceNotFoundException("Invalid Barber"));

        ensureAvailableOrThrow(barber.getId(), validServices, start);

        Scheduling sched = new Scheduling();
        sched.setClientName(request.clientName());
        sched.setClientNumber(request.clientNumber());
        sched.setBarber(barber);
        sched.setBarberService(new ArrayList<>(validServices));
        sched.setDateTime(start);
        sched.setStates(AppointmentStatus.SCHEDULED);
        sched.setBusiness(business);

        Scheduling saved;

        try {
            saved = schedulingRepository.saveAndFlush(sched);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictingScheduleException("Hórario conflitante.");
        }

        SchedulingResponse response = schedulingMapper.toResponse(saved);

        try {
            googleCalenderService.syncSchedulingCreated(saved);
        } catch (Exception ex) {
            log.warn("falha ao tentar salvar agendamento no calendario." + ex.getMessage());
        }

        return response;
    }

    /**
     * Cliente cancelando o próprio agendamento.
     */
    @Transactional
    public void cancelClient(Long clientId, Long schedulingId) {
        Long businessId = BusinessContext.requireBusinessId();

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(schedulingId, businessId)
                .orElseThrow( () -> new ResourceNotFoundException("Scheduling not found."));

        if (scheduling.getUser() == null || !scheduling.getUser().getId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "FORBIDDEN");
        }

        scheduling.setStates(AppointmentStatus.CANCELED);

        Scheduling saved;
        try {
            saved = schedulingRepository.saveAndFlush(scheduling);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("It was not possible to cancel the scheduling." + ex);
        }

        try {
            googleCalenderService.syncSchedulingCanceled(saved);
        } catch (Exception ex) {
            log.warn("Falha ao cancelar evento no Google Calendar para scheduling {}: {}", saved.getId(), ex.getMessage());
        }
    }

    /**
     * Barbeiro, gerente ou dono cancelando, com motivo registrado.
     */
    @Transactional
    public void cancelByBusinessMember(Long schedulingId, Long barberId, ReasonRequest reason) {
        Long businessId = BusinessContext.requireBusinessId();

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(schedulingId, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Scheduling not found."));

        boolean isOwnerOrManager = businessGuard.isOwnerOrManager();
        boolean isAssignedBarber = scheduling.getBarber().getId().equals(barberId);

        if (!isOwnerOrManager && !isAssignedBarber) {
            throw new SecurityException("Unauthorized.");
        }

        scheduling.setStates(AppointmentStatus.CANCELED);
        scheduling.setReasonCancel(reason.reason());
        Scheduling saved = schedulingRepository.save(scheduling);

        try {
            googleCalenderService.syncSchedulingCanceled(saved);
        } catch (Exception ex) {
            log.warn("Falha ao cancelar evento no Google Calendar para scheduling {}: {}", saved.getId(), ex.getMessage());
        }
    }

    /**
     * Finaliza o atendimento: forma de pagamento, serviços extras, produtos usados
     * (com baixa de estoque) e valores adicionais por barbeiro.
     */
    @Transactional
    public SchedulingResponse endService(Long schedulingId, EndSchedulingRequest request, Long currentUserId) {
        Long businessId = BusinessContext.requireBusinessId();

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(schedulingId, businessId).orElseThrow(
                () -> new ResourceNotFoundException("Agendamento não encontrado."));

        boolean isManagerOrOwner = businessGuard.isOwnerOrManager();
        boolean isAssignedBarber = scheduling.getBarber() != null && scheduling.getBarber().getId().equals(currentUserId);

        if (!isManagerOrOwner && !isAssignedBarber) {
            throw new AccessDeniedException("Não autorizado.");
        }

        // Verifica se o agendamneto que esta sendo finalizado esta com o STATUS de SCHEDULED (AGENDADO)
        if (scheduling.getStates() != AppointmentStatus.SCHEDULED) {
            throw new InvalidRequestException("Requisição inválida.");
        }

        applyAdditionalServices(scheduling, request.servicesIds(), businessId);
        applyProductUsage(scheduling, request.productsUsed(), businessId, currentUserId);

        applyAdditionalValues(scheduling, request.additionalValue(), businessId);

        scheduling.setStates(AppointmentStatus.COMPLETED);
        scheduling.setObservation(request.observation());
        scheduling.setPaymentMethod(request.paymentMethod());

        Scheduling saved = schedulingRepository.save(scheduling);

        eventPublisher.publishEvent(new SchedulingCompletedEvent(saved.getId(), businessId));

        return schedulingMapper.toResponse(saved);
    }

    /**
     * Adiciona serviços a um agendamento em andamento
     * (quando o cliente pede um novo serviço apos já ter começado o atendimento).
     */
    @Transactional
    public SchedulingResponse addService(Long schedulingId, List<Long> newServiceIds) {

        Long businessId = BusinessContext.requireBusinessId();

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

        Scheduling saved = schedulingRepository.save(scheduling);

        try {
            googleCalenderService.syncSchedulingUpdated(saved);
        } catch (Exception ex) {
            log.warn("Falha ao atualizar evento no Google Calendar para scheduling {}: {}", saved.getId(), ex.getMessage());
        }

        return schedulingMapper.toResponse(saved);

    }

    /**
     * Retorna os horários disponíveis de um barbeiro para uma data e conjunto
     * de serviços. É consumido pela tela pública de agendamento antes da criação
     * da reserva.
     * O formato da data é definido explicitamente para evitar dependência de
     * configurações globais do Spring Boot.
     */
    public List<LocalTime> getAvailableSlots(LocalDate date, List<Long> barberServiceIds, Long barberId) {

        Long businessId = BusinessContext.requireBusinessId();

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

    /**
     * Vincula serviços adicionais.
     */
    private void applyAdditionalServices(Scheduling scheduling, List<Long> servicesIds, Long businessId) {

        if (servicesIds == null || servicesIds.isEmpty()) return;

        Set<Long> uniqueIds = new LinkedHashSet<>(servicesIds);

        List<BarberService> services = barberServiceRepository.findAllByIdInAndBusinessId(uniqueIds, businessId);

        if (services.size() != uniqueIds.size()) {
            throw new ResourceNotFoundException("One or more services not found.");
        }

        Set<Long> alreadyLinked = scheduling.getBarberService().stream()
                .map(BarberService::getId)
                .collect(Collectors.toSet());

        services.stream()
                .filter(service -> !alreadyLinked.contains(service.getId()))
                .forEach(scheduling.getBarberService()::add);
    }

    /**
     * Registra produtos usados e dá baixa no estoque.
     */
    private void applyProductUsage(Scheduling scheduling, List<ProductUsageRequest> usages, Long businessId, Long currentUserId) {
        if (usages == null || usages.isEmpty()) return;

        // Normaliza o payload antes de ir para o banco
        Map<Long, Integer> quantityByProductId = new LinkedHashMap<>();
        for (ProductUsageRequest usage : usages) {
            if (usage.quantity() == null || usage.quantity() <= 0) {
                throw new InvalidRequestException("Quantidade inválida para o produto " + usage.productId());
            }
            quantityByProductId.merge(usage.productId(), usage.quantity(), Integer::sum);
        }

        List<Product> products = productRepository.findAllByIdInAndBusinessId(quantityByProductId.keySet(), businessId);

        Map<Long, Product> productById = products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));

        if (productById.size() != quantityByProductId.size()) {
            Set<Long> notFound = new HashSet<>(quantityByProductId.keySet());
            notFound.removeAll(productById.keySet());
            throw new ResourceNotFoundException("Products not found: " + notFound);
        }

        List<StockMovementCommand> movements = new ArrayList<>(quantityByProductId.size());

        for(Map.Entry<Long, Integer> entry : quantityByProductId.entrySet()) {
            Product product = productById.get(entry.getKey());
            Integer quantity = entry.getValue();

            scheduling.addProductUsed(SchedulingProduct.builder()
                    .scheduling(scheduling)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(product.getPrice())
                    .build());

            movements.add(new StockMovementCommand(
                    product, quantity, "Usado no agendamento #" + scheduling.getId()
            ));
        }

        inventoryService.registerMovements(businessId, StockMovementType.EXIT, movements, currentUserId);
    }

    /**
     * Converte os valores adicionais do request em entidades e substitui a coleção do agendamento.
     */
    private void applyAdditionalValues(Scheduling scheduling, List<AdditionalValueRequest> requests, Long businessId) {

        if (requests == null || requests.isEmpty()) return;

        Set<Long> barberIds = requests.stream()
                .map(AdditionalValueRequest::barberId)
                .collect(Collectors.toSet());

        for (Long barberId :barberIds) {
            boolean isMember = userBusinessRepository
                    .existsByUserIdAndBusinessIdAndRole(barberId, businessId, BusinessRole.BARBER);

            if (!isMember) {
                throw new ResourceNotFoundException("Barbeiro inválido: " + barberId);
            }
        }

        scheduling.getAdditionalValue().clear();

        for (AdditionalValueRequest req : requests) {
            SchedulingAdditionalValue value = SchedulingAdditionalValue.builder()
                    .barber(userRepository.getReferenceById(req.barberId()))
                    .value(req.value())
                    .build();

            scheduling.addAdditionalValue(value);
        }
    }

    /**
     * Inicia o atendimento e abre a comanda.
     */
    @Transactional
    public void startScheduling(Long schedulingId, Long currentUserId) {
        businessGuard.requireOwnerOrMangerOrBarber();

        Long businessId = BusinessContext.requireBusinessId();

        AppUser user = userRepository.findById(currentUserId).orElseThrow(
                () -> new ResourceNotFoundException("Usuário não encontrado"));

        Scheduling scheduling = schedulingRepository.findByIdAndBusinessId(schedulingId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));

        if (scheduling.getStates() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Não é possível alterar o status do agendamento no estado atual");
        }

        schedulingRepository.save(scheduling);

        orderService.createOrder(new CreateOrderRequest(scheduling.getId(), user.getId(), user.getName(), scheduling.getBarber().getId()));
    }
}
