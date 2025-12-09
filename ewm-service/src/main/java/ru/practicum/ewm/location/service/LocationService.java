package ru.practicum.ewm.location.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.mapper.LocationMapper;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.location.repository.LocationRepository;


import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper mapper;

    @Transactional
    public Location getOrCreateLocation(LocationDto locationDto) {

        Optional<Location> existingLocation = locationRepository
                .findByLatAndLon(locationDto.getLat(), locationDto.getLon());

        if (existingLocation.isPresent()) {
            return existingLocation.get();
        }

        Location newLocation = mapper.toLocation(locationDto);
        return locationRepository.save(newLocation);
    }
}